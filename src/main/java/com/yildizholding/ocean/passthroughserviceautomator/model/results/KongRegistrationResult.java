package com.yildizholding.ocean.passthroughserviceautomator.model.results;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KongRegistrationResult {
    private boolean attempted;
    private boolean success;
    private String serviceUrl;
    private String username;
    // Şifre DÖNMEYECEK
    private String errorMessage;

    public static KongRegistrationResult skipped() { // Atlanma durumu için
        return KongRegistrationResult.builder().attempted(false).success(false).build();
    }
}