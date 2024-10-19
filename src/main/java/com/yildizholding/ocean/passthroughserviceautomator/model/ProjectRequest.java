package com.yildizholding.ocean.passthroughserviceautomator.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

@Data
public class ProjectRequest {
    private String projectName;
    private String projectType; // "REST" veya "SOAP"
    private String groupId;
    private String artifactId;
    private String packageName;
    private String outputPath;
    private List<String> dependencies;
    private String javaVersion;
    private String springBootVersion;
    private String authType; // "None", "Basic", "Bearer", vb.
    private String baseUrl;
    private String username;
    private String password;
    private String apiKey;
    private String systemName;
    private JsonNode jsonBody; // Genel JSON gövdesi (opsiyonel)
    private List<ApiRequest> apiRequests;

    public void generatePackageName() {
        if (projectName != null && !projectName.isEmpty()) {
            String generatedName = projectName.replace("-", "").toLowerCase();
            if (groupId != null && !groupId.isEmpty()) {
                this.packageName = groupId + "." + generatedName;
            } else {
                this.packageName = generatedName;
            }
        }
    }

    public String generateProjectPath(){
        String projectPath = outputPath+"\\"+projectName+"\\";
        return projectPath;
    }

    public String generateProjectSrcMain(){
        String projectPath = outputPath+"\\"+projectName+"\\src\\main\\";
        return projectPath;
    }

}
