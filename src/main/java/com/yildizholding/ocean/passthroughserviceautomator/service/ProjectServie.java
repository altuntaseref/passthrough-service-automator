package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.builder.Project;
import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectBuilder;
import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilder;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie {

    private final ProjectInitializerService projectInitializerService;
    private final RestProjectBuilder restProjectBuilder;
    private final ProjectDirector projectDirector;

    public ProjectResponse generateProject(ProjectRequest request) {
        ProjectResponse response = new ProjectResponse();
        try {
            if (request.getProjectType().equalsIgnoreCase("REST")) {
                projectDirector.constructProject(restProjectBuilder, request);
                Project project = restProjectBuilder.getResult();

                // Proje yolunu ayarlayın
                response.setProjectPath(project.getProjectPath());

                // Servis kayıt işlemlerini gerçekleştirin ve yanıtları alın
                List<RegisterServiceResponse> registerServiceResponses = restProjectBuilder.generateOceanLinks(request);
                response.setRegisterServiceResponses(registerServiceResponses);

                response.setMessage("Proje başarıyla oluşturuldu.");
            } else {
                response.setMessage("Şu anda yalnızca REST projeleri desteklenmektedir.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setMessage("Proje oluşturulurken hata oluştu: " + e.getMessage());
        }
        return response;
    }



}
