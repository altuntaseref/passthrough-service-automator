package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilderImpl;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterResponseModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie { // Sınıf adını ProjectService olarak değiştirmek daha iyi olabilir

    // Ana Orkestrasyon Bağımlılıkları
    private final RestProjectBuilderImpl restProjectBuilderImpl; // Veya Interface'i: RestProjectBuilder
    private final ProjectDirector projectDirector;
    private final CodeEnhancementService codeEnhancementService;
    private final GeneratedCodeWriterService codeWriterService;
    private final GitLabService gitLabService; // GitLab servisi inject edildi
    private final KongIntegrationService kongIntegrationService; // Kong entegrasyon servisi inject edildi


    /**
     * Yeni bir REST projesi oluşturur.
     */
    public ProjectResponse generateProject(RestProjectRequest request, MultipartFile postmanFile) {
        ProjectResponse response = new ProjectResponse();
        String projectPath = request.generateProjectPath();

        boolean gitPushAttempted = false; // Git push denendi mi?
        boolean gitPushSucceeded = false; // Git push başarılı mı?

        boolean projectFilesGenerated = false;
        if (projectPath == null) {
            response.setMessage("Proje yolu oluşturulamadı.");
            log.error("Proje yolu null döndü.");
            return response;
        }
        response.setProjectPath(projectPath);

        try {
            // --- Postman Dosyasını Oku ---
            if (postmanFile == null || postmanFile.isEmpty()) {
                throw new IllegalArgumentException("Postman Collection dosyası yüklenmedi veya boş.");
            }
            log.info("Yüklenen Postman dosyası okunuyor: {}", postmanFile.getOriginalFilename());
            String postmanCollectionContent = new String(postmanFile.getBytes(), StandardCharsets.UTF_8);
            log.info("Postman dosyası başarıyla okundu.");

            // --- Aşama 1: Proje İskeletini Oluştur ---
            log.info("Aşama 1: Proje iskeleti ve şablon tabanlı sınıflar oluşturuluyor...");
            projectDirector.constructProject(restProjectBuilderImpl, request);
            log.info("Aşama 1 tamamlandı. Proje iskeleti: {}", projectPath);

            // --- Aşama 2: LLM ile Kodu Geliştir ve Yaz ---
            log.info("Aşama 2: LLM ile kod geliştirme ve yazma başlatılıyor...");
            Optional<Map<Path, String>> aiGeneratedCodeMapOpt = codeEnhancementService.enhanceCode(request, postmanCollectionContent);

            if (aiGeneratedCodeMapOpt.isPresent() && !aiGeneratedCodeMapOpt.get().isEmpty()) {
                codeWriterService.writeCodeFiles(aiGeneratedCodeMapOpt.get(), request);
                response.setMessage("Proje başarıyla oluşturuldu ve LLM kodları entegre edildi.");
                projectFilesGenerated = true;
                log.info("Aşama 2 tamamlandı.");
            } else {
                response.setMessage("Proje iskeleti oluşturuldu, ancak LLM kodları alınamadı/boş. Şablonlar kullanılıyor.");
                projectFilesGenerated = true;
                log.warn("Aşama 2 tamamlanamadı veya LLM yanıtı boş.");
            }
            // Sadece proje dosyaları başarıyla oluşturulduysa ve GitLab URL'si varsa push et
            if (projectFilesGenerated && request.getGitlabRepoUrl() != null && !request.getGitlabRepoUrl().isBlank()) {
                log.info("Aşama 3: Proje GitLab deposuna push edilecek...");
                try {
                    gitLabService.pushProjectToGitLab(projectPath, request.getGitlabRepoUrl());
                    gitPushSucceeded = true;
                    // Başarılı mesajını ayarla
                    if (aiGeneratedCodeMapOpt.isPresent() && !aiGeneratedCodeMapOpt.get().isEmpty()) {
                        response.setMessage("Proje başarıyla oluşturuldu, LLM kodları entegre edildi ve GitLab'e push edildi.");
                    } else {
                        response.setMessage("Proje iskeleti başarıyla oluşturuldu ve GitLab'e push edildi (LLM kodu olmadan).");
                    }
                    log.info("Aşama 3 tamamlandı: Proje GitLab'e başarıyla push edildi.");
                } catch (GitAPIException | IOException | IllegalArgumentException e) {
                    gitPushSucceeded = false;
                    log.error("Proje GitLab'e push edilirken hata oluştu!", e);
                    // Hata mesajını ayarla ama işlem başarılı kabul edilebilir (yerelde oluşturuldu)
                    if (aiGeneratedCodeMapOpt.isPresent() && !aiGeneratedCodeMapOpt.get().isEmpty()) {
                        response.setMessage("Proje başarıyla oluşturuldu ve LLM kodları entegre edildi, ancak GitLab'e pushlanamadı: " + e.getMessage());
                    } else {
                        response.setMessage("Proje iskeleti başarıyla oluşturuldu, ancak GitLab'e pushlanamadı: " + e.getMessage());
                    }
                }
            } else if (projectFilesGenerated) {
                // GitLab URL yoksa bilgilendirme mesajı
                log.info("GitLab URL'si sağlanmadığı için GitLab'e pushlama atlandı.");
                if (aiGeneratedCodeMapOpt.isPresent() && !aiGeneratedCodeMapOpt.get().isEmpty()) {
                    response.setMessage("Proje başarıyla oluşturuldu ve LLM kodları entegre edildi (GitLab push atlandı).");
                } else {
                    response.setMessage("Proje iskeleti başarıyla oluşturuldu (GitLab push atlandı).");
                }
            } else {
                // Eğer proje dosyaları hiç oluşturulamadıysa (Aşama 1 veya 2'de hata)
                response.setMessage("Proje dosyaları oluşturulamadığı için GitLab'e pushlanamadı.");
            }

            // --- Aşama 4: Kong'a Kaydet (Eğer proje oluşturulduysa) ---
            if (projectFilesGenerated) {
                log.info("Aşama 4: Servis Kong API Gateway'e kaydedilecek...");
                OceanServiceRegisterResponseModel kongResponse = kongIntegrationService.registerServiceOnKong(request);

                // --- Nihai Yanıt Mesajını Oluştur ---
                if (kongResponse != null && "Success".equalsIgnoreCase(kongResponse.getResult())) {
                    log.info("Aşama 4 tamamlandı: Kong kaydı başarılı.");
                    if (gitPushAttempted && gitPushSucceeded) {
                        response.setMessage("Proje oluşturuldu, GitLab'e push edildi ve Kong'a kaydedildi.");
                    } else if (gitPushAttempted && !gitPushSucceeded) {
                        response.setMessage("Proje oluşturuldu, Kong'a kaydedildi ancak GitLab'e push edilemedi.");
                    } else { // Git push hiç denenmedi
                        response.setMessage("Proje oluşturuldu ve Kong'a kaydedildi (GitLab push atlandı).");
                    }
                } else { // Kong kaydı başarısız
                    log.error("Kong servis kaydı başarısız oldu.");
                    if (gitPushAttempted && gitPushSucceeded) {
                        response.setMessage("Proje oluşturuldu ve GitLab'e push edildi, ancak Kong kaydı başarısız.");
                    } else if (gitPushAttempted && !gitPushSucceeded) {
                        response.setMessage("Proje oluşturuldu, ancak GitLab push ve Kong kaydı başarısız.");
                    } else { // Git push hiç denenmedi
                        response.setMessage("Proje oluşturuldu, ancak Kong kaydı başarısız (GitLab push atlandı).");
                    }
                }
            } else {
                // Eğer proje dosyaları hiç oluşturulamadıysa
                response.setMessage("Proje dosyaları oluşturulamadığı için sonraki adımlar (GitLab/Kong) atlandı.");
            }

        } catch (IllegalArgumentException e) {
            log.error("Geçersiz istek: {}", e.getMessage());
            response.setMessage("İstek hatası: " + e.getMessage());
        } catch (IOException e) {
            log.error("Dosya işlemi sırasında hata oluştu!", e);
            response.setMessage("Dosya işlemi hatası: " + e.getMessage());
        } catch (Exception e) {
            log.error("Proje oluşturma sırasında kritik bir hata oluştu!", e);
            response.setMessage("Proje oluşturulurken hata oluştu: " + e.getMessage());
        }
        return response;
    }
}