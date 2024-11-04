package com.yildizholding.ocean.passthroughserviceautomator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("kong")
public class KongServiceConfig {
    private String baseUrl;
    private String username;
    private String password;
}
