package com.yildizholding.ocean.passthroughserviceautomator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.service.LlmService;
import com.yildizholding.ocean.passthroughserviceautomator.util.JavaCodeExtractor;
import com.yildizholding.ocean.passthroughserviceautomator.service.ProjectServie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ApiController {

    private final ProjectServie projectServie;
    private final LlmService llmService;
    private final JavaCodeExtractor javaCodeExtractor;
    private final ObjectMapper objectMapper; // ObjectMapper'ı inject et


//    @PostMapping("/generate")
//    public ProjectResponse generateProject(@RequestBody RestProjectRequest request) {
//        return projectServie.generateProject(request);
//    }

    @PostMapping(value = "/generate", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ProjectResponse generateProject(
            @RequestPart("request") String requestJson, // String olarak al
            @RequestPart("postmanFile") MultipartFile postmanFile) {
        try {
            // String JSON'ı RestProjectRequest nesnesine dönüştür
            RestProjectRequest request = objectMapper.readValue(requestJson, RestProjectRequest.class);
            return projectServie.generateProject(request, postmanFile);
        } catch (IOException e) {
            // JSON parse hatası
            log.error("Request JSON parse edilirken hata oluştu: {}", e.getMessage());
            // Uygun bir hata yanıtı döndür
            return createErrorResponse("Invalid request data format.");
        }
    }
    private ProjectResponse createErrorResponse(String message) {
        ProjectResponse response = new ProjectResponse();
        response.setMessage("Error: " + message);
        // response.setProjectPath(null); // veya uygun bir değer
        return response;
    }

    @PostMapping("/promt")
    public String generatePromt(@RequestBody String promt) throws IOException {
        String responsePromt = llmService.getCompletion(promt);
        javaCodeExtractor.extractAndSaveFiles(responsePromt, "C:\\Users\\seraf\\Desktop\\auto-web-service\\passthrough-service-automator\\src\\main\\resources\\class");
        return responsePromt;
    }
}
