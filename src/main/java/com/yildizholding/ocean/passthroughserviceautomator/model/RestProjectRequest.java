package com.yildizholding.ocean.passthroughserviceautomator.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.yildizholding.ocean.passthroughserviceautomator.config.ProjectConfig;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
//    private String postmanCollectionJson; // Postman Collection JSON içeriğini tutacak

//    private String authenticationPrompt;
//    private List<String> methodPrompts;
//    private List<ApiRequest> apiRequests;



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

}
