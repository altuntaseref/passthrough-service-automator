package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import org.springframework.stereotype.Service;

@Service
public class ProjectDirector {

    public void constructProject(RestProjectBuilder builder, RestProjectRequest restProjectRequest){
        builder.createBaseProject(restProjectRequest);
        builder.createPackages(restProjectRequest);
        builder.updatePomXml(restProjectRequest);
        builder.createInboundRequestLoggingFilterConfig(restProjectRequest);
        builder.createServiceConfig(restProjectRequest);
        builder.createApplicationProperties(restProjectRequest);
        builder.createBootStrap(restProjectRequest);
        builder.createSettingsXml(restProjectRequest);
        builder.createSwaggerConfig(restProjectRequest);
        builder.createLogbackXml(restProjectRequest);
        builder.addConfigClasses(restProjectRequest);
        builder.generateService(restProjectRequest);
        builder.generateController(restProjectRequest);

    }


}
