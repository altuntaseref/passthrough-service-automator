package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.util.JavaCodeExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeEnhancementService {

    private final PromptGenerationService promptGenerationService;
    private final LlmService llmService;
    private final JavaCodeExtractor javaCodeExtractor;
    private final ResourceReaderService resourceReaderService;

    /**
     * LLM kullanarak kodu geliştirir ve ayrıştırılmış kod haritasını döndürür.
     */
    public Optional<Map<Path, String>> enhanceCode(RestProjectRequest request, String postmanCollectionContent) {
        try {
            log.info("Kod geliştirme süreci başlatılıyor...");

            // 1. Gerekli şablonları oku
            String controllerTemplate = resourceReaderService.readGeneratedTemplateContent(request, "controller")
                    .orElseThrow(() -> new IOException("Başlangıç Controller şablonu okunamadı."));
            String serviceTemplate = resourceReaderService.readGeneratedTemplateContent(request, "service")
                    .orElseThrow(() -> new IOException("Başlangıç Service şablonu okunamadı."));

            // 2. Prompt'u oluştur
            String prompt = promptGenerationService.buildLlmPrompt(request, controllerTemplate, serviceTemplate, postmanCollectionContent);

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
            Map<Path, String> codeMap = javaCodeExtractor.extractCodeToMap(aiResponse, request.generateProjectPath()); // Base path proje kökü

            return Optional.of(codeMap);

        } catch (Exception e) {
            log.error("LLM ile kod geliştirme aşamasında bir hata oluştu.", e);
            return Optional.empty();
        }
    }
}
