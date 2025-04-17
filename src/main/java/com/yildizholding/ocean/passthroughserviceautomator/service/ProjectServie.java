package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilderImpl;
// TemplateGenerator'a artık doğrudan ihtiyacımız yok, FreeMakerService'i kullanacağız
// import com.yildizholding.ocean.passthroughserviceautomator.generator.TemplateGenerator;
import com.yildizholding.ocean.passthroughserviceautomator.model.FreeMakeModel; // Eklendi
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.util.FileUtils;
import com.yildizholding.ocean.passthroughserviceautomator.util.JavaCodeExtractor;
// FreeMarker Exception'larına doğrudan ihtiyacımız yok, FreeMakerService yönetecek
// import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie {

    // Bağımlılıklar
    private final RestProjectBuilderImpl restProjectBuilderImpl;
    private final ProjectDirector projectDirector;
    private final LlmService llmService;
    private final JavaCodeExtractor javaCodeExtractor;
    private final FreeMakerService freeMakerService; // TemplateGenerator yerine doğrudan FreeMakerService

    // Sabitler
    private static final String CONTROLLER_TYPE = "controller";
    private static final String SERVICE_TYPE = "service";
    private static final String LLM_PROMPT_TEMPLATE_PATH = "src/main/resources/templates/prompts/llm_code_generation_prompt.ftl";
    private static final String REFERENCE_CONTROLLER_PATH = "templates/reference/ReferenceController.txt";
    private static final String REFERENCE_SERVICE_PATH = "templates/reference/ReferenceService.txt";
    private static final String GENERATED_PROMPTS_DIR = "target/generated-prompts"; // Prompt dosyalarının yazılacağı yer (opsiyonel)

    /**
     * Yeni bir REST projesi oluşturur.
     *
     * @param request Proje oluşturma isteği detayları (JSON).
     * @param postmanFile Yüklenen Postman Collection dosyası.
     * @return Proje oluşturma sonucunu içeren yanıt nesnesi.
     */
    public ProjectResponse generateProject(RestProjectRequest request, MultipartFile postmanFile) {
        ProjectResponse response = new ProjectResponse();
        String projectPath = request.generateProjectPath();
        if (projectPath == null) {
            response.setMessage("Proje yolu oluşturulamadı.");
            log.error("Proje yolu null döndü.");
            return response;
        }
        response.setProjectPath(projectPath);

        String postmanCollectionContent = null;
        try {
            // --- Postman Dosyasını Oku ---
            if (postmanFile == null || postmanFile.isEmpty()) {
                throw new IllegalArgumentException("Postman Collection dosyası yüklenmedi veya boş.");
            }
            log.info("Yüklenen Postman dosyası okunuyor: {}", postmanFile.getOriginalFilename());
            postmanCollectionContent = new String(postmanFile.getBytes(), StandardCharsets.UTF_8);
            log.info("Postman dosyası başarıyla okundu.");

            // --- Aşama 1: Proje İskeletini Oluştur ---
            log.info("Aşama 1: Proje iskeleti ve şablon tabanlı sınıflar oluşturuluyor...");
            projectDirector.constructProject(restProjectBuilderImpl, request);
            log.info("Aşama 1 tamamlandı. Proje iskeleti: {}", projectPath);

            // --- Aşama 2: LLM ile Kodu Geliştir ---
            log.info("Aşama 2: LLM ile kod geliştirme başlatılıyor...");
            Optional<Map<Path, String>> aiGeneratedCodeMapOpt = enhanceCodeWithLlm(request, postmanCollectionContent);

            if (aiGeneratedCodeMapOpt.isPresent() && !aiGeneratedCodeMapOpt.get().isEmpty()) {
                Map<Path, String> aiGeneratedCodeMap = aiGeneratedCodeMapOpt.get();
                log.info("LLM'den {} adet kod bloğu başarıyla alındı. Dosyalar yazılıyor...", aiGeneratedCodeMap.size());
                writeGeneratedCodeFiles(aiGeneratedCodeMap);
                response.setMessage("Proje başarıyla oluşturuldu ve LLM kodları entegre edildi.");
                log.info("Aşama 2 tamamlandı: LLM kodları dosyalara yazıldı.");
            } else {
                response.setMessage("Proje iskeleti başarıyla oluşturuldu, ancak LLM kodları alınamadı veya boş döndü. Şablon kodları kullanılıyor.");
                log.warn("Aşama 2 tamamlanamadı veya LLM yanıtı boş. Şablon kodları geçerli olacak.");
            }

        } catch (IllegalArgumentException e) {
            log.error("Geçersiz istek: {}", e.getMessage());
            response.setMessage("İstek hatası: " + e.getMessage());
        } catch (IOException e) {
            log.error("Dosya işlemi sırasında hata oluştu!", e);
            response.setMessage("Dosya işlemi hatası: " + e.getMessage());
        } catch (Exception e) {
            log.error("Proje oluşturma sırasında kritik bir hata oluştu!", e);
            response.setMessage("Proje oluşturulurken hata oluştu: " + e.getMessage());
        }
        return response;
    }

    // --- Özel (Private) Yardımcı Metotlar ---

    /**
     * LLM kullanarak kodu geliştirir. Mevcut FreeMakerService'i kullanarak prompt oluşturur.
     * @param request Proje isteği.
     * @param postmanCollectionContent Okunan Postman Collection içeriği.
     * @return Kod haritasını içeren Optional.
     */
    private Optional<Map<Path, String>> enhanceCodeWithLlm(RestProjectRequest request, String postmanCollectionContent) {
        try {
            // 1. Gerekli içerikleri topla
            log.info("LLM süreci için gerekli dosyalar okunuyor...");
            String controllerTemplate = readGeneratedFileContent(request, CONTROLLER_TYPE)
                    .orElseThrow(() -> new IOException("Başlangıç Controller şablonu okunamadı."));
            String serviceTemplate = readGeneratedFileContent(request, SERVICE_TYPE)
                    .orElseThrow(() -> new IOException("Başlangıç Service şablonu okunamadı."));
            String referenceController = readResourceFileContent(REFERENCE_CONTROLLER_PATH);
            String referenceService = readResourceFileContent(REFERENCE_SERVICE_PATH);
            log.info("Gerekli dosyalar başarıyla okundu.");

            // 2. LLM için Prompt'u FreeMakerService ile oluştur
            log.info("LLM prompt'u FreeMakerService ile oluşturuluyor...");
            String prompt = buildLlmPromptUsingFreemakerService(request, controllerTemplate, serviceTemplate,
                    referenceController, referenceService,
                    postmanCollectionContent);
            if (prompt == null) {
                // buildLlmPromptUsingFreemakerService içinde hata loglanmış olmalı
                throw new RuntimeException("LLM prompt'u oluşturulamadı.");
            }
            log.info("LLM prompt'u başarıyla oluşturuldu.");

            // 3. LLM'i çağır
            log.info("LLM servisine istek gönderiliyor...");
            String aiResponse = llmService.getCompletion(prompt);
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("LLM'den boş veya null yanıt alındı.");
                return Optional.empty();
            }
            log.info("LLM'den yanıt başarıyla alındı.");

            // 4. Yanıtı ayrıştır
            log.info("LLM yanıtı JavaCodeExtractor ile ayrıştırılıyor...");
            // Proje kök dizinini ver, extractor tam yolu hesaplasın
            Map<Path, String> codeMap = javaCodeExtractor.extractCodeToMap(aiResponse, request.generateProjectPath());

            return Optional.of(codeMap);

        } catch (Exception e) {
            log.error("LLM ile kod geliştirme aşamasında bir hata oluştu.", e);
            return Optional.empty();
        }
    }

    /**
     * LLM'e gönderilecek prompt metnini FreeMakerService kullanarak oluşturur.
     * Bu metot, prompt'un bir dosyaya yazılması için GEREKLİ DİZİNLERİ OLUŞTURUR.
     * @param request Proje isteği.
     * @param controllerTemplateContent Boş controller içeriği.
     * @param serviceTemplateContent Boş service içeriği.
     * @param referenceController Referans controller içeriği.
     * @param referenceService Referans service içeriği.
     * @param postmanCollectionContent Okunan Postman Collection içeriği.
     * @return LLM için hazır prompt metni veya hata durumunda null.
     */
    private String buildLlmPromptUsingFreemakerService(RestProjectRequest request,
                                                       String controllerTemplateContent,
                                                       String serviceTemplateContent,
                                                       String referenceController,
                                                       String referenceService,
                                                       String postmanCollectionContent) {
        log.debug("LLM prompt'u FreeMakerService ile oluşturuluyor. Şablon: {}", LLM_PROMPT_TEMPLATE_PATH);

        // Şablon için veri modeli oluştur
        Map<String, Object> dataModel = new HashMap<>();
        // ... (dataModel'e verileri ekleme kısmı aynı) ...
        dataModel.put("packageName", request.getPackageName());
        dataModel.put("serviceTemplateContent", serviceTemplateContent != null ? serviceTemplateContent : "// Service template content could not be read.");
        dataModel.put("controllerTemplateContent", controllerTemplateContent != null ? controllerTemplateContent : "// Controller template content could not be read.");
        dataModel.put("referenceControllerContent", referenceController != null ? referenceController : "// Reference controller content could not be read.");
        dataModel.put("referenceServiceContent", referenceService != null ? referenceService : "// Reference service content could not be read.");
        dataModel.put("postmanCollectionJson", postmanCollectionContent);
        dataModel.put("systemName", request.getSystemName() != null ? request.getSystemName() : "UnknownSystem");
        dataModel.put("baseUrl", request.getBaseUrl() != null ? request.getBaseUrl() : "N/A");


        // FreeMakeModel'i hazırla
        FreeMakeModel promptModel = new FreeMakeModel();
        promptModel.setArgs(dataModel);
        promptModel.setTemplateFilePath(LLM_PROMPT_TEMPLATE_PATH);

        // Prompt dosyasının yazılacağı yeri belirle
        String promptFileName = GENERATED_PROMPTS_DIR + File.separator + request.getProjectName() + "_llm_prompt_" + System.currentTimeMillis() + ".txt";
        promptModel.setFileName(promptFileName);
        log.info("LLM prompt içeriği şu dosyaya da yazılacak (FreeMakerService tarafından): {}", promptFileName);

        // --- Dizin Oluşturma Adımı (BURADA YAPILIYOR) ---
        try {
            Path targetPath = Paths.get(promptFileName);
            Path parentDir = targetPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir); // Gerekli dizinleri oluştur
                log.debug("Prompt hedef dizini kontrol edildi/oluşturuldu: {}", parentDir);
            }
        } catch (IOException e) {
            log.error("Prompt hedef dizini ({}) oluşturulurken hata oluştu: {}", GENERATED_PROMPTS_DIR, e.getMessage());
            // Dizin oluşturulamazsa FreeMakerService hata verecektir, yine de null dönelim
            return null;
        }
        // --- Dizin Oluşturma Adımı Sonu ---


        // FreeMakerService'i çağır (Artık dizin var olduğu için hata vermemeli)
        StringWriter resultWriter = freeMakerService.createJavaClassesFromTemplates(promptModel);

        // Sonucu kontrol et ve string'i al
        if (resultWriter != null) {
            String generatedPrompt = resultWriter.toString();
            log.debug("Oluşturulan LLM Prompt (ilk 1000 karakter):\n{}", generatedPrompt.substring(0, Math.min(generatedPrompt.length(), 1000)));
            return generatedPrompt;
        } else {
            log.error("FreeMakerService'ten prompt oluşturulurken null StringWriter döndü. Şablon: {}, Hedef Dosya: {}", LLM_PROMPT_TEMPLATE_PATH, promptFileName);
            return null;
        }
    }


    // writeGeneratedCodeFiles metodu aynı kalır...
    private void writeGeneratedCodeFiles(Map<Path, String> codeMap) {
        log.info("{} adet ayrıştırılmış kod dosyası diske yazılacak...", codeMap.size());
        int successCount = 0;
        int errorCount = 0;
        for (Map.Entry<Path, String> entry : codeMap.entrySet()) {
            Path targetPath = entry.getKey();
            String codeContent = entry.getValue();
            try {
                Files.createDirectories(targetPath.getParent());
                Files.writeString(targetPath, codeContent, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.debug("Dosya başarıyla yazıldı/güncellendi: {}", targetPath);
                successCount++;
            } catch (IOException e) {
                log.error("AI tarafından oluşturulan dosya yazılamadı: {}", targetPath, e);
                errorCount++;
            } catch (InvalidPathException e) {
                log.error("Geçersiz dosya yolu hesaplandı veya alındı: {}. Hata: {}", targetPath, e.getMessage());
                errorCount++;
            }
        }
        log.info("Dosya yazma işlemi tamamlandı. Başarılı: {}, Hatalı: {}", successCount, errorCount);
    }


    // readGeneratedFileContent metodu aynı kalır...
    private Optional<String> readGeneratedFileContent(RestProjectRequest request, String type) {
        String className = null;
        Path filePath = null;
        try {
            String systemNameCapitalized = FileUtils.capitalizeFirstLetter(request.getSystemName());
            className = systemNameCapitalized + (type.equals(CONTROLLER_TYPE) ? "Controller" : "Service");
            String subPackage = type;
            String mainSrcPath = request.generateProjectSrcMain();
            if (mainSrcPath == null) {
                log.error("'generateProjectSrcMain()' null döndü.");
                return Optional.empty();
            }
            filePath = Paths.get(
                    mainSrcPath, "java",
                    request.getPackageName().replace('.', File.separatorChar),
                    subPackage, className + ".java"
            );
            if (Files.exists(filePath)) {
                log.debug("Okunuyor: {}", filePath);
                return Optional.of(Files.readString(filePath, StandardCharsets.UTF_8));
            } else {
                log.error("Şablon dosyası bulunamadı: {}", filePath);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("{} şablon dosyası ({}) okunurken hata oluştu", type, filePath, e);
            return Optional.empty();
        }
    }

    // readResourceFileContent metodu aynı kalır...
    private String readResourceFileContent(String resourcePath) throws IOException {
        log.debug("Resource dosyası okunuyor: classpath:{}", resourcePath);
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.error("Resource dosyası bulunamadı: classpath:{}", resourcePath);
            throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
        }
        try (InputStream inputStream = resource.getInputStream();
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            log.error("Resource dosyası okunurken hata: classpath:{}", resourcePath, e);
            throw e;
        }
    }
}