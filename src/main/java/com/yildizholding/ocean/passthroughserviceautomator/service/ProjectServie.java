package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.Project;
import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilderImpl;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie {

    private final ProjectInitializerService projectInitializerService;
    private final RestProjectBuilderImpl restProjectBuilderImpl;
    private final ProjectDirector projectDirector;

    public ProjectResponse generateProject(RestProjectRequest request) {
        ProjectResponse response = new ProjectResponse();
        try {

                projectDirector.constructProject(restProjectBuilderImpl, request);
                Project project = restProjectBuilderImpl.getResult();
                response.setProjectPath(project.getProjectPath());



                response.setMessage("Proje başarıyla oluşturuldu.");
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Proje oluşturulurken hata oluştu: " + e.getMessage());
        }
        return response;
    }



}
