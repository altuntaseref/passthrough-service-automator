package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.service.ProjectInitializerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectInitializer {

    private final ProjectInitializerService projectInitializerService;

    public void createBaseProject(ProjectRequest request) {
        try {
            projectInitializerService.generateProject(request);
        } catch (Exception e) {
            log.error("ERROR for create base project", e);
        }
    }

    public void createPackages(ProjectRequest request) {
        try {
            projectInitializerService.createPackageStructure(request);
        } catch (Exception e) {
            log.error("ERROR for create package structure", e);
        }
    }
}
