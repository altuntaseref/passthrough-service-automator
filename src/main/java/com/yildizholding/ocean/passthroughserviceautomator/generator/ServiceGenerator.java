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
public class ServiceGenerator {

    private final TemplateGenerator templateGenerator;

    public void generateService(ProjectRequest request) {
        try {
            String packageName = request.getPackageName();
            String className = FileUtils.capitalizeFirstLetter(request.getSystemName()) + "Service";
            String templateFile = "src/main/resources/templates/rest/DynamicService.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", packageName);
            args.put("className", className);
            args.put("apiRequests", request.getApiRequests());
            args.put("authType", request.getAuthType());
            args.put("username", request.getUsername());
            args.put("password", request.getPassword());
            args.put("apiKey", request.getApiKey());
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
