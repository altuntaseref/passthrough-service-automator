package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

@Data

public class RegisterServiceRequest {
    private String serviceLink;
    private String username;
    private String password;
    private String kongServer;
    private boolean basicAuth;

    // Getter ve Setter metotları
}

