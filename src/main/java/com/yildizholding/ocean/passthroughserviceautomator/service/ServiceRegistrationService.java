package com.yildizholding.ocean.passthroughserviceautomator.service;


import com.yildizholding.ocean.passthroughserviceautomator.model.ApiRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRegistrationService {

    private final RestTemplate restTemplate;
    private static final String REGISTER_SERVICE_URL = "https://kong-service-register-api.apps.ocean-octest.yildiz.domain/v1//registerService";


    public List<RegisterServiceResponse> registerService(String applicationName,  List<ApiRequest> apiRequests) {

        String username = oceanUsername(applicationName);
        String password = generatePassword(16);

        List<RegisterServiceResponse> registerServiceResponseList = new ArrayList<>();

        for (ApiRequest apiRequest : apiRequests) {
            RegisterServiceRequest request = new RegisterServiceRequest();
            request.setServiceLink("https://oceantest.yildizholding.com.tr/" + applicationName + apiRequest.getUri());
            request.setUsername(username);
            request.setPassword(password);
            request.setKongServer("UAT");
            request.setBasicAuth(true);
            registerServiceResponseList.add(executeOceanKongApi(request));
        }
        return registerServiceResponseList;
    }

    private RegisterServiceResponse executeOceanKongApi(RegisterServiceRequest request){
        RegisterServiceResponse response = restTemplate.postForObject(
                REGISTER_SERVICE_URL,
                request,
                RegisterServiceResponse.class
        );

        return response;
    }

    private String generatePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(chars.length());
            sb.append(chars.charAt(idx));
        }

        return sb.toString();
    }

    private String oceanUsername(String appName){
        return appName+"-uat";
    }
}
