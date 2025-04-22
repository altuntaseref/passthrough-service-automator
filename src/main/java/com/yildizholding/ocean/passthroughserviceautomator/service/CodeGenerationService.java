package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.builder.ProjectDirector;
import com.yildizholding.ocean.passthroughserviceautomator.builder.RestProjectBuilderImpl;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.results.CodeGenerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeGenerationService {

    private final RestProjectBuilderImpl restProjectBuilder;
    private final ProjectDirector projectDirector;
    private final ResourceReaderService resourceReaderService;
    private final PromptGenerationService promptGenerationService;
    private final CodeEnhancementService codeEnhancementService; // LLM çağrısı için
    private final GeneratedCodeWriterService codeWriterService; // Dosya yazma için

    /**
     * Proje iskeletini oluşturur, LLM ile kodu geliştirir ve yazar.
     */
    public CodeGenerationResult generateCode(RestProjectRequest request, String postmanContent) {
        CodeGenerationResult.CodeGenerationResultBuilder resultBuilder = CodeGenerationResult.builder();
        try {
            // 1. İskelet Oluştur
            projectDirector.constructProject(restProjectBuilder, request);
            resultBuilder.skeletonSuccess(true);
            log.info("Proje iskeleti oluşturuldu.");

            // 2. LLM ile Geliştirme
            resultBuilder.llmAttempted(true);
            try {
                String controllerTemplate = resourceReaderService.readGeneratedTemplateContent(request, "controller").orElseThrow(() -> new IOException("Controller şablonu okunamadı."));
                String serviceTemplate = resourceReaderService.readGeneratedTemplateContent(request, "service").orElseThrow(() -> new IOException("Service şablonu okunamadı."));
                String prompt = promptGenerationService.buildLlmPrompt(request, controllerTemplate, serviceTemplate, postmanContent);

                Optional<Map<Path, String>> codeMapOpt = codeEnhancementService.enhanceCode(request, prompt); // Sadece LLM çağrısı ve parse

                if (codeMapOpt.isPresent() && !codeMapOpt.get().isEmpty()) {
                    // Kodu yaz
                    codeWriterService.writeCodeFiles(codeMapOpt.get(), request);
                    resultBuilder.llmSuccess(true).generatedCodeMap(codeMapOpt.get()); // Kodu sonuca ekle (opsiyonel)
                    log.info("LLM kodları başarıyla yazıldı.");
                } else {
                    resultBuilder.llmSuccess(false);
                    log.warn("LLM kodları alınamadı/boş.");
                }
            } catch (Exception e) {
                log.error("LLM kod geliştirme sırasında hata!", e);
                resultBuilder.llmSuccess(false).errorMessage("LLM Error: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Kod oluşturma sırasında kritik hata!", e);
            resultBuilder.skeletonSuccess(false).llmAttempted(false).llmSuccess(false).errorMessage("Code Generation Error: " + e.getMessage());
        }
        return resultBuilder.build();
    }
}