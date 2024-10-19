package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JCodeModel;
import com.yildizholding.ocean.passthroughserviceautomator.config.ProjectConfig;
import com.yildizholding.ocean.passthroughserviceautomator.model.ApiRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.FreeMakeModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.service.FreeMakerService;
import com.yildizholding.ocean.passthroughserviceautomator.service.ProjectInitializerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestProjectBuilder implements ProjectBuilder {

    private final ProjectInitializerService projectInitializerService;
    private final FreeMakerService freeMakerService;

    @Override
    public void createBaseProject(ProjectRequest request) {
        try {
            projectInitializerService.generateProject(request);
        } catch (Exception e) {
            log.error("ERROR for create base project");
        }

    }

    @Override
    public void createPackages(ProjectRequest request) {
        try {
            projectInitializerService.createPackageStructure(request);
        } catch (Exception e) {
            log.error("ERROR for create base project");
        }
    }

    @Override
    public void updatePomXml(ProjectRequest request) {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();

            String module = request.getProjectName();
            String fileName = String.format(outputPath + "%s\\pom.xml", module);
            String templateFile = "src/main/resources/templates/rest/pomxml.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", module);


            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...");
        }


    }

    @Override
    public void createInboundRequestLoggingFilterConfig(ProjectRequest request) {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();
            String packagePath = request.getPackageName().replace(".", File.separator);
            String module = request.generateProjectSrcMain()+"java\\"+packagePath+"\\config";
            String fileName = String.format("%s\\InboundRequestLoggingFilterConfig.java", module);
            String templateFile = "src/main/resources/templates/rest/InboundRequestLoggingFilterConfig.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", request.getPackageName());


            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...");
        }
    }

    @Override
    public void createServiceConfig(ProjectRequest request) {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();
            String packagePath = request.getPackageName().replace(".", File.separator);
            String module = request.generateProjectSrcMain()+"java\\"+packagePath+"\\config";
            String className = capitalizeFirstLetter(request.getSystemName())+"ServiceConfig.java";
            String fileName = module+"\\"+className;
            String templateFile = "src/main/resources/templates/rest/ServiceConfig.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", request.getPackageName());
            args.put("systemName", request.getSystemName());


            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...");
        }
    }

    @Override
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

            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("Application properties oluşturulurken hata oluştu", e);
        }
    }


    @Override
    public void createBootStrap(ProjectRequest request) {
        try {

            String module = request.generateProjectSrcMain()+"resources";
            String fileName = String.format("%s\\bootstrap.properties", module);
            String templateFile = "src/main/resources/templates/rest/bootstrap.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());

            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...");
        }
    }

    @Override
    public void createLogbackXml(ProjectRequest request) {
        try {

            String module = request.generateProjectSrcMain()+"resources";
            String fileName = String.format("%s\\logback.xml", module);
            String templateFile = "src/main/resources/templates/rest/logbackXml.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("CONSOLE_LOG_PATTERN","${CONSOLE_LOG_PATTERN}");

            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...");
        }
    }

    @Override
    public void createSettingsXml(ProjectRequest request) {
        try {

            String module = request.generateProjectPath();
            String fileName = String.format("%s\\settings.xml", module);
            String templateFile = "src/main/resources/templates/rest/settingsXml.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("yildizRepo", "${yildiz-repo.url}");
            args.put("yildizSnapshots", "${yildiz-snapshots.url}");

            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...");
        }
    }

    @Override
    public void createSwaggerConfig(ProjectRequest request) {
        try {
            String outputPath = ProjectConfig.getInstance().getOutputPath();
            String packagePath = request.getPackageName().replace(".", File.separator);
            String module = request.generateProjectSrcMain()+"java\\"+packagePath+"\\config";
            String fileName = String.format("%s\\SwaggerConfig.java", module);
            String templateFile = "src/main/resources/templates/rest/SwaggerConfig.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("packageName", request.getPackageName());


            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);
        } catch (Exception e) {
            log.error("ERROR for update Pom.xml file...");
        }
    }


    @Override
    public void addConfigClasses(ProjectRequest request) {

    }

    @Override
    public void generateModelClasses(ProjectRequest request) {
        try {
            String packageName = request.getPackageName() + ".model";
            Path outputDir = Paths.get(request.generateProjectPath(), "src", "main", "java");

            for (ApiRequest api : request.getApiRequests()) {
                // İstek gövdesi için model sınıfı oluşturma
                if (api.getRequestBody() != null && api.getRequestClassName() != null) {
                    createModelClass(api.getRequestClassName(), api.getRequestBody(), packageName, outputDir);
                }
                // Yanıt için model sınıfı oluşturma
                if (api.getResponseBody() != null && api.getResponseClassName() != null) {
                    createModelClass(api.getResponseClassName(), api.getResponseBody(), packageName, outputDir);
                }
            }
        } catch (Exception e) {
            log.error("Model sınıfları oluşturulurken hata oluştu", e);
        }
    }

    private void createModelClass(String className, JsonNode jsonBody, String packageName, Path outputDir) {
        try {
            JCodeModel codeModel = new JCodeModel();
            GenerationConfig config = new DefaultGenerationConfig() {
                @Override
                public boolean isUseLongIntegers() {
                    return true; // long tipini kullan
                }

                @Override
                public SourceType getSourceType() {
                    return SourceType.JSON; // Kaynak tipi JSON
                }

                @Override
                public boolean isIncludeHashcodeAndEquals() {
                    return false;
                }

                @Override
                public boolean isIncludeToString() {
                    return false;
                }

                @Override
                public boolean isUseOptionalForGetters() {
                    return false;
                }

                @Override
                public boolean isIncludeAdditionalProperties() {
                    return false;
                }
            };

            // JSON verisini geçici bir dosyaya yazın
            Path tempDir = Files.createTempDirectory("jsonschema");
            Path jsonSchemaPath = tempDir.resolve("schema.json");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(jsonSchemaPath.toFile(), jsonBody);

            // JSON'dan Java sınıflarını oluşturun
            SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()), new SchemaGenerator());
            mapper.generate(codeModel, className, packageName, jsonSchemaPath.toUri().toURL());

            // Sınıfları hedef dizine yaz
            codeModel.build(outputDir.toFile());

            // Geçici dosyaları temizle
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            log.info("Model sınıfı oluşturuldu: {}", className);

        } catch (IOException e) {
            log.error("Model sınıfı oluşturulurken hata oluştu: " + className, e);
        }
    }

    @Override
    public void generateService(ProjectRequest request) {
        try {
            String packageName = request.getPackageName();
            String className = capitalizeFirstLetter(request.getSystemName()) + "Service";
            String templateFile = "src/main/resources/templates/rest/DynamicService.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", packageName);
            args.put("className", className);
            args.put("apiRequests", request.getApiRequests());
            args.put("authType", request.getAuthType());
            args.put("username", request.getUsername());
            args.put("password", request.getPassword());
            args.put("apiKey", request.getApiKey());
            args.put("systemName", request.getSystemName());

            String module = request.generateProjectSrcMain() + "java\\" + packageName.replace(".", "\\") + "\\service";
            String fileName = module + "\\" + className + ".java";

            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);

            log.info("Service sınıfı oluşturuldu: {}", className);

        } catch (Exception e) {
            log.error("Service sınıfı oluşturulurken hata oluştu", e);
        }
    }



    @Override
    public void generateController(ProjectRequest request) {
        try {
            String packageName = request.getPackageName();
            String className = capitalizeFirstLetter(request.getSystemName()) + "Controller";
            String serviceClassName = capitalizeFirstLetter(request.getSystemName()) + "Service";
            String serviceVarName = decapitalizeFirstLetter(serviceClassName);
            String templateFile = "src/main/resources/templates/rest/DynamicController.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("packageName", packageName);
            args.put("className", className);
            args.put("serviceClassName", serviceClassName);
            args.put("serviceVarName", serviceVarName);
            args.put("apiRequests", request.getApiRequests());

            String module = request.generateProjectSrcMain() + "java\\" + packageName.replace(".", "\\") + "\\controller";
            String fileName = module + "\\" + className + ".java";

            FreeMakeModel freeMakeModel = new FreeMakeModel();
            freeMakeModel.setArgs(args);
            freeMakeModel.setFileName(fileName);
            freeMakeModel.setTemplateFilePath(templateFile);

            freeMakerService.createJavaClassesFromTemplates(freeMakeModel);

            log.info("Controller sınıfı oluşturuldu: {}", className);

        } catch (Exception e) {
            log.error("Controller sınıfı oluşturulurken hata oluştu", e);
        }
    }


    @Override
    public Project getResult() {
        return null;
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String decapitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
