package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;

import java.io.IOException;

public interface ProjectBuilder {

    void createBaseProject(ProjectRequest request) ;
    void updatePomXml(ProjectRequest request);
    void addCommonClasses(ProjectRequest request);
    void addConfigClasses(ProjectRequest request);
    void generateModelClasses(ProjectRequest request);
    void generateController(ProjectRequest request);

    Project getResult();

}
