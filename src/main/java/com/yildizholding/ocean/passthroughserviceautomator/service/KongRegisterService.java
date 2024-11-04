//package com.yildizholding.ocean.passthroughserviceautomator.service;
//
//import com.yildizholding.ocean.passthroughserviceautomator.config.KongServiceConfig;
//import com.yildizholding.ocean.passthroughserviceautomator.config.ProjectConfig;
//import com.yildizholding.ocean.passthroughserviceautomator.model.OceanServiceRegisterModel;
//import com.yildizholding.ocean.passthroughserviceautomator.model.OceanServiceRegisterResponseModel;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//
//import java.util.Base64;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class KongRegisterService {
//
//    private final KongServiceConfig kongServiceConfig;
//
//
//
//
//    public OceanServiceRegisterResponseModel registerService(OceanServiceRegisterModel request) {
//        RestTemplate restTemplate = new RestTemplate();
//        String url = kongServiceConfig.getBaseUrl() + "/register"; // İstek atılacak URL
//
//        // İstek için header'ları oluşturuyoruz
//        HttpHeaders headers = new HttpHeaders();
//        if (basicAuth) {
//            String auth = username + ":" + password;
//            String encodedAuth = new String(Base64.getEncoder().encode(auth.getBytes()));
//            headers.set("Authorization", "Basic " + encodedAuth);
//        }
//
//        // Eğer gerekirse diğer header'lar da buraya eklenebilir.
//        // İstek gövdesi (body) boş olabilir veya bir request body eklenebilir
//        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
//
//        // Servise istek atma
//        ResponseEntity<OceanServiceRegisterResponseModel> response = restTemplate.exchange(
//                url,
//                HttpMethod.POST,
//                requestEntity,
//                OceanServiceRegisterResponseModel.class
//        );
//
//        // Response'u dönüyoruz
//        return response.getBody();
//    }
//}
//
//}
