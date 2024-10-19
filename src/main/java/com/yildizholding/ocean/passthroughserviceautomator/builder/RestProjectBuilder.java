package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JCodeModel;
import com.yildizholding.ocean.passthroughserviceautomator.config.ProjectConfig;
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
            String outputPath = ProjectConfig.getInstance().getOutputPath();
            String packagePath = request.getPackageName().replace(".", File.separator);
            String module = request.generateProjectSrcMain()+"resources";
            String fileName = String.format("%s\\application-dev.properties", module);
            String templateFile = "src/main/resources/templates/rest/applicationProperties.ftl";

            HashMap<String, Object> args = new HashMap<>();
            args.put("projectName", request.getProjectName());
            args.put("baseUrl", request.getEndpoint());
            args.put("username", request.getBasicAuth().getUsername());
            args.put("password", request.getBasicAuth().getPassword());
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
            JsonNode jsonNode = request.getJsonBody();
            if (jsonNode != null && !jsonNode.isNull()) {
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
                };

                request.generatePackageName();

                Path outputDir = Paths.get(request.generateProjectPath(), "src", "main", "java");
                String packageName = request.getPackageName()+ ".model";



// JSON verisini geçici bir dosyaya yazın
                Path tempDir = Files.createTempDirectory("jsonschema");
                Path jsonSchemaPath = tempDir.resolve("schema.json");
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(jsonSchemaPath.toFile(), jsonNode);

                // JSON'dan Java sınıflarını oluşturun
                SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()), new SchemaGenerator());
                mapper.generate(codeModel, "Root", packageName, jsonSchemaPath.toUri().toURL());

                // Sınıfları hedef dizine yaz
                codeModel.build(outputDir.toFile());

                System.out.println("Model sınıfları oluşturuldu.");

                // Geçici dosyaları temizle
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

            } else {
                System.out.println("JSON içeriği boş veya null.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void generateController(ProjectRequest request) {

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
}
