package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.Project;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceResponse;

import java.util.List;

public interface ProjectBuilder {

    void createBaseProject(RestProjectRequest request);

    void updatePomXml(RestProjectRequest request);
    void createInboundRequestLoggingFilterConfig(RestProjectRequest request);
    void createServiceConfig(RestProjectRequest request);
    void createApplicationProperties(RestProjectRequest request);
    void createBootStrap(RestProjectRequest request);
    void createLogbackXml(RestProjectRequest request);
    void createSettingsXml(RestProjectRequest request);
    void addConfigClasses(RestProjectRequest request);




    Project getResult();

}
