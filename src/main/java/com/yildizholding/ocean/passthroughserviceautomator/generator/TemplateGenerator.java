package com.yildizholding.ocean.passthroughserviceautomator.generator;

import com.yildizholding.ocean.passthroughserviceautomator.model.FreeMakeModel;
import com.yildizholding.ocean.passthroughserviceautomator.service.FreeMakerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateGenerator {

    private final FreeMakerService freeMakerService;

    public void generateFromTemplate(String templateFile, String fileName, Map<String, Object> args) {
        FreeMakeModel freeMakeModel = new FreeMakeModel();
        freeMakeModel.setArgs(args);
        freeMakeModel.setFileName(fileName);
        freeMakeModel.setTemplateFilePath(templateFile);

        freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
    }
}
