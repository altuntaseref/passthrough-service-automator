//package com.yildizholding.ocean.passthroughserviceautomator.mapper;
//
//import com.yildizholding.ocean.passthroughserviceautomator.model.Project;
//import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
//import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilderImpl;
//import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
//import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
//// import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceResponse; // Gerekmiyorsa kaldırılabilir
//import com.yildizholding.ocean.passthroughserviceautomator.util.FileUtils; // FileUtils import edildi varsayılıyor
//import com.yildizholding.ocean.passthroughserviceautomator.util.JavaCodeExtractor; // Yeni extractor
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.util.Collections;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ProjectServie {
//
//    private final ProjectInitializerService projectInitializerService; // Kullanılmıyorsa kaldırılabilir? Belki Builder içindedir.
//    private final RestProjectBuilderImpl restProjectBuilderImpl;
//    private final ProjectDirector projectDirector;
//    private final LlmService llmService; // Yeni LLM servisi inject edildi
//    private final JavaCodeExtractor javaCodeExtractor; // Yeni extractor inject edildi
//
//    public ProjectResponse generateProject(RestProjectRequest request) {
//        ProjectResponse response = new ProjectResponse();
//        String projectPath = request.generateProjectPath(); // Proje yolunu başta alalım
//        response.setProjectPath(projectPath); // Yanıta ekleyelim
//
//        try {
//            // --- AŞAMA 1: Proje İskeletini ve Boş Sınıfları Oluşturma ---
//            log.info("Aşama 1: Proje iskeleti ve şablon tabanlı sınıflar oluşturuluyor...");
//            projectDirector.constructProject(restProjectBuilderImpl, request);
//            Project initialProject = restProjectBuilderImpl.getResult(); // Bu belki kullanılmayabilir
//            log.info("Aşama 1 tamamlandı.");
//
//            // --- AŞAMA 2: LLM ile Kod Doldurma ve Dosyaları Güncelleme ---
//            log.info("Aşama 2: LLM ile kodlar doldurulacak ve dosyalar güncellenecek...");
//
//            // 2.1 Oluşturulan boş Controller ve Service dosyalarının içeriğini oku
//            String controllerContent = readGeneratedFileContent(request, "controller");
//            String serviceContent = readGeneratedFileContent(request, "service");
//
//            if (controllerContent == null || serviceContent == null) {
//                throw new IOException("Başlangıç Controller veya Service dosyası okunamadı.");
//            }
//
//            // 2.2 LLM için Prompt'u Hazırla (İhtiyaca göre detaylandırın)
//            String prompt = buildLlmPrompt(request, controllerContent, serviceContent);
//
//            // 2.3 LLM'den tamamlanmış kodu al
//            String aiResponse = llmService.getCompletion(prompt);
//
//            // 2.4 AI Yanıtını Ayrıştır
//            // Base directory olarak proje kök dizinini verelim, extractor paketlerden yolu tamamlasın
//            Map<Path, String> aiGeneratedCodeMap = javaCodeExtractor.extractCodeToMap(aiResponse, projectPath);
//
//            if (aiGeneratedCodeMap.isEmpty()) {
//                 log.warn("LLM yanıtından ayrıştırılacak geçerli Java kodu bulunamadı!");
//                 // Hata fırlatabilir veya sadece uyarı verip devam edebiliriz.
//                 // Şimdilik devam edelim, şablon kodları kalır.
//            } else {
//                log.info("LLM yanıtından {} adet kod bloğu ayrıştırıldı. Dosyalar yazılıyor...", aiGeneratedCodeMap.size());
//                // 2.5 Ayrıştırılan kodları ilgili dosyalara yaz (üzerine yazma veya oluşturma)
//                for (Map.Entry<Path, String> entry : aiGeneratedCodeMap.entrySet()) {
//                    Path targetPath = entry.getKey(); // Extractor mutlak yolu hesaplamış olmalı
//                    String codeContent = entry.getValue();
//
//                    try {
//                        // Gerekli dizinleri oluştur (varsa dokunmaz)
//                        Files.createDirectories(targetPath.getParent());
//                        // Dosyayı yaz (varsa üzerine yazar)
//                        Files.writeString(targetPath, codeContent, StandardCharsets.UTF_8,
//                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//                        log.info("Dosya başarıyla yazıldı/güncellendi: {}", targetPath);
//                    } catch (IOException e) {
//                        log.error("AI tarafından oluşturulan dosya yazılamadı: {}", targetPath, e);
//                        // Hata durumunda ne yapılacağına karar verin (işleme devam et, hata fırlat vb.)
//                    }
//                }
//                log.info("Aşama 2 tamamlandı: LLM kodları dosyalara yazıldı.");
//            }
//             response.setMessage("Proje başarıyla oluşturuldu ve LLM kodları entegre edildi.");
//
//
//        } catch (Exception e) {
//            log.error("Proje oluşturma sırasında bir hata oluştu", e);
//            response.setMessage("Proje oluşturulurken hata oluştu: " + e.getMessage());
//            // Hata durumunda proje dizinini temizlemek isteyebilirsiniz.
//        }
//        return response;
//    }
//
//    /**
//     * Aşama 1'de oluşturulan Controller veya Service dosyasının içeriğini okur.
//     */
//    private String readGeneratedFileContent(RestProjectRequest request, String type) {
//        try {
//            String className = FileUtils.capitalizeFirstLetter(request.getSystemName()) + (type.equals("controller") ? "Controller" : "Service");
//            String subPackage = type; // "controller" veya "service"
//            String basePackagePath = request.getPackageName().replace('.', File.separatorChar);
//            Path filePath = Paths.get(request.generateProjectSrcMain(), "java", basePackagePath, subPackage, className + ".java");
//
//            if (Files.exists(filePath)) {
//                log.info("Okunuyor: {}", filePath);
//                return Files.readString(filePath, StandardCharsets.UTF_8);
//            } else {
//                log.error("Dosya bulunamadı: {}", filePath);
//                return null;
//            }
//        } catch (IOException e) {
//            log.error("{} dosyası okunurken hata oluştu", type, e);
//            return null;
//        }
//    }
//
//    /**
//     * LLM'e gönderilecek prompt'u oluşturur.
//     * Bu metodu ihtiyaçlarınıza göre detaylandırın.
//     */
//    private String buildLlmPrompt(RestProjectRequest request, String controllerTemplate, String serviceTemplate) {
//        // Bu prompt'u LLM'inizin beklentilerine ve istediğiniz detay seviyesine göre ayarlayın.
//        StringBuilder promptBuilder = new StringBuilder();
//        promptBuilder.append("Aşağıdaki Spring Boot projesi için Controller ve Service sınıflarını ve JUnit 5 testlerini oluşturun.\n");
//        promptBuilder.append("Proje Adı: ").append(request.getProjectName()).append("\n");
//        promptBuilder.append("Ana Paket: ").append(request.getPackageName()).append("\n");
//        promptBuilder.append("Sistem Adı (Endpoint/URL için kullanılabilir): ").append(request.getSystemName()).append("\n");
//        // Varsa API bilgileri eklenebilir: request.getBaseUrl(), request.getUsername() vs.
//
//        promptBuilder.append("\n--- Mevcut Controller Şablonu ---\n");
//        promptBuilder.append(controllerTemplate);
//        promptBuilder.append("\n-------------------------------\n");
//
//        promptBuilder.append("\n--- Mevcut Service Şablonu ---\n");
//        promptBuilder.append(serviceTemplate);
//        promptBuilder.append("\n-----------------------------\n");
//
//        promptBuilder.append("\nİstenenler:\n");
//        promptBuilder.append("1. Controller sınıfını tamamla. Gelen isteği Service sınıfına yönlendirsin.\n");
//        promptBuilder.append("2. Service sınıfını tamamla. Gerekli iş mantığını (örneğin RestTemplate ile dış API çağrısı, loglama vb.) içersin.\n");
//        promptBuilder.append("   - Gerekirse Service için bir Config sınıfı (örn: ").append(FileUtils.capitalizeFirstLetter(request.getSystemName())).append("ServiceConfig) kullanabilirsin.\n");
//        promptBuilder.append("3. Controller için JUnit 5 test sınıfı oluştur.\n");
//        promptBuilder.append("4. Service için JUnit 5 test sınıfı oluştur.\n");
//        promptBuilder.append("5. Tüm kodlar belirtilen ana paket (`").append(request.getPackageName()).append("`) altında uygun alt paketlerde (controller, service) olmalı.\n");
//        promptBuilder.append("6. Test kodları `src/test/java` altında aynı paket yapısında olmalı.\n");
//        promptBuilder.append("7. Kodlar Java 17 uyumlu ve modern Spring Boot pratiklerine uygun olsun (Lombok, Dependency Injection vb.).\n");
//
//        return promptBuilder.toString();
//    }
//}