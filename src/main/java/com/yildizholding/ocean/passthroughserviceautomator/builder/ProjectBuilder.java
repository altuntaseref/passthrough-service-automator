package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;

import java.io.IOException;

public interface ProjectBuilder {

    void createBaseProject(ProjectRequest request);
    void createPackages(ProjectRequest request);
    void updatePomXml(ProjectRequest request);
    void createInboundRequestLoggingFilterConfig(ProjectRequest request);
    void createServiceConfig(ProjectRequest request);
    void createApplicationProperties(ProjectRequest request);
    void createBootStrap(ProjectRequest request);
    void createLogbackXml(ProjectRequest request);
    void createSettingsXml(ProjectRequest request);
    void createSwaggerConfig(ProjectRequest request);
    void addConfigClasses(ProjectRequest request);
    void generateModelClasses(ProjectRequest request);
    void generateService(ProjectRequest projectRequest);
    void generateController(ProjectRequest request);

    Project getResult();

}
