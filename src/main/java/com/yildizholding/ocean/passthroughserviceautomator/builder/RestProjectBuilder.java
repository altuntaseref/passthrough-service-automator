package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;

public interface RestProjectBuilder extends ProjectBuilder {
    void createSwaggerConfig(RestProjectRequest request);
    void generateModelClasses(RestProjectRequest request);
    void generateService(RestProjectRequest restProjectRequest);
    void generateController(RestProjectRequest request);
    void createPackages(RestProjectRequest request);
}
