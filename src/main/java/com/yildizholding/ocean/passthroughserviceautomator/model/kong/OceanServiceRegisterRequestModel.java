package com.yildizholding.ocean.passthroughserviceautomator.model.kong; // Farklı paket olabilir
import lombok.Data;
@Data
public class OceanServiceRegisterRequestModel {
    private String serviceLink;
    private String username;
    private String password;
    private String kongServer; // UAT veya PROD
    private boolean basicAuth;
}