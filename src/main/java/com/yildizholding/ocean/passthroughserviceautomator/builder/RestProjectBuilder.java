package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.config.ProjectConfig;
import com.yildizholding.ocean.passthroughserviceautomator.model.FreeMakeModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.service.FreeMakerService;
import com.yildizholding.ocean.passthroughserviceautomator.service.ProjectInitializerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestProjectBuilder implements ProjectBuilder{

    private final ProjectInitializerService projectInitializerService;
    private final FreeMakerService freeMakerService;

    @Override
    public void createBaseProject(ProjectRequest request)  {
        try {
            projectInitializerService.generateProject(request);
        }catch (Exception e){
            log.error("ERROR for create base project");
        }

    }

    @Override
    public void updatePomXml(ProjectRequest request) {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();

            String module = request.getProjectName();
            String fileName = String.format(outputPath +"%s\\pom.xml", module);
            String templateFile = "src/main/resources/templates/rest/pomxml.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", module);


            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        }catch (Exception e){
            log.error("ERROR for update Pom.xml file...");
        }


    }

    @Override
    public void addCommonClasses(ProjectRequest request) {

    }

    @Override
    public void addConfigClasses(ProjectRequest request) {

    }

    @Override
    public void generateModelClasses(ProjectRequest request) {

    }

    @Override
    public void generateController(ProjectRequest request) {

    }

    @Override
    public Project getResult() {
        return null;
    }
}
