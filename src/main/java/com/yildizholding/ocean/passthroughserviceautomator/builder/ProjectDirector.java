package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import org.springframework.stereotype.Service;

@Service
public class ProjectDirector {

    public void constructProject(ProjectBuilder builder, ProjectRequest projectRequest){
        builder.createBaseProject(projectRequest);
        builder.updatePomXml(projectRequest);
        builder.addCommonClasses(projectRequest);
        builder.addConfigClasses(projectRequest);
        builder.generateModelClasses(projectRequest);
        builder.generateController(projectRequest);
    }

}
