package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import org.springframework.stereotype.Service;

@Service
public class ProjectDirector {

    public void constructProject(ProjectBuilder builder, ProjectRequest projectRequest){
        builder.createBaseProject(projectRequest);
        builder.createPackages(projectRequest);
        builder.updatePomXml(projectRequest);
        builder.createInboundRequestLoggingFilterConfig(projectRequest);
        builder.createServiceConfig(projectRequest);
        builder.createApplicationProperties(projectRequest);
        builder.createBootStrap(projectRequest);
        builder.createSettingsXml(projectRequest);
        builder.createSwaggerConfig(projectRequest);
        builder.createLogbackXml(projectRequest);
        builder.addConfigClasses(projectRequest);
        builder.generateModelClasses(projectRequest);
        builder.generateService(projectRequest);
        builder.generateController(projectRequest);

    }

}
