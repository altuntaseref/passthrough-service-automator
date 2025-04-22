package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.results.GitPushResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List; // generatedDocFiles listesi için

@Service
@Slf4j
@RequiredArgsConstructor
public class GitLabService {

    @Value("${gitlab.pat:#{null}}")
    private String gitlabToken;

    private static final String GITLAB_USERNAME_FOR_PAT = "private-token";

    /**
     * Projeyi başlatır/açar, TÜM dosyaları ekler, commit atar, pushlar ve
     * başarılı olursa dosya linklerini içeren bir sonuç döndürür.
     *
     * @param projectLocalPath Projenin yerel diskteki yolu.
     * @param gitlabRepoUrl    Hedef GitLab deposunun HTTPS URL'si.
     * @param commitMessage    Atılacak commit mesajı.
     * @param generatedDocFiles Oluşturulan doküman dosyalarının göreli yolları (URL hesaplamak için).
     * @return Git push işleminin sonucunu içeren GitPushResult nesnesi.
     */
    public GitPushResult initializeCommitAndPush(String projectLocalPath, String gitlabRepoUrl, String commitMessage, List<String> generatedDocFiles) {
        GitPushResult.GitPushResultBuilder resultBuilder = GitPushResult.builder()
                .attempted(true)
                .repoUrl(gitlabRepoUrl);

        String determinedDefaultBranch = "main"; // Varsayılan, hata durumunda kullanılacak

        try {
            validateInput(projectLocalPath, gitlabRepoUrl);
            log.info("Git deposu hazırlanıyor ve GitLab'e push edilecek: {}", projectLocalPath);
            File localRepoDir = new File(projectLocalPath);

            try (Git git = initializeOrOpenRepository(localRepoDir)) {
                Repository repository = git.getRepository();
                configureRemoteOrigin(repository, gitlabRepoUrl);
                addAndCommitChanges(git, commitMessage);
                CredentialsProvider cp = createCredentialsProvider();
                determinedDefaultBranch = determineDefaultBranch(repository); // Gerçek branch'i al

                // Branch'leri push et
                pushBranchInternal(git, "develop", cp);
                pushBranchInternal(git, determinedDefaultBranch, cp);

                log.info("Proje başarıyla GitLab'e push edildi: {}", gitlabRepoUrl);

                // --- URL Hesaplama ve Sonuca Ekleme ---
                resultBuilder.success(true);
                String repoBaseWebUrl = gitlabRepoUrl.replace(".git", "");

                if (generatedDocFiles != null) {
                    // GitLab URL yapısı: {repoBaseWebUrl}/-/blob/{branch}/{dosyaYolu}
                    // DocumentationGeneratorService'teki sabitleri kullanmak daha iyi
                    if (generatedDocFiles.contains(DocumentationGeneratorService.README_FILENAME)) {
                        resultBuilder.readmeUrl(repoBaseWebUrl + "/-/blob/" + determinedDefaultBranch + "/" + DocumentationGeneratorService.README_FILENAME);
                    }
                    if (generatedDocFiles.contains(DocumentationGeneratorService.POSTMAN_COLLECTION_FILENAME)) {
                        resultBuilder.postmanUrl(repoBaseWebUrl + "/-/blob/" + determinedDefaultBranch + "/" + DocumentationGeneratorService.POSTMAN_COLLECTION_FILENAME);
                    }
                }
                // --- URL Hesaplama Sonu ---
            }
        } catch (Exception e) { // Tüm hataları yakala
            log.error("GitLab işlemi sırasında hata oluştu: {}", e.getMessage(), e);
            resultBuilder.success(false).errorMessage(e.getMessage());
        }

        return resultBuilder.build();
    }

    // --- Özel (Private) Yardımcı Metotlar ---

    private void validateInput(String path, String url) throws IllegalArgumentException {
        if (!StringUtils.hasText(gitlabToken)) {
            throw new IllegalArgumentException("GitLab PAT (gitlab.pat) konfigürasyonda tanımlanmamış veya boş.");
        }
        if (!StringUtils.hasText(path) || !StringUtils.hasText(url)) {
            throw new IllegalArgumentException("Proje yolu veya GitLab URL'si sağlanmadı.");
        }
        File localPathFile = new File(path);
        if (!localPathFile.exists() || !localPathFile.isDirectory()) {
            throw new IllegalArgumentException("Proje dizini bulunamadı veya geçersiz: " + path);
        }
    }

