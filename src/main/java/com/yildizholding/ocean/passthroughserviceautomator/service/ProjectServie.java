package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.builder.Project;
import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectBuilder;
import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilder;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie {

    private final ProjectInitializerService projectInitializerService;
    private final RestProjectBuilder restProjectBuilder;
    private final ProjectDirector projectDirector;

    public String generateProject(ProjectRequest request) {
        try {

            if (request.getProjectType().equalsIgnoreCase("REST")) {
                projectDirector.constructProject(restProjectBuilder, request);
                Project project = restProjectBuilder.getResult();
                return "Proje başarıyla oluşturuldu: " + project.getProjectPath();
            } else {
                return "Şu anda yalnızca REST projeleri desteklenmektedir.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Proje oluşturulurken hata oluştu: " + e.getMessage();
        }
    }


}
