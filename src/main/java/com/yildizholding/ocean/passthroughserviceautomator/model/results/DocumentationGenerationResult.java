package com.yildizholding.ocean.passthroughserviceautomator.model.results;

import lombok.Builder;
import lombok.Getter;
import java.util.Collections; // Boş liste için
import java.util.List;

@Getter
@Builder // Builder pattern ile kolay oluşturma
public class DocumentationGenerationResult {

    @Builder.Default // Builder'da set edilmezse varsayılan değer
    private boolean attempted = false; // Doküman oluşturma denendi mi?

    @Builder.Default
    private boolean success = false;   // Başarılı oldu mu?

    @Builder.Default
    private List<String> generatedFileRelativePaths = Collections.emptyList(); // Oluşturulan dosyaların göreli yolları (örn: ["README.md", "postman_collection.json"])

    private String errorMessage; // Hata mesajı

    // Başarısızlık durumu için kolay oluşturucu
    public static DocumentationGenerationResult failure(String message) {
        return DocumentationGenerationResult.builder()
                .attempted(true)
                .success(false)
                .errorMessage(message)
                .build();
    }

    // Atlanma durumu için kolay oluşturucu
    public static DocumentationGenerationResult skipped() {
        return DocumentationGenerationResult.builder()
                .attempted(false) // Hiç denenmedi
                .success(false)
                .build();
    }

     // Başarı durumu için kolay oluşturucu
     public static DocumentationGenerationResult success(List<String> generatedFiles) {
        return DocumentationGenerationResult.builder()
                .attempted(true)
                .success(true)
                .generatedFileRelativePaths(generatedFiles != null ? generatedFiles : Collections.emptyList())
                .build();
    }
}