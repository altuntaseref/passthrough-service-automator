package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

@Data
public class OceanServiceRegisterModel {
    private String serviceLink;
    private String username;
    private String password;
    private String kongServer; // PROD1-PROD2
    private boolean basicAuth;
}