    private Git initializeOrOpenRepository(File directory) throws IOException, GitAPIException {
        File gitDir = new File(directory, ".git");
        if (gitDir.exists()) {
            log.debug("Mevcut Git deposu açılıyor: {}", directory.getAbsolutePath());
            try {
                return Git.open(directory);
            } catch (IOException e) {
                throw new IOException("Mevcut .git deposu açılamadı: " + directory.getAbsolutePath(), e);
            }
        } else {
            log.info("Yeni Git deposu başlatılıyor: {}", directory.getAbsolutePath());
            return Git.init().setDirectory(directory).call();
        }
    }

    private void configureRemoteOrigin(Repository repository, String gitlabRepoUrl) throws IOException {
        StoredConfig config = repository.getConfig();
        String existingUrl = config.getString("remote", "origin", "url");
        if (!gitlabRepoUrl.equals(existingUrl)) {
            config.setString("remote", "origin", "url", gitlabRepoUrl);
            config.save();
            log.info("Remote 'origin' ayarlandı/güncellendi: {}", gitlabRepoUrl);
        } else {
            log.debug("Remote 'origin' zaten doğru şekilde ayarlanmış.");
        }
    }

    private void addAndCommitChanges(Git git, String commitMessage) throws GitAPIException, IOException {
        git.add().addFilepattern(".").call(); // Tüm dosyaları ekle
        log.debug("Dosyalar Git index'e eklendi/güncellendi.");
        Status status = git.status().call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD); // HEAD var mı kontrol et
        if (head == null || !status.isClean()) { // Hiç commit yoksa VEYA değişiklik varsa
            log.info("Commit atılıyor: '{}'", commitMessage);
            RevCommit commit = git.commit().setMessage(commitMessage).setAllowEmpty(head == null).call();
            log.info("Yeni commit oluşturuldu: {}", commit.getId().getName());
        } else {
            log.info("Commit atılacak yeni değişiklik bulunamadı.");
        }
    }

    private CredentialsProvider createCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(GITLAB_USERNAME_FOR_PAT, gitlabToken);
    }

    private String determineDefaultBranch(Repository repository) throws IOException {
        Ref headRef = repository.findRef(Constants.HEAD);
        String targetBranch = null;
        if (headRef != null && headRef.isSymbolic()) {
            String targetName = headRef.getTarget().getName();
            if (targetName.startsWith(Constants.R_HEADS)) {
                targetBranch = targetName.substring(Constants.R_HEADS.length());
            }
        }

        if (targetBranch == null || repository.findRef(Constants.R_HEADS + targetBranch) == null) {
            if (repository.findRef(Constants.R_HEADS + "main") != null) targetBranch = "main";
            else if (repository.findRef(Constants.R_HEADS + Constants.MASTER) != null) targetBranch = Constants.MASTER;
            else targetBranch = "main"; // Hiçbiri yoksa main varsayalım
            log.warn("Varsayılan branch belirlenemedi veya mevcut değil, '{}' kullanılacak.", targetBranch);
        } else {
            log.debug("Varsayılan branch olarak '{}' belirlendi.", targetBranch);
        }
        return targetBranch;
    }

    private void pushBranchInternal(Git git, String branchName, CredentialsProvider cp) throws GitAPIException, IOException {
        log.info("'{}' branch'i push ediliyor...", branchName);
        String localRef = Constants.R_HEADS + branchName;
        String remoteRef = Constants.R_HEADS + branchName;

        if (git.getRepository().findRef(localRef) == null) {
            log.info("Lokal branch '{}' bulunamadı, oluşturuluyor...", branchName);
            try {
                git.branchCreate().setName(branchName).call();
                log.info("Lokal branch '{}' başarıyla oluşturuldu.", branchName);
            } catch (GitAPIException e) {
                log.error("Lokal branch '{}' oluşturulamadı: {}", branchName, e.getMessage());
                if ("develop".equals(branchName)) throw e; // develop kritik
                else log.warn("Ana branch '{}' oluşturulamadı, init ile oluşmuş olabilir.", branchName);
            }
        }

        PushCommand pushCommand = git.push();
        pushCommand.setRemote("origin");
        pushCommand.setRefSpecs(new RefSpec(localRef + ":" + remoteRef));
        pushCommand.setCredentialsProvider(cp);

        try {
            pushCommand.call();
            log.info("'{}' branch'i başarıyla push edildi.", branchName);
        } catch (TransportException e) {
            log.error("'{}' branch'i push edilirken kimlik doğrulama/ağ hatası: {}", branchName, e.getMessage());
            throw e;
        } catch (GitAPIException e) {
            log.error("'{}' branch'i push edilirken Git hatası: {}", branchName, e.getMessage());
            throw e;
        }
    }
}