package com.yildizholding.ocean.passthroughserviceautomator.model.kong; // Farklı paket olabilir
import lombok.Data;
@Data
public class OceanServiceRegisterResponseModel {
    private String serviceLink;
    private String serviceUsername;
    private String servicePassword;
    private String result; // "Success" veya "Fail" gibi
}