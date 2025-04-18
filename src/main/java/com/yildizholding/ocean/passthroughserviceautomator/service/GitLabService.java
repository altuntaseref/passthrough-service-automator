package com.yildizholding.ocean.passthroughserviceautomator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitLabService {

    // GitLab PAT'ını application.properties'ten güvenli bir şekilde oku
    @Value("${gitlab.pat}")
    private String gitlabToken;

    // GitLab PAT ile HTTPS klonlama/pushlama yaparken kullanıcı adı genellikle önemsizdir
    // veya belirli token tipleri için özel isimler gerekebilir (örn: "oauth2", "PRIVATE-TOKEN").
    // En yaygın kullanılanlardan biri "PRIVATE-TOKEN" veya boş olmayan herhangi bir string'dir.
    private static final String GITLAB_USERNAME_FOR_PAT = "private-token"; // Veya "oauth2" deneyebilirsiniz

    /**
     * Oluşturulan projeyi belirtilen GitLab deposuna push eder.
     * Önce 'develop', sonra 'master' (veya 'main') branch'lerini oluşturur/günceller.
     *
     * @param projectLocalPath Projenin yerel diskteki yolu.
     * @param gitlabRepoUrl    Hedef GitLab deposunun HTTPS URL'si.
     * @throws IOException     Dosya sistemi, ağ hatası veya depo bulunamadı.
     * @throws GitAPIException Git işlemi hatası.
     * @throws IllegalArgumentException Gerekli bilgiler eksikse (token, path, url).
     */
    public void pushProjectToGitLab(String projectLocalPath, String gitlabRepoUrl)
            throws IOException, GitAPIException, IllegalArgumentException {

        validateInput(projectLocalPath, gitlabRepoUrl);
        log.info("Proje GitLab'e push edilecek: Yerel={}, Remote={}", projectLocalPath, gitlabRepoUrl);
        File localRepoDir = new File(projectLocalPath);

        // try-with-resources ile Git ve Repository nesnelerinin kapatılmasını garantile
        try (Git git = initializeOrOpenRepository(localRepoDir)) {
            Repository repository = git.getRepository();

            // Remote 'origin' ayarını yap veya güncelle
            configureRemoteOrigin(repository, gitlabRepoUrl);

            // Tüm değişiklikleri ekle ve commit at (eğer değişiklik varsa)
            addAndCommitChanges(git, "Initial project structure and code generation");

            // Kimlik bilgilerini oluştur (PAT ile)
            CredentialsProvider credentialsProvider = createCredentialsProvider();

            // Ana branch'leri push et (develop ve main/master)
            String defaultBranch = determineDefaultBranch(repository); // main mi master mı?
            pushBranch(git, "develop", credentialsProvider);
            pushBranch(git, defaultBranch, credentialsProvider);

            log.info("Proje başarıyla GitLab'e push edildi: {}", gitlabRepoUrl);

        } catch (GitAPIException | IOException e) {
            log.error("GitLab işlemi sırasında hata oluştu: {}", e.getMessage(), e);
            throw e; // Hatayı tekrar fırlat ki üst katman yakalasın
        }
    }

    // --- Özel (Private) Yardımcı Metotlar ---

    /** Girdi parametrelerini doğrular. */
    private void validateInput(String path, String url) {
        if (gitlabToken == null || gitlabToken.isBlank()) {
            throw new IllegalArgumentException("GitLab Personal Access Token (gitlab.pat) konfigürasyonda tanımlanmamış.");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Proje yerel yolu sağlanmadı.");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("GitLab repository URL'si sağlanmadı.");
        }
        File localPathFile = new File(path);
        if (!localPathFile.exists() || !localPathFile.isDirectory()) {
            throw new IllegalArgumentException("Proje dizini bulunamadı veya geçerli bir dizin değil: " + path);
        }
    }

    /** Mevcut Git deposunu açar veya yoksa yenisini başlatır. */
    private Git initializeOrOpenRepository(File directory) throws IOException, GitAPIException {
        File gitDir = new File(directory, ".git");
        if (gitDir.exists()) {
            log.info("Mevcut Git deposu açılıyor: {}", directory.getAbsolutePath());
            return Git.open(directory);
        } else {
            log.info("Yeni Git deposu başlatılıyor: {}", directory.getAbsolutePath());
            return Git.init().setDirectory(directory).call();
        }
    }

    /** Remote 'origin' URL'sini ayarlar veya günceller. */
    private void configureRemoteOrigin(Repository repository, String gitlabRepoUrl) throws IOException {
        StoredConfig config = repository.getConfig();
        String existingUrl = config.getString("remote", "origin", "url");

        if (gitlabRepoUrl.equals(existingUrl)) {
            log.debug("Remote 'origin' zaten doğru şekilde ayarlanmış: {}", gitlabRepoUrl);
            return;
        }

        config.setString("remote", "origin", "url", gitlabRepoUrl);
        // Push URL'si genellikle aynıdır, ama isterseniz ayrıca ayarlayabilirsiniz
        // config.setString("remote", "origin", "pushurl", gitlabRepoUrl);
        config.save();
        log.info("Remote 'origin' ayarlandı/güncellendi: {}", gitlabRepoUrl);
    }

    /** Tüm dosyaları ekler ve değişiklik varsa commit atar. */
    private void addAndCommitChanges(Git git, String commitMessage) throws GitAPIException, IOException {
        // Önce tüm değişiklikleri stage'e ekle
        git.add().addFilepattern(".").call();
        log.debug("Dosyalar Git index'e eklendi/güncellendi.");

        // Değişiklik olup olmadığını kontrol et
        Status status = git.status().call();
        boolean hasChanges = !status.isClean(); // Temiz DEĞİLSE değişiklik vardır

        // Eğer hiç commit yoksa (yeni repo) veya değişiklik varsa commit at
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        if (head == null || hasChanges) {
            log.info("Commit atılıyor: '{}'", commitMessage);
            RevCommit commit = git.commit()
                                  .setMessage(commitMessage)
                                  .setAllowEmpty(head == null) // İlk commit boş olabilir
                                  .call();
            log.info("Yeni commit oluşturuldu: {}", commit.getId().getName());
        } else {
            log.info("Commit atılacak yeni değişiklik bulunamadı.");
        }
    }

    /** GitLab PAT kullanarak kimlik bilgisi sağlayıcısını oluşturur. */
    private CredentialsProvider createCredentialsProvider() {
        // PAT kullandığımızda şifre PAT'ın kendisidir.
        return new UsernamePasswordCredentialsProvider(GITLAB_USERNAME_FOR_PAT, gitlabToken);
    }

    /** Depodaki varsayılan branch'i belirler (main veya master). */
    private String determineDefaultBranch(Repository repository) throws IOException {
        // Genellikle 'main' veya 'master' olur. HEAD sembolik referansına bakabiliriz.
        Ref headRef = repository.findRef(Constants.HEAD);
        String targetBranch = Constants.MASTER; // Varsayılan olarak master
        if (headRef != null && headRef.isSymbolic()) {
             String target = headRef.getTarget().getName(); // Örn: refs/heads/main
             if (target.startsWith(Constants.R_HEADS)) {
                 targetBranch = target.substring(Constants.R_HEADS.length());
             }
        }
        // Eğer HEAD bir branch'e işaret etmiyorsa (detached HEAD veya yeni repo), 'main' kullanalım
        if (targetBranch.equals(Constants.HEAD) || repository.findRef(Constants.R_HEADS + targetBranch) == null) {
            log.warn("Varsayılan branch belirlenemedi veya mevcut değil, '{}' kullanılacak.", Constants.MASTER);
            return Constants.MASTER; // Veya 'main' tercih ediliyorsa Constants.MAIN
        }

        log.debug("Varsayılan branch olarak '{}' belirlendi.", targetBranch);
        return targetBranch;
    }


    /** Belirtilen branch'i remote 'origin'e push eder. */
    private void pushBranch(Git git, String branchName, CredentialsProvider cp) throws GitAPIException, IOException {
        log.info("'{}' branch'i push ediliyor...", branchName);
        String localRef = Constants.R_HEADS + branchName; // Örn: refs/heads/develop
        String remoteRef = Constants.R_HEADS + branchName; // Örn: refs/heads/develop

        // Lokal branch yoksa oluştur
        if (git.getRepository().findRef(localRef) == null) {
            log.info("Lokal branch '{}' bulunamadı, oluşturuluyor...", branchName);
            // Mevcut HEAD'den (muhtemelen son commit'ten) branch oluştur
            git.branchCreate().setName(branchName).call();
            log.info("Lokal branch '{}' başarıyla oluşturuldu.", branchName);
        }

        PushCommand pushCommand = git.push();
        pushCommand.setRemote("origin");
        pushCommand.setRefSpecs(new RefSpec(localRef + ":" + remoteRef)); // Lokal:Remote eşlemesi
        pushCommand.setCredentialsProvider(cp);
        // pushCommand.setForce(true); // Genellikle ilk push için gerekmez, dikkatli olun!

        try {
            pushCommand.call();
            log.info("'{}' branch'i başarıyla push edildi.", branchName);
        } catch (GitAPIException e) {
            log.error("'{}' branch'i push edilirken hata oluştu: {}", branchName, e.getMessage());
            throw e;
        }
    }
}