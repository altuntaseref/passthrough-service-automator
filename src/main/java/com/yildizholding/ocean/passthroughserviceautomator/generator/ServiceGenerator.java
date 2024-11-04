package com.yildizholding.ocean.passthroughserviceautomator.generator;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceGenerator {

    private final TemplateGenerator templateGenerator;

    public void generateService(RestProjectRequest request) {
        try {
            String packageName = request.getPackageName();
            String className = FileUtils.capitalizeFirstLetter(request.getSystemName()) + "Service";
            String templateFile = "src/main/resources/templates/rest/serviceClass.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", packageName);
            args.put("className", className);
            args.put("systemName", FileUtils.capitalizeFirstLetter(request.getSystemName()));


            String module = request.generateProjectSrcMain() + "java\\" + packageName.replace(".", "\\") + "\\service";
            String fileName = module + "\\" + className + ".java";

            templateGenerator.generateFromTemplate(templateFile, fileName, args);

            log.info("Service sınıfı oluşturuldu: {}", className);

        } catch (Exception e) {
            log.error("Service sınıfı oluşturulurken hata oluştu", e);
        }
    }
}
