package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.generator.ConfigGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.generator.ControllerGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.generator.ModelGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.generator.ServiceGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmptyProjectBuilder implements ProjectBuilder{

    private final ProjectInitializer projectInitializer;
    private final ModelGenerator modelGenerator;
    private final ServiceGenerator serviceGenerator;
    private final ControllerGenerator controllerGenerator;
    private final ConfigGenerator configGenerator;
    @Override
    public void createBaseProject(ProjectRequest request) {
        projectInitializer.createBaseProject(request);
    }

    @Override
    public void createPackages(ProjectRequest request) {
        projectInitializer.createPackages(request);
    }

    @Override
    public void updatePomXml(ProjectRequest request) {
        configGenerator.updatePomXml(request);
    }

    @Override
    public void createInboundRequestLoggingFilterConfig(ProjectRequest request) {
        configGenerator.createInboundRequestLoggingFilterConfig(request);
    }

    @Override
    public void createServiceConfig(ProjectRequest request) {
        configGenerator.createServiceConfig(request);
    }

    @Override
    public void createApplicationProperties(ProjectRequest request) {

    }

    @Override
    public void createBootStrap(ProjectRequest request) {
        configGenerator.createBootStrap(request);
    }

    @Override
    public void createLogbackXml(ProjectRequest request) {
        configGenerator.createLogbackXml(request);
    }

    @Override
    public void createSettingsXml(ProjectRequest request) {

    }

    @Override
    public void createSwaggerConfig(ProjectRequest request) {

    }

    @Override
    public void addConfigClasses(ProjectRequest request) {

    }

    @Override
    public void generateModelClasses(ProjectRequest request) {

    }

    @Override
    public void generateService(ProjectRequest projectRequest) {

    }

    @Override
    public void generateController(ProjectRequest request) {

    }

    @Override
    public Project getResult() {
        return new Project();
    }
}
