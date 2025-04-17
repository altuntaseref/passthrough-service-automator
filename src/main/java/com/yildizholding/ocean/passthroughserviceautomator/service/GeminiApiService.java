package com.yildizholding.ocean.passthroughserviceautomator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiApiService {

    private final ChatClient.Builder builder;


    public String generateContentFromGemini(String text) {
        var client = builder.build();
        var response = client.prompt(text)
                .call()
                .content();

        try {
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling Gemini API: " + e.getMessage();
        }
    }
}
