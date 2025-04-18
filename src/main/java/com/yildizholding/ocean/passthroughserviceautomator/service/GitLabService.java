package com.yildizholding.ocean.passthroughserviceautomator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException; // Spesifik hatalar için
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // String kontrolü için

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitLabService {

    @Value("${gitlab.pat:#{null}}") // Ortam değişkenini oku, yoksa null ata
    private String gitlabToken;

    // GitLab PAT ile HTTPS push yaparken kullanıcı adı genellikle önemsizdir
    // veya "oauth2", "private-token" gibi özel değerler alabilir.
    private static final String GITLAB_USERNAME_FOR_PAT = "private-token";

    /**
     * Proje dizinini başlatır/açar, TÜM dosyaları ekler, commit atar ve
     * 'develop' ile varsayılan ('main'/'master') branch'lerini pushlar.
     *
     * @param projectLocalPath Projenin yerel diskteki yolu.
     * @param gitlabRepoUrl    Hedef GitLab deposunun HTTPS URL'si.
     * @param commitMessage    Atılacak commit mesajı.
     * @throws IOException     Dosya sistemi, ağ hatası veya depo bulunamadı.
     * @throws GitAPIException Git işlemi hatası.
     * @throws IllegalArgumentException Gerekli bilgiler eksikse.
     */
    public void initializeCommitAndPush(String projectLocalPath, String gitlabRepoUrl, String commitMessage)
            throws IOException, GitAPIException, IllegalArgumentException {

        validateInput(projectLocalPath, gitlabRepoUrl); // Girdi kontrolü
        log.info("Proje Git deposu hazırlanıyor ve GitLab'e push edilecek: {}", projectLocalPath);
        File localRepoDir = new File(projectLocalPath);

        // try-with-resources JGit için önemli, Git nesnesini otomatik kapatır
        try (Git git = initializeOrOpenRepository(localRepoDir)) {
            Repository repository = git.getRepository();

            // Remote 'origin' ayarını yap veya güncelle
            configureRemoteOrigin(repository, gitlabRepoUrl);

            // Tüm değişiklikleri ekle ve commit at (eğer değişiklik varsa)
            addAndCommitChanges(git, commitMessage);

            // Kimlik bilgilerini oluştur (PAT ile)
            CredentialsProvider cp = createCredentialsProvider();

            // Ana branch'leri push et (develop ve main/master)
            String defaultBranch = determineDefaultBranch(repository); // main mi master mı?

            // Önce develop branch'ini push et
            pushBranchInternal(git, "develop", cp);

            // Sonra varsayılan ana branch'i push et
            pushBranchInternal(git, defaultBranch, cp);

            log.info("Proje başarıyla GitLab'e push edildi: {}", gitlabRepoUrl);

        } catch (TransportException e) {
            log.error("GitLab kimlik doğrulama hatası veya ağ sorunu: {}", e.getMessage());
            // Sadece loglayıp daha genel bir hata fırlatabiliriz veya spesifik mesajla RuntimeException
            throw new RuntimeException("GitLab kimlik doğrulama/ağ hatası: " + e.getMessage(), e); // RuntimeException ile sarmala
            // VEYA sadece throw e; (ama bu durumda üst katman yine GitAPIException yakalamaya çalışabilir)
        } catch (GitAPIException | IOException e) { // Birden fazla exception tipi yakala
            log.error("GitLab işlemi sırasında hata oluştu: {}", e.getMessage(), e);
            throw new RuntimeException("GitLab işlemi hatası: " + e.getMessage(), e); // RuntimeException ile sarmala
            // VEYA sadece throw e;
        } catch (Exception e) { // Beklenmedik hatalar
            log.error("GitLab işlemi sırasında beklenmedik bir hata oluştu.", e);
            throw new RuntimeException("Beklenmedik GitLab hatası", e);
        }
    }

    // --- Özel (Private) Yardımcı Metotlar ---

    /** Girdi parametrelerini doğrular. */
    private void validateInput(String path, String url) throws IllegalArgumentException {
        if (!StringUtils.hasText(gitlabToken)) { // application.properties'ten gelen token boş mu?
            throw new IllegalArgumentException("GitLab Personal Access Token (gitlab.pat) konfigürasyonda tanımlanmamış veya boş.");
        }
        if (!StringUtils.hasText(path)) {
            throw new IllegalArgumentException("Proje yerel yolu sağlanmadı.");
        }
        if (!StringUtils.hasText(url)) {
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
            log.debug("Mevcut Git deposu açılıyor: {}", directory.getAbsolutePath());
            // Mevcut depoyu açarken build() yerine doğrudan open() kullanmak daha iyi olabilir
            try {
                return Git.open(directory);
            } catch (IOException e) {
                log.error(".git dizini var ama depo açılamıyor: {}. Hata: {}", directory.getAbsolutePath(), e.getMessage());
                // Belki bozuktur? Yeniden başlatmayı deneyebilir miyiz? Veya hata fırlat.
                // Şimdilik hata fırlatalım.
                throw new IOException("Mevcut .git deposu açılamadı: " + directory.getAbsolutePath(), e);
            }
        } else {
            log.info("Yeni Git deposu başlatılıyor: {}", directory.getAbsolutePath());
            // Yeni depo başlat
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
        config.save(); // Değişikliği kaydet
        log.info("Remote 'origin' ayarlandı/güncellendi: {}", gitlabRepoUrl);
    }

    /** Tüm dosyaları ekler ve değişiklik varsa commit atar. */
    private void addAndCommitChanges(Git git, String commitMessage) throws GitAPIException, IOException {
        // Önce tüm değişiklikleri stage'e ekle (.gitignore dosyaları hariç tutulur)
        git.add().addFilepattern(".").call();
        log.debug("Dosyalar Git index'e eklendi/güncellendi.");

        // Değişiklik olup olmadığını kontrol et
        Status status = git.status().call();
        boolean hasChanges = !status.isClean();

        // Eğer hiç commit yoksa (yeni repo) veya değişiklik varsa commit at
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        if (head == null || hasChanges) {
            log.info("Commit atılıyor: '{}'", commitMessage);
            RevCommit commit = git.commit()
                    .setMessage(commitMessage)
                    .setAllowEmpty(head == null) // Sadece ilk commit boş olabilir
                    .call();
            log.info("Yeni commit oluşturuldu: {}", commit.getId().getName());
        } else {
            log.info("Commit atılacak yeni değişiklik bulunamadı.");
        }
    }

    /** GitLab PAT kullanarak kimlik bilgisi sağlayıcısını oluşturur. */
    private CredentialsProvider createCredentialsProvider() {
        return new UsernamePasswordCredentialsProvider(GITLAB_USERNAME_FOR_PAT, gitlabToken);
    }

    /** Depodaki varsayılan branch'i belirler (genellikle main veya master). */
    private String determineDefaultBranch(Repository repository) throws IOException {
        // HEAD sembolik referansına bakarak ana branch'i bulmaya çalış
        Ref headRef = repository.findRef(Constants.HEAD);
        String targetBranch = null;
        if (headRef != null && headRef.isSymbolic()) {
            String targetName = headRef.getTarget().getName();
            if (targetName.startsWith(Constants.R_HEADS)) {
                targetBranch = targetName.substring(Constants.R_HEADS.length());
            }
        }

        // Eğer HEAD bir branch'e işaret etmiyorsa veya bulunan branch yoksa,
        // önce 'main' sonra 'master' var mı diye kontrol et
        if (targetBranch == null || repository.findRef(Constants.R_HEADS + targetBranch) == null) {
            if (repository.findRef(Constants.R_HEADS + "main") != null) {
                targetBranch = "main";
                log.warn("HEAD anlaşılamadı veya hedef branch yok, '{}' varsayılan olarak kullanılacak.", targetBranch);
            } else if (repository.findRef(Constants.R_HEADS + Constants.MASTER) != null) {
                targetBranch = Constants.MASTER;
                log.warn("HEAD anlaşılamadı veya hedef branch yok, '{}' varsayılan olarak kullanılacak.", targetBranch);
            } else {
                // Ne main ne master yoksa (çok nadir, sadece init sonrası olabilir)
                targetBranch = Constants.MASTER; // Veya main, standardınıza göre
                log.warn("Ne main ne master bulunamadı, '{}' varsayılan olarak kullanılacak.", targetBranch);
            }
        }

        log.debug("Varsayılan branch olarak '{}' belirlendi.", targetBranch);
        return targetBranch;
    }


    /** Belirtilen branch'i remote 'origin'e push eder (iç kullanım için). */
    private void pushBranchInternal(Git git, String branchName, CredentialsProvider cp) throws GitAPIException, IOException {
        log.info("'{}' branch'i push ediliyor...", branchName);
        String localRef = Constants.R_HEADS + branchName;
        String remoteRef = Constants.R_HEADS + branchName;

        // Lokal branch yoksa oluştur
        if (git.getRepository().findRef(localRef) == null) {
            log.info("Lokal branch '{}' bulunamadı, oluşturuluyor...", branchName);
            try {
                // Mevcut HEAD'den (muhtemelen son commit'ten) branch oluştur
                git.branchCreate().setName(branchName).call();
                log.info("Lokal branch '{}' başarıyla oluşturuldu.", branchName);
            } catch (GitAPIException e) {
                log.error("Lokal branch '{}' oluşturulurken hata: {}. Push işlemine devam edilemiyor.", branchName, e.getMessage());
                // Eğer develop yoksa ve oluşturulamazsa push başarısız olur.
                // Ana branch zaten init ile oluşmuş olabilir.
                if ("develop".equals(branchName)) { // Sadece develop için kritik
                    throw e;
                } else {
                    log.warn("Ana branch '{}' oluşturulamadı ama init ile oluşmuş olabilir, push deneniyor.", branchName);
                }
            }
        }

        PushCommand pushCommand = git.push();
        pushCommand.setRemote("origin");
        pushCommand.setRefSpecs(new RefSpec(localRef + ":" + remoteRef)); // Lokal:Remote eşlemesi
        pushCommand.setCredentialsProvider(cp);
        // pushCommand.setForce(true); // Force push GEREKMEZ ve tehlikelidir.

        try {
            pushCommand.call();
            log.info("'{}' branch'i başarıyla push edildi.", branchName);
        } catch (TransportException e) {
            log.error("'{}' branch'i push edilirken kimlik doğrulama/ağ hatası: {}", branchName, e.getMessage());
            throw e; // Hatayı yukarı fırlat
        } catch (GitAPIException e) {
            log.error("'{}' branch'i push edilirken Git hatası: {}", branchName, e.getMessage());
            // Hatanın detayını incelemek gerekebilir (örn: non-fast-forward)
            throw e; // Hatayı yukarı fırlat
        }
    }
}