package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectInitializer {

    private final ProjectInitializerService projectInitializerService;

    public void createBaseProject(RestProjectRequest request) {
        try {
            projectInitializerService.generateProject(request);
        } catch (Exception e) {
            log.error("ERROR for create base project", e);
        }
    }

    public void createPackages(RestProjectRequest request) {
        try {
            projectInitializerService.createPackageStructure(request);
        } catch (Exception e) {
            log.error("ERROR for create package structure", e);
        }
    }
}
