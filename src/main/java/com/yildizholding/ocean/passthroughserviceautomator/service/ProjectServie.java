package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilderImpl;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServie { // Sınıf adını ProjectService olarak değiştirmek daha iyi olabilir

    // Ana Orkestrasyon Bağımlılıkları
    private final RestProjectBuilderImpl restProjectBuilderImpl; // Veya Interface'i: RestProjectBuilder
    private final ProjectDirector projectDirector;
    private final CodeEnhancementService codeEnhancementService;
    private final GeneratedCodeWriterService codeWriterService;

    /**
     * Yeni bir REST projesi oluşturur.
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

        try {
            // --- Postman Dosyasını Oku ---
            if (postmanFile == null || postmanFile.isEmpty()) {
                throw new IllegalArgumentException("Postman Collection dosyası yüklenmedi veya boş.");
            }
            log.info("Yüklenen Postman dosyası okunuyor: {}", postmanFile.getOriginalFilename());
            String postmanCollectionContent = new String(postmanFile.getBytes(), StandardCharsets.UTF_8);
            log.info("Postman dosyası başarıyla okundu.");

            // --- Aşama 1: Proje İskeletini Oluştur ---
            log.info("Aşama 1: Proje iskeleti ve şablon tabanlı sınıflar oluşturuluyor...");
            projectDirector.constructProject(restProjectBuilderImpl, request);
            log.info("Aşama 1 tamamlandı. Proje iskeleti: {}", projectPath);

            // --- Aşama 2: LLM ile Kodu Geliştir ve Yaz ---
            log.info("Aşama 2: LLM ile kod geliştirme ve yazma başlatılıyor...");
            Optional<Map<Path, String>> aiGeneratedCodeMapOpt = codeEnhancementService.enhanceCode(request, postmanCollectionContent);

            if (aiGeneratedCodeMapOpt.isPresent() && !aiGeneratedCodeMapOpt.get().isEmpty()) {
                codeWriterService.writeCodeFiles(aiGeneratedCodeMapOpt.get(), request);
                response.setMessage("Proje başarıyla oluşturuldu ve LLM kodları entegre edildi.");
                log.info("Aşama 2 tamamlandı.");
            } else {
                response.setMessage("Proje iskeleti oluşturuldu, ancak LLM kodları alınamadı/boş. Şablonlar kullanılıyor.");
                log.warn("Aşama 2 tamamlanamadı veya LLM yanıtı boş.");
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
}