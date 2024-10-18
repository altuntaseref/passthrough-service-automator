package com.yildizholding.ocean.passthroughserviceautomator.model;

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
    private String httpMethod; // "GET", "POST", "PUT", "DELETE"
    private String authType; // "None", "Basic", "Bearer"
    private boolean parameterInBody;
    private String jsonSchema;
    private String modelClassName;
    private String controllerClassName;
    private String endpoint;

}
