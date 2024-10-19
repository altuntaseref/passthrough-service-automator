package com.yildizholding.ocean.passthroughserviceautomator.generator;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ControllerGenerator {

    private final TemplateGenerator templateGenerator;

    public void generateController(ProjectRequest request) {
        try {
            String packageName = request.getPackageName();
            String className = FileUtils.capitalizeFirstLetter(request.getSystemName()) + "Controller";
            String serviceClassName = FileUtils.capitalizeFirstLetter(request.getSystemName()) + "Service";
            String serviceVarName = FileUtils.decapitalizeFirstLetter(serviceClassName);
            String templateFile = "src/main/resources/templates/rest/DynamicController.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", packageName);
            args.put("className", className);
            args.put("serviceClassName", serviceClassName);
            args.put("serviceVarName", serviceVarName);
            args.put("apiRequests", request.getApiRequests());

            String module = request.generateProjectSrcMain() + "java\\" + packageName.replace(".", "\\") + "\\controller";
            String fileName = module + "\\" + className + ".java";

            templateGenerator.generateFromTemplate(templateFile, fileName, args);

            log.info("Controller sınıfı oluşturuldu: {}", className);

        } catch (Exception e) {
            log.error("Controller sınıfı oluşturulurken hata oluştu", e);
        }
    }
}
