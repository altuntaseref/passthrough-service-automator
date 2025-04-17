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
                // request'i de göndererek doğru yolu hesaplamasını sağla
                writeGeneratedCodeFiles(aiGeneratedCodeMap, request);
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

            // 2. LLM için Prompt'u FreeMakerService ile oluştur (dizinleri de oluşturur)
            log.info("LLM prompt'u FreeMakerService ile oluşturuluyor...");
            String prompt = buildLlmPromptUsingFreemakerService(request, controllerTemplate, serviceTemplate,
                    referenceController, referenceService,
                    postmanCollectionContent);
            if (prompt == null) {
                throw new RuntimeException("LLM prompt'u oluşturulamadı (FreeMakerService null döndü).");
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

            // 4. Yanıtı ayrıştır (YANLIŞ path'ler içeren bir map dönecek)
            log.info("LLM yanıtı JavaCodeExtractor ile ayrıştırılıyor...");
            // Temel dizin olarak PROJE KÖKÜNÜ veriyoruz.
            Map<Path, String> codeMapWithIncorrectPaths = javaCodeExtractor.extractCodeToMap(aiResponse, request.generateProjectPath());

            return Optional.of(codeMapWithIncorrectPaths);

        } catch (Exception e) {
            log.error("LLM ile kod geliştirme aşamasında bir hata oluştu.", e);
            return Optional.empty();
        }
    }

    /**
     * LLM'e gönderilecek prompt metnini FreeMakerService kullanarak oluşturur.
     * Bu metot, prompt'un bir dosyaya yazılması için GEREKLİ DİZİNLERİ OLUŞTURUR.
     * @return LLM için hazır prompt metni veya hata durumunda null.
     */
    private String buildLlmPromptUsingFreemakerService(RestProjectRequest request,
                                                       String controllerTemplateContent,
                                                       String serviceTemplateContent,
                                                       String referenceController,
                                                       String referenceService,
                                                       String postmanCollectionContent) {
        log.debug("LLM prompt'u FreeMakerService ile oluşturuluyor. Şablon: {}", LLM_PROMPT_TEMPLATE_PATH);

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", request.getPackageName());
        dataModel.put("serviceTemplateContent", serviceTemplateContent != null ? serviceTemplateContent : "// Service template could not be read.");
        dataModel.put("controllerTemplateContent", controllerTemplateContent != null ? controllerTemplateContent : "// Controller template could not be read.");
        dataModel.put("referenceControllerContent", referenceController != null ? referenceController : "// Reference controller could not be read.");
        dataModel.put("referenceServiceContent", referenceService != null ? referenceService : "// Reference service could not be read.");
        dataModel.put("postmanCollectionJson", postmanCollectionContent);
        dataModel.put("systemName", request.getSystemName() != null ? request.getSystemName() : "UnknownSystem");
        dataModel.put("baseUrl", request.getBaseUrl() != null ? request.getBaseUrl() : "N/A");

        FreeMakeModel promptModel = new FreeMakeModel();
        promptModel.setArgs(dataModel);
        promptModel.setTemplateFilePath(LLM_PROMPT_TEMPLATE_PATH);

        String promptFileName = GENERATED_PROMPTS_DIR + File.separator + request.getProjectName() + "_llm_prompt_" + System.currentTimeMillis() + ".txt";
        promptModel.setFileName(promptFileName);
        log.info("LLM prompt içeriği şu dosyaya da yazılacak (FreeMakerService tarafından): {}", promptFileName);

        // Dizin Oluşturma Adımı
        try {
            Path targetPath = Paths.get(promptFileName);
            Path parentDir = targetPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
                log.debug("Prompt hedef dizini kontrol edildi/oluşturuldu: {}", parentDir);
            }
        } catch (IOException e) {
            log.error("Prompt hedef dizini ({}) oluşturulurken hata oluştu: {}", GENERATED_PROMPTS_DIR, e.getMessage());
            return null; // Dizin oluşturulamazsa devam etme
        }

        // FreeMakerService'i çağır
        StringWriter resultWriter = freeMakerService.createJavaClassesFromTemplates(promptModel);

        if (resultWriter != null) {
            String generatedPrompt = resultWriter.toString();
            log.debug("Oluşturulan LLM Prompt (ilk 1000 karakter):\n{}", generatedPrompt.substring(0, Math.min(generatedPrompt.length(), 1000)));
            return generatedPrompt;
        } else {
            log.error("FreeMakerService'ten prompt oluşturulurken null StringWriter döndü.");
            return null;
        }
    }

    /**
     * Verilen kod haritasındaki dosyaları DOĞRU projenin kaynak/test dizinlerine yazar.
     *
     * @param codeMap Anahtar olarak extractor'ın HESAPLADIĞI (yanlış) yol,
     *                değer olarak kod içeriği (String) içeren harita.
     * @param request Proje isteği (src/main/java ve src/test/java yollarını almak için).
     */
    private void writeGeneratedCodeFiles(Map<Path, String> codeMap, RestProjectRequest request) {
        if (request == null) {
            log.error("writeGeneratedCodeFiles çağrıldı ancak RestProjectRequest null.");
            return;
        }
        String mainJavaPath = request.generateProjectSrcMain();
        String testJavaPath = request.generateProjectSrcTest();

        if (mainJavaPath == null || testJavaPath == null) {
            log.error("Proje ana veya test kaynak yolları null.");
            return;
        }
        Path mainSrcJavaBase = Paths.get(mainJavaPath, "java");
        Path testSrcJavaBase = Paths.get(testJavaPath, "java");

        log.info("{} adet ayrıştırılmış kod dosyası doğru konumlara yazılacak...", codeMap.size());
        int successCount = 0;
        int errorCount = 0;

        for (Map.Entry<Path, String> entry : codeMap.entrySet()) {
            String codeContent = entry.getValue();
            Path targetPath = null; // Döngü içinde hesaplanacak

            try {
                // 1. Kod içeriğinden paket ve sınıf adını çıkar
                String packageName = javaCodeExtractor.extractPackageName(codeContent);
                String className = javaCodeExtractor.extractClassName(codeContent);

                if (packageName == null || className == null) {
                    log.warn("Kod bloğundan paket veya sınıf adı çıkarılamadı, yazma işlemi atlanıyor:\n{}", codeContent.substring(0, Math.min(100, codeContent.length())));
                    errorCount++;
                    continue;
                }

                // 2. Test sınıfı mı kontrol et
                boolean isTestClass = className.endsWith("Test");

                // 3. Doğru temel yolu seç
                Path correctBaseDir = isTestClass ? testSrcJavaBase : mainSrcJavaBase;

                // 4. Doğru tam hedef yolu oluştur
                Path packageAsSubPath = Paths.get(packageName.replace('.', File.separatorChar));
                targetPath = correctBaseDir.resolve(packageAsSubPath).resolve(className + ".java").normalize();
                log.debug("Hesaplanan doğru hedef yol: {}", targetPath);

                // 5. Gerekli dizinleri oluştur
                Path parentDir = targetPath.getParent();
                if(parentDir != null) Files.createDirectories(parentDir);

                // 6. Dosyayı yaz
                Files.writeString(targetPath, codeContent, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("Dosya başarıyla yazıldı/güncellendi: {}", targetPath);
                successCount++;

            } catch (IOException e) {
                log.error("Dosya yazılamadı ({}) : {}", targetPath, e.getMessage());
                errorCount++;
            } catch (InvalidPathException e) {
                log.error("Geçersiz dosya yolu hesaplandı. Hata: {}", e.getMessage());
                errorCount++;
            } catch (Exception e) {
                log.error("Dosya yazılırken beklenmedik hata ({}) : {}", targetPath, e.getMessage(), e);
                errorCount++;
            }
        }
        log.info("Dosya yazma işlemi tamamlandı. Başarılı: {}, Hatalı: {}", successCount, errorCount);
    }


    /**
     * Belirtilen tipteki (controller/service) şablon dosyasının içeriğini okur.
     */
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
            // generateProjectSrcMain() zaten .../src/main/ döndürüyor, /java ekleyelim
            filePath = Paths.get( mainSrcPath, "java",
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

    /**
     * Classpath'teki bir resource dosyasının içeriğini okur.
     */
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