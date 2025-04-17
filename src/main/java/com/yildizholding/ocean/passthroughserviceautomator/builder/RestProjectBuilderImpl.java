package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.generator.ConfigGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.generator.ControllerGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.generator.ModelGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.generator.ServiceGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.model.Project;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceResponse;
import com.yildizholding.ocean.passthroughserviceautomator.service.ProjectInitializer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class RestProjectBuilderImpl implements RestProjectBuilder {

    private final ProjectInitializer projectInitializer;
    private final ModelGenerator modelGenerator;
    private final ServiceGenerator serviceGenerator;
    private final ControllerGenerator controllerGenerator;
    private final ConfigGenerator configGenerator;

    private List<RegisterServiceResponse> registerServiceResponses;

    @Override
    public void createBaseProject(RestProjectRequest request) {
        projectInitializer.createBaseProject(request);
    }

    @Override
    public void createPackages(RestProjectRequest request) {
        projectInitializer.createPackages(request);
    }

    @Override
    public void updatePomXml(RestProjectRequest request) {
        configGenerator.updatePomXml(request);
    }

    @Override
    public void createInboundRequestLoggingFilterConfig(RestProjectRequest request) {
        configGenerator.createInboundRequestLoggingFilterConfig(request);
    }

    @Override
    public void createServiceConfig(RestProjectRequest request) {
        configGenerator.createServiceConfig(request);
    }

    @Override
    public void createApplicationProperties(RestProjectRequest request) {
        configGenerator.createApplicationProperties(request);
    }

    @Override
    public void createBootStrap(RestProjectRequest request) {
        configGenerator.createBootStrap(request);
    }

    @Override
    public void createLogbackXml(RestProjectRequest request) {
        configGenerator.createLogbackXml(request);
    }

    @Override
    public void createSettingsXml(RestProjectRequest request) {
        configGenerator.createSettingsXml(request);
    }

    @Override
    public void createSwaggerConfig(RestProjectRequest request) {
        configGenerator.createSwaggerConfig(request);
    }

    @Override
    public void addConfigClasses(RestProjectRequest request) {
        // Gerekirse ekleyebilirsiniz
    }

//    @Override
//    public void generateModelClasses(RestProjectRequest request) {
//        modelGenerator.generateModelClasses(request);
//    }

    @Override
    public void generateService(RestProjectRequest request) {
        serviceGenerator.generateService(request);
    }

    @Override
    public void generateController(RestProjectRequest request) {
        controllerGenerator.generateController(request);
    }



    @Override
    public Project getResult() {
        Project project = new Project();
        project.setProjectPath("");
        project.setRegisterServiceResponses(this.registerServiceResponses);
        return project;
    }

}
