package com.yildizholding.ocean.passthroughserviceautomator.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.yildizholding.ocean.passthroughserviceautomator.config.ProjectConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;

@Getter
@Setter
public class RestProjectRequest  {

    protected String projectName;
    protected String packageName;
    private String baseUrl;
    private String username;
    private String password;
    private String apiKey;
    private String systemName;
    private Boolean sslCerIsRequired;
    private String gitlabRepoUrl;
    private boolean registerOnKong = true;

    public void generatePackageName() {
        try {
            String groupId = ProjectConfig.getInstance().getGroupId();
            if (projectName != null && !projectName.isEmpty()) {
                String generatedName = projectName.replace("-", "").toLowerCase();
                if (groupId != null && !groupId.isEmpty()) {
                    this.packageName = groupId + "." + generatedName;
                } else {
                    this.packageName = generatedName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String generateProjectPath() {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();
            String projectPath = outputPath + "\\" + projectName + "\\";
            return projectPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String generateProjectSrcMain() {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();
            String projectPath = outputPath + "\\" + projectName + "\\src\\main\\";
            return projectPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * Projenin src/test dizininin yolunu oluşturur.
     * Örn: C:\path\to\projects\my-project\src\test\
     *
     * @return Oluşturulan src/test yolu veya hata durumunda null.
     */
    public String generateProjectSrcTest() {
        try {
            // ProjectConfig singleton'ından çıktı yolunu al
            String outputPath = ProjectConfig.getInstance().getOutputPath();
            if (outputPath == null || projectName == null || projectName.isBlank()) {
                System.err.println("Hata: Proje adı veya çıktı yolu eksik/geçersiz.");
                return null;
            }
            // İşletim sisteminden bağımsız yol oluştur
            String projectPath = outputPath + File.separator + projectName + File.separator + "src" + File.separator + "test" + File.separator;
            return projectPath;
        } catch (Exception e) {
            // ProjectConfig veya path birleştirme sırasında hata olursa
            System.err.println("generateProjectSrcTest metodu hatası: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
