package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

@Data
public class RegisterServiceResponse {
    private String serviceLink;
    private String serviceUsername;
    private String servicePassword;
    private String result;

    // Getter ve Setter metotları
}
