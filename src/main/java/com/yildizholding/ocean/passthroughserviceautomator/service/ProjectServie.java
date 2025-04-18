package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilderImpl;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterResponseModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie { // Sınıf adı ProjectService olmalı

    // Gerekli servisler
    private final RestProjectBuilderImpl restProjectBuilder;
    private final ProjectDirector projectDirector;
    private final ResourceReaderService resourceReaderService; // Eklendi (Controller okumak için)
    private final PromptGenerationService promptGenerationService; // Eklendi
    private final CodeEnhancementService codeEnhancementService;
    private final GeneratedCodeWriterService codeWriterService;
    private final KongIntegrationService kongIntegrationService;
    private final DocumentationGeneratorService documentationGeneratorService;
    private final GitLabService gitLabService;

    public ProjectResponse generateProject(RestProjectRequest request, MultipartFile postmanFile) {
        ProjectResponse response = new ProjectResponse();
        String projectPath = request.generateProjectPath();
        response.setProjectPath(projectPath); // Proje yolunu başta ayarla

        OceanServiceRegisterResponseModel kongData = null;
        List<String> generatedDocFiles = List.of();
        String finalCommitMessage = "Automated project generation";

        try {
            // --- 1. Hazırlık ve İskelet ---
            log.info("Adım 1: Hazırlık ve İskelet Oluşturma...");
            if (projectPath == null) throw new IllegalArgumentException("Proje yolu oluşturulamadı.");
            if (postmanFile == null || postmanFile.isEmpty()) throw new IllegalArgumentException("Postman dosyası eksik.");
            String postmanContent = new String(postmanFile.getBytes(), StandardCharsets.UTF_8); // getBytes düzeltildi
            // Gerekirse packageName'i oluştur
            if (request.getPackageName() == null || request.getPackageName().isBlank()) {
                request.generatePackageName();
                if(request.getPackageName() == null) throw new IllegalStateException("Paket adı oluşturulamadı.");
            }
            projectDirector.constructProject(restProjectBuilder, request);
            log.info("Adım 1 tamamlandı.");

            // --- 2. LLM ile Kod Geliştirme ve Yazma ---
            log.info("Adım 2: LLM ile Kod Geliştirme...");
            // Prompt oluşturmak için gerekli şablonları oku
            String controllerTemplate = resourceReaderService.readGeneratedTemplateContent(request, "controller")
                    .orElseThrow(() -> new IOException("Başlangıç Controller şablonu okunamadı."));
            String serviceTemplate = resourceReaderService.readGeneratedTemplateContent(request, "service")
                    .orElseThrow(() -> new IOException("Başlangıç Service şablonu okunamadı."));
            // Prompt'u oluştur
            String prompt = promptGenerationService.buildLlmPrompt(request, controllerTemplate, serviceTemplate, postmanContent);
            // LLM'i çağır ve kodu ayrıştır/yaz (CodeEnhancementService içinde yapılmalı)
            Optional<Map<Path, String>> codeMapOpt = codeEnhancementService.enhanceCode(request, prompt); // enhanceCode prompt almalı

            if (codeMapOpt.isPresent() && !codeMapOpt.get().isEmpty()) {
                codeWriterService.writeCodeFiles(codeMapOpt.get(), request);
                log.info("Adım 2 tamamlandı: LLM kodları yazıldı.");
                finalCommitMessage += " with LLM enhancements";
            } else {
                log.warn("Adım 2: LLM kodları alınamadı/boş. Sadece iskelet kullanılacak.");
            }

            // --- 3. Kong Kaydı (İsteğe Bağlı) ---
            if (request.isRegisterOnKong()) { // isRegisterOnKong düzeltildi (modelde olmalı)
                log.info("Adım 3: Servis Kong'a kaydediliyor...");
                try {
                    kongData = kongIntegrationService.registerServiceOnKong(request);
                    if (kongData != null && "Success".equalsIgnoreCase(kongData.getResult())) {
                        log.info("Adım 3 tamamlandı: Kong kaydı başarılı.");
                        finalCommitMessage += ", Kong registration";
                    } else {
                        log.error("Adım 3 başarısız: Kong kaydı yapılamadı veya başarısız yanıt.");
                    }
                } catch (Exception e) {
                    log.error("Adım 3 başarısız: Kong kaydı sırasında hata oluştu!", e);
                }
            } else {
                log.info("Adım 3 atlandı: Kong kaydı istenmedi.");
            }

            // --- 4. Dokümantasyon Oluşturma ---
            log.info("Adım 4: Dokümantasyon (Readme, Postman) oluşturuluyor...");
            try {
                generatedDocFiles = documentationGeneratorService.generateDocumentation(request, kongData, projectPath); // projectPath eklendi
                if (!generatedDocFiles.isEmpty()) {
                    log.info("Adım 4 tamamlandı: Dokümantasyon dosyaları oluşturuldu: {}", generatedDocFiles);
                    finalCommitMessage += " and documentation";
                } else {
                    log.warn("Adım 4: Dokümantasyon dosyaları oluşturulamadı.");
                }
            } catch (Exception e) {
                log.error("Adım 4 başarısız: Dokümantasyon oluşturulurken hata oluştu!", e);
            }

            // --- 5. GitLab Push (İsteğe Bağlı - EN SONDA) ---
            if (request.getGitlabRepoUrl() != null && !request.getGitlabRepoUrl().isBlank()) {
                log.info("Adım 5: Proje GitLab deposuna push edilecek (tüm dosyalar)...");
                try {
                    gitLabService.initializeCommitAndPush(projectPath, request.getGitlabRepoUrl(), finalCommitMessage); // initializeCommitAndPush düzeltildi
                    log.info("Adım 5 tamamlandı: Proje başarıyla GitLab'e push edildi.");
                    response.setMessage("Proje başarıyla oluşturuldu ve GitLab'e push edildi."); // Genel başarı
                } catch (Exception e) {
                    log.error("Adım 5 başarısız: Proje GitLab'e push edilirken hata oluştu!", e);
                    response.setMessage("Proje oluşturuldu, ancak GitLab'e push edilemedi: " + e.getMessage());
                }
            } else {
                log.info("Adım 5 atlandı: GitLab URL'si sağlanmadı.");
                // Önceki adımlara göre mesaj ayarla, Git push atlandıysa
                if (response.getMessage() == null) { // Eğer önceki adımlarda hata mesajı set edilmediyse
                    response.setMessage("Proje başarıyla oluşturuldu (GitLab push atlandı).");
                }
            }

            // --- Nihai Mesaj Güncellemesi (Opsiyonel Hatalar İçin) ---
            if (request.isRegisterOnKong() && (kongData == null || !"Success".equalsIgnoreCase(kongData.getResult()))) {
                response.setMessage( (response.getMessage() != null ? response.getMessage() : "Proje oluşturuldu") + " (Kong kaydı başarısız)");
            }
            if (generatedDocFiles.isEmpty() && kongData != null && "Success".equalsIgnoreCase(kongData.getResult()) ) { // Kong başarılı ama doküman yoksa
                response.setMessage( (response.getMessage() != null ? response.getMessage() : "Proje oluşturuldu") + " (Dokümantasyon oluşturulamadı)");
            }


        } catch (IllegalArgumentException e) {
            log.error("Ön koşul hatası: {}", e.getMessage());
            response.setMessage("İstek hatası: " + e.getMessage());
        } catch (IOException e) {
            log.error("Kritik dosya işlemi hatası!", e);
            response.setMessage("Kritik dosya hatası: " + e.getMessage());
        } catch (Exception e) {
            log.error("Proje oluşturma sürecinde kritik bir hata oluştu!", e);
            response.setMessage("Proje oluşturulurken kritik hata: " + e.getMessage());
        }
        return response;
    }
}