package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

import java.util.Map;

@Data
public class TokenServiceConfig {
    private String tokenUrl; // Token servisi URL'si
    private String clientId; // Client ID
    private String clientSecret; // Client Secret
    private String grantType; // Örn: "client_credentials"

}
