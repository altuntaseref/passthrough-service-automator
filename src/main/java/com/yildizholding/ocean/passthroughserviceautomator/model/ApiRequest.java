package com.yildizholding.ocean.passthroughserviceautomator.model;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

@Data
public class ApiRequest {
    private String methodName; // Servis sınıfındaki metod ismi
    private String httpMethod; // "GET", "POST", "PUT", "DELETE"
    private String authType; // "Basic", "Bearer", "None"
    private String uri; // Endpoint URI
    private Map<String, String> pathParams; // Path parametreleri
    private Map<String, String> queryParams; // Query parametreleri
    private Map<String, String> headers; // Özel header'lar
    private JsonNode requestBody; // İstek gövdesi (JSON nesnesi)
    private String requestClassName; // İstek gövdesi sınıfının ismi
    private JsonNode responseBody; // Yanıt gövdesi (JSON nesnesi)
    private String responseClassName; // Yanıt sınıfının ismi
    private String description; // İsteğin açıklaması (opsiyonel)
}
