package com.yildizholding.ocean.passthroughserviceautomator.model.results;

import com.yildizholding.ocean.passthroughserviceautomator.model.results.*; // Yeni result modellerini import et
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectResponse {

    private boolean overallSuccess;
    private String message; // Genel özet mesaj

    // Ana Çıktılar
    private String projectName;
    private String packageName;
    private String projectLocalPath; // Belki bu da kaldırılabilir veya sadece debug için?
    private String gitlabRepoUrl; // Ana repo linki
    private String kongServiceUrl; // Ana Kong linki

    // Adım Detayları (Opsiyonel, sadece loglama veya debug için gerekirse)
    // private CodeGenerationResult codeGenerationResult;
    // private KongRegistrationResult kongRegistrationResult;
    // private DocumentationGenerationResult documentationGenerationResult;
    // private GitPushResult gitPushResult;

    private String errorDetails; // Kritik hata detayı
}