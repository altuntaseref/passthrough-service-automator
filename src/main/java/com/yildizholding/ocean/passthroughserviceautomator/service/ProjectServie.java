package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterResponseModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.results.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List; // Gerekli

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie { // -> ProjectService

    // Gerekli servisler
    private final CodeGenerationService codeGenerationService;
    private final KongIntegrationService kongIntegrationService;
    private final DocumentationGeneratorService documentationGeneratorService;
    private final GitLabService gitLabService;

    public ProjectResponse generateProject(RestProjectRequest request, MultipartFile postmanFile) {
        ProjectResponse response = new ProjectResponse();
        String projectPath = null;
        CodeGenerationResult codeGenResult = null;
        KongRegistrationResult kongResult = KongRegistrationResult.skipped();
        DocumentationGenerationResult docsResult = DocumentationGenerationResult.skipped();
        GitPushResult gitResult = GitPushResult.skipped(request.getGitlabRepoUrl());

        try {
            // --- 1. Hazırlık ---
            log.info("Adım 1: Hazırlık...");
            validatePreRequisites(request, postmanFile);
            if (request.getPackageName() == null) request.generatePackageName();
            projectPath = request.generateProjectPath();
            if (projectPath == null) throw new IllegalArgumentException("Proje yolu oluşturulamadı.");
            response.setProjectName(request.getProjectName());
            response.setPackageName(request.getPackageName());
            response.setProjectLocalPath(projectPath);
            String postmanContent = new String(postmanFile.getBytes(), StandardCharsets.UTF_8);
            log.info("Adım 1 tamamlandı.");


            // --- 2. Kod Oluşturma ---
            log.info("Adım 2: Kod Oluşturma...");
            codeGenResult = codeGenerationService.generateCode(request, postmanContent);
            if (!codeGenResult.isSkeletonSuccess()) {
                throw new RuntimeException("Proje iskeleti oluşturulamadı: " + codeGenResult.getErrorMessage());
            }
            log.info("Adım 2 tamamlandı. LLM Başarılı: {}", codeGenResult.isLlmSuccess());


            // --- 3. Kong Kaydı ---
            log.info("Adım 3: Kong Kaydı...");
            kongResult = kongIntegrationService.registerServiceOnKong(request); // İçinde attempt kontrolü var
            log.info("Adım 3 tamamlandı. Başarılı: {}", kongResult.isSuccess());


            // --- 4. Dokümantasyon ---
            log.info("Adım 4: Dokümantasyon Oluşturma...");
            OceanServiceRegisterResponseModel kongApiResponse = kongResult.isSuccess() ? mapKongResultToApiResponse(kongResult) : null;
            try {
                // Dosyaları yazar ve dosya adlarını döndürür
                docsResult = documentationGeneratorService.generateDocumentationFiles(request, kongApiResponse, projectPath);
                log.info("Adım 4 tamamlandı. Başarılı: {}", docsResult.isSuccess());
            } catch(Exception e) {
                log.error("Dokümantasyon adımı başarısız!", e);
                docsResult = DocumentationGenerationResult.failure(e.getMessage());
            }


            // --- 5. GitLab Push ---
            log.info("Adım 5: GitLab Push...");
            response.setGitlabRepoUrl(request.getGitlabRepoUrl()); // Repo URL'sini baştan set et (varsa)
            if (request.getGitlabRepoUrl() != null && !request.getGitlabRepoUrl().isBlank()) {
                try {
                    String commitMessage = buildCommitMessage(codeGenResult, kongResult, docsResult);
                    // Doküman listesini GitLab servisine gönder
                    gitResult = gitLabService.initializeCommitAndPush(
                            projectPath,
                            request.getGitlabRepoUrl(),
                            commitMessage,
                            docsResult.getGeneratedFileRelativePaths() // Göreli dosya yolları listesi
                    );
                    log.info("Adım 5 tamamlandı. Başarılı: {}", gitResult.isSuccess());
                } catch(Exception e) {
                    log.error("GitLab push adımı başarısız!", e);
                    // Hata durumunda gitResult'ı güncelle
                    gitResult = GitPushResult.builder()
                            .attempted(true).success(false)
                            .errorMessage(e.getMessage())
                            .repoUrl(request.getGitlabRepoUrl()).build();
                }
            } else {
                log.info("Adım 5 atlandı: GitLab URL'si yok.");
                // gitResult skipped olarak kalır
            }


            // --- Nihai Yanıtı Oluştur ---
            response.setOverallSuccess(true); // Kritik hata olmadıysa başarılı
            response.setMessage(generateFinalMessage(codeGenResult, kongResult, docsResult, gitResult));
            // Ana linkleri ve bilgileri ekle
            response.setKongServiceUrl(kongResult.getServiceUrl());
            // GitLab linklerini gitResult'tan al (başarılıysa dolu gelir)
            response.setGitlabReadmeUrl(gitResult.getReadmeUrl());
            response.setGitlabPostmanUrl(gitResult.getPostmanUrl());


        } catch (IllegalArgumentException | IOException | IllegalStateException e) { // Kritik ve ön koşul hataları
            log.error("Proje oluşturma sürecinde kritik bir hata oluştu!", e);
            response.setOverallSuccess(false);
            response.setMessage("Proje oluşturma başarısız: " + e.getMessage());
            response.setErrorDetails(e.toString());
        } catch (Exception e) { // Diğer beklenmedik kritik hatalar
            log.error("Proje oluşturma sürecinde beklenmedik kritik bir hata oluştu!", e);
            response.setOverallSuccess(false);
            response.setMessage("Beklenmedik kritik hata: " + e.getMessage());
            response.setErrorDetails(e.toString());
        }

        log.info("Proje oluşturma süreci tamamlandı. Genel Başarı: {}", response.isOverallSuccess());
        return response;
    }


    // --- Özel Yardımcı Metotlar ---

    private void validatePreRequisites(RestProjectRequest request, MultipartFile postmanFile) throws IllegalArgumentException {
        if (postmanFile == null || postmanFile.isEmpty()) throw new IllegalArgumentException("Postman dosyası eksik.");
        if (!StringUtils.hasText(request.getProjectName())) throw new IllegalArgumentException("Proje adı eksik.");
        if (!StringUtils.hasText(request.getSystemName())) throw new IllegalArgumentException("Sistem adı (SystemName) eksik.");
    }

    private String buildCommitMessage(CodeGenerationResult codeGen, KongRegistrationResult kong, DocumentationGenerationResult docs) {
        StringBuilder commitMsg = new StringBuilder("Automated project generation");
        if (codeGen != null && codeGen.isLlmSuccess()) commitMsg.append(" with LLM code"); // Null check eklendi
        if (kong != null && kong.isSuccess()) commitMsg.append(", Kong registration"); // Null check eklendi
        if (docs != null && docs.isSuccess()) commitMsg.append(", documentation files"); // Null check eklendi
        return commitMsg.toString();
    }

    private String generateFinalMessage(CodeGenerationResult codeGen, KongRegistrationResult kong, DocumentationGenerationResult docs, GitPushResult git) {
        if (codeGen==null || !codeGen.isSkeletonSuccess()){ // İskelet bile yoksa ana hata
            return "Proje oluşturma BAŞARISIZ: İskelet oluşturulamadı.";
        }

        StringBuilder msg = new StringBuilder("Proje oluşturma tamamlandı: İskelet OK");
        if(codeGen.isLlmAttempted()) msg.append(codeGen.isLlmSuccess() ? ", LLM OK" : ", LLM BAŞARISIZ");
        if(kong.isAttempted()) msg.append(kong.isSuccess() ? ", Kong OK" : ", Kong BAŞARISIZ"); else msg.append(", Kong Atlandı");
        if(docs.isAttempted()) msg.append(docs.isSuccess() ? ", Dokümanlar OK" : ", Dokümanlar BAŞARISIZ"); else msg.append(", Dokümanlar Atlandı");
        if(git.isAttempted()) msg.append(git.isSuccess() ? ", Git OK" : ", Git BAŞARISIZ"); else msg.append(", Git Atlandı");

        return msg.toString();
    }

    // Kong sonucunu eski modele dönüştürmek için basit bir yardımcı (DocumentationGeneratorService için)
    private OceanServiceRegisterResponseModel mapKongResultToApiResponse(KongRegistrationResult kongResult) {
        if (kongResult == null || !kongResult.isSuccess()) return null;
        OceanServiceRegisterResponseModel apiResponse = new OceanServiceRegisterResponseModel();
        apiResponse.setResult("Success");
        apiResponse.setServiceLink(kongResult.getServiceUrl());
        apiResponse.setServiceUsername(kongResult.getUsername());
        return apiResponse;
    }
}