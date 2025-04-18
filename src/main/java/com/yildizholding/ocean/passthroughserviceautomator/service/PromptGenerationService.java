package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.FreeMakeModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptGenerationService {

    private final FreeMakerService freeMakerService; // Değiştirilmeyen servis
    private final ResourceReaderService resourceReaderService; // Yeni okuyucu servis

    // Sabitler
    private static final String LLM_PROMPT_TEMPLATE_PATH = "src/main/resources/templates/prompts/llm_code_generation_prompt.ftl";
    private static final String REFERENCE_CONTROLLER_PATH = "templates/reference/ReferenceController.txt";
    private static final String REFERENCE_SERVICE_PATH = "templates/reference/ReferenceService.txt";
    private static final String GENERATED_PROMPTS_DIR = "target/generated-prompts";

    /**
     * LLM'e gönderilecek prompt metnini FreeMakerService kullanarak oluşturur.
     * @param request Proje isteği.
     * @param controllerTemplateContent Boş controller içeriği.
     * @param serviceTemplateContent Boş service içeriği.
     * @param postmanCollectionContent Okunan Postman Collection içeriği.
     * @return LLM için hazır prompt metni.
     * @throws IOException Dosya okuma veya dizin oluşturma hatası.
     * @throws RuntimeException Prompt oluşturma sırasında kritik hata olursa.
     */
    public String buildLlmPrompt(RestProjectRequest request,
                                 String controllerTemplateContent,
                                 String serviceTemplateContent,
                                 String postmanCollectionContent) throws IOException {
        log.debug("LLM prompt'u oluşturuluyor. Şablon: {}", LLM_PROMPT_TEMPLATE_PATH);

        // Referans dosyaları oku
        String referenceController = resourceReaderService.readClasspathResourceFileContent(REFERENCE_CONTROLLER_PATH);
        String referenceService = resourceReaderService.readClasspathResourceFileContent(REFERENCE_SERVICE_PATH);

        // Veri modelini oluştur
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", request.getPackageName());
        dataModel.put("serviceTemplateContent", serviceTemplateContent);
        dataModel.put("controllerTemplateContent", controllerTemplateContent);
        dataModel.put("referenceControllerContent", referenceController);
        dataModel.put("referenceServiceContent", referenceService);
        dataModel.put("postmanCollectionJson", postmanCollectionContent);
        dataModel.put("systemName", request.getSystemName() != null ? request.getSystemName() : "UnknownSystem");
        dataModel.put("baseUrl", request.getBaseUrl() != null ? request.getBaseUrl() : "N/A");

        // FreeMakeModel'i hazırla
        FreeMakeModel promptModel = new FreeMakeModel();
        promptModel.setArgs(dataModel);
        promptModel.setTemplateFilePath(LLM_PROMPT_TEMPLATE_PATH);

        // Hedef prompt dosya adını ve dizinini belirle
        String promptFileName = GENERATED_PROMPTS_DIR + File.separator + request.getProjectName() + "_llm_prompt_" + System.currentTimeMillis() + ".txt";
        promptModel.setFileName(promptFileName);
        log.info("LLM prompt içeriği şu dosyaya da yazılacak (FreeMakerService tarafından): {}", promptFileName);

        // Gerekli dizini oluştur
        ensureDirectoryExists(promptFileName);

        // FreeMakerService'i çağır
        StringWriter resultWriter = freeMakerService.createJavaClassesFromTemplates(promptModel);

        if (resultWriter != null) {
            String generatedPrompt = resultWriter.toString();
            log.debug("Oluşturulan LLM Prompt (ilk 1000 karakter):\n{}", generatedPrompt.substring(0, Math.min(generatedPrompt.length(), 1000)));
            return generatedPrompt;
        } else {
            log.error("FreeMakerService'ten prompt oluşturulurken null StringWriter döndü.");
            throw new RuntimeException("LLM prompt'u oluşturulamadı.");
        }
    }

    private void ensureDirectoryExists(String filePath) throws IOException {
        Path targetPath = Paths.get(filePath);
        Path parentDir = targetPath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
            log.debug("Prompt hedef dizini kontrol edildi/oluşturuldu: {}", parentDir);
        } else {
            log.warn("Prompt dosyası için üst dizin belirlenemedi: {}", filePath);
            // Belki hata fırlatmak daha iyi olabilir?
        }
    }
}
