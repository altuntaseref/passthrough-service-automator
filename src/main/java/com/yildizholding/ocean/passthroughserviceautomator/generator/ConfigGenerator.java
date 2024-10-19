package com.yildizholding.ocean.passthroughserviceautomator.generator;

import com.yildizholding.ocean.passthroughserviceautomator.config.ProjectConfig;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigGenerator {

    private final TemplateGenerator templateGenerator;

    public void updatePomXml(ProjectRequest request) {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();

            String module = request.getProjectName();
            String fileName = String.format(outputPath + "%s\\pom.xml", module);
            String templateFile = "src/main/resources/templates/rest/pomxml.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", module);

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...", e);
        }
    }

    public void createInboundRequestLoggingFilterConfig(ProjectRequest request) {
        try {
            String packagePath = request.getPackageName().replace(".", File.separator);
            String module = request.generateProjectSrcMain() + "java\\" + packagePath + "\\config";
            String fileName = String.format("%s\\InboundRequestLoggingFilterConfig.java", module);
            String templateFile = "src/main/resources/templates/rest/InboundRequestLoggingFilterConfig.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", request.getPackageName());

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("ERROR for create InboundRequestLoggingFilterConfig...", e);
        }
    }

    public void createServiceConfig(ProjectRequest request) {
        try {
            String packagePath = request.getPackageName().replace(".", File.separator);
            String module = request.generateProjectSrcMain() + "java\\" + packagePath + "\\config";
            String className = FileUtils.capitalizeFirstLetter(request.getSystemName()) + "ServiceConfig.java";
            String fileName = module + "\\" + className;
            String templateFile = "src/main/resources/templates/rest/ServiceConfig.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", request.getPackageName());
            args.put("systemName", FileUtils.capitalizeFirstLetter(request.getSystemName()));

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("ERROR for create ServiceConfig...", e);
        }
    }

    public void createApplicationProperties(ProjectRequest request) {
        try {
            String module = request.generateProjectSrcMain() + "resources";
            String fileName = String.format("%s\\application-dev.properties", module);
            String templateFile = "src/main/resources/templates/rest/applicationProperties.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("baseUrl", request.getBaseUrl());
            args.put("username", request.getUsername());
            args.put("password", request.getPassword());
            args.put("apiKey", request.getApiKey());
            args.put("systemName", request.getSystemName());

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("Application properties oluşturulurken hata oluştu", e);
        }
    }

    public void createBootStrap(ProjectRequest request) {
        try {
            String module = request.generateProjectSrcMain() + "resources";
            String fileName = String.format("%s\\bootstrap.properties", module);
            String templateFile = "src/main/resources/templates/rest/bootstrap.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("ERROR for create bootstrap.properties...", e);
        }
    }

    public void createLogbackXml(ProjectRequest request) {
        try {
            String module = request.generateProjectSrcMain() + "resources";
            String fileName = String.format("%s\\logback.xml", module);
            String templateFile = "src/main/resources/templates/rest/logbackXml.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("CONSOLE_LOG_PATTERN", "${CONSOLE_LOG_PATTERN}");

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("ERROR for create logback.xml...", e);
        }
    }

    public void createSettingsXml(ProjectRequest request) {
        try {
            String module = request.generateProjectPath();
            String fileName = String.format("%s\\settings.xml", module);
            String templateFile = "src/main/resources/templates/rest/settingsXml.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("yildizRepo", "${yildiz-repo.url}");
            args.put("yildizSnapshots", "${yildiz-snapshots.url}");

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("ERROR for create settings.xml...", e);
        }
    }

    public void createSwaggerConfig(ProjectRequest request) {
        try {
            String packagePath = request.getPackageName().replace(".", File.separator);
            String module = request.generateProjectSrcMain() + "java\\" + packagePath + "\\config";
            String fileName = String.format("%s\\SwaggerConfig.java", module);
            String templateFile = "src/main/resources/templates/rest/SwaggerConfig.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("packageName", request.getPackageName());

            templateGenerator.generateFromTemplate(templateFile, fileName, args);
        } catch (Exception e) {
            log.error("ERROR for create SwaggerConfig...", e);
        }
    }
}
