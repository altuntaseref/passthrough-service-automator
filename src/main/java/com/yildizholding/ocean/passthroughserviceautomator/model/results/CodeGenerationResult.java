package com.yildizholding.ocean.passthroughserviceautomator.model.results;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;
import java.nio.file.Path;

@Getter
@Builder // Builder pattern ile oluşturmak kolaylaşır
public class CodeGenerationResult {
    private boolean skeletonSuccess;
    private boolean llmAttempted;
    private boolean llmSuccess;
    private Map<Path, String> generatedCodeMap; // LLM'den gelen kod (opsiyonel)
    private String errorMessage;
}