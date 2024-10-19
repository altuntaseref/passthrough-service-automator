package com.yildizholding.ocean.passthroughserviceautomator.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JCodeModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.ApiRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsonschema2pojo.*;
import org.jsonschema2pojo.rules.RuleFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

@Slf4j
@Service
public class ModelGenerator {

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
                    return false; // additionalProperties alanını eklememek için
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
                    .forEach(java.io.File::delete);

            log.info("Model sınıfı oluşturuldu: {}", className);

        } catch (IOException e) {
            log.error("Model sınıfı oluşturulurken hata oluştu: " + className, e);
        }
    }
}
