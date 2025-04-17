package com.yildizholding.ocean.passthroughserviceautomator.controller;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectResponse;
import com.yildizholding.ocean.passthroughserviceautomator.model.ResponseModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.UserModel;
import com.yildizholding.ocean.passthroughserviceautomator.service.GeminiApiService;
import com.yildizholding.ocean.passthroughserviceautomator.service.JavaCodeExtractor;
import com.yildizholding.ocean.passthroughserviceautomator.service.ProjectServie;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final ProjectServie projectServie;
    private final GeminiApiService geminiApiService;
    private final JavaCodeExtractor javaCodeExtractor;

    @PostMapping("/generate")
    public ProjectResponse generateProject(@RequestBody RestProjectRequest request) {
        return projectServie.generateProject(request);
    }

    @PostMapping("/promt")
    public String generatePromt(@RequestBody String promt) throws IOException {
        String responsePromt = geminiApiService.generateContentFromGemini(promt);
        javaCodeExtractor.extractAndSaveFiles(responsePromt, "C:\\Users\\seraf\\Desktop\\auto-web-service\\passthrough-service-automator\\src\\main\\resources\\class");
        return responsePromt;
    }
}
