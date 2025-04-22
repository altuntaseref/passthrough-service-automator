package com.yildizholding.ocean.passthroughserviceautomator.service;

// ... (importlar: RestTemplate, Kong modelleri, Apache Commons Lang) ...
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterRequestModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterResponseModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.results.KongRegistrationResult; // Yeni sonuç modeli
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class KongIntegrationService {

    private final RestTemplate restTemplate;
    // ... (Diğer @Value alanları: kongRegisterApiUrl, kongBaseDomain) ...
     @Value("${kong.register.api.url}")
    private String kongRegisterApiUrl;
    @Value("${kong.base.domain}")
    private String kongBaseDomain;

    private static final String KONG_ENV = "UAT";
    private static final boolean KONG_BASIC_AUTH = true;
    private static final int PASSWORD_LENGTH = 16;


    public KongRegistrationResult registerServiceOnKong(RestProjectRequest automatorRequest) {
        if (!automatorRequest.isRegisterOnKong()) {
             return KongRegistrationResult.skipped(); // İstenmiyorsa atlandı sonucu dön
        }

        KongRegistrationResult.KongRegistrationResultBuilder resultBuilder = KongRegistrationResult.builder().attempted(true);
        log.info("Kong kaydı başlatılıyor: Proje Adı={}", automatorRequest.getProjectName());

        try {
            OceanServiceRegisterRequestModel kongRequest = buildKongRequest(automatorRequest);
            // ... (API'ye istek gönderme - önceki gibi) ...
             HttpEntity<OceanServiceRegisterRequestModel> requestEntity = new HttpEntity<>(kongRequest, createHeaders());
             ResponseEntity<OceanServiceRegisterResponseModel> responseEntity = restTemplate.postForEntity(
                 kongRegisterApiUrl, requestEntity, OceanServiceRegisterResponseModel.class
             );


            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null && "Success".equalsIgnoreCase(responseEntity.getBody().getResult())) {
                OceanServiceRegisterResponseModel apiResponse = responseEntity.getBody();
                resultBuilder.success(true)
                             .serviceUrl(apiResponse.getServiceLink())
                             .username(apiResponse.getServiceUsername());
                log.info("Kong kaydı başarılı.");
            } else {
                 String errorMsg = "Kong API returned failure or invalid response. Status: " + responseEntity.getStatusCode();
                 log.error(errorMsg);
                 resultBuilder.success(false).errorMessage(errorMsg);
            }
        } catch (RestClientException e) {
            log.error("Kong API'sine bağlanırken hata: {}", e.getMessage());
            resultBuilder.success(false).errorMessage("Kong API Connection Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Kong kaydı sırasında beklenmedik hata.", e);
            resultBuilder.success(false).errorMessage("Unexpected Kong Error: " + e.getMessage());
        }
        return resultBuilder.build();
    }

    // --- Private Yardımcı Metotlar ---
    // buildKongRequest, formatAppName, createHeaders (önceki yanıttaki gibi)
    private OceanServiceRegisterRequestModel buildKongRequest(RestProjectRequest automatorRequest) { /* ... önceki kod ... */
         OceanServiceRegisterRequestModel kongRequest = new OceanServiceRegisterRequestModel();
         String appName = formatAppName(automatorRequest.getProjectName());
         kongRequest.setServiceLink(String.format("https://%s.%s/", appName, kongBaseDomain));
         kongRequest.setUsername(appName + "-" + KONG_ENV.toLowerCase());
         kongRequest.setPassword(RandomStringUtils.randomAlphanumeric(PASSWORD_LENGTH));
         kongRequest.setKongServer(KONG_ENV);
         kongRequest.setBasicAuth(KONG_BASIC_AUTH);
         return kongRequest;
    }
    private String formatAppName(String projectName) { /* ... önceki kod ... */
         if (projectName == null) return "unknown-app";
         return projectName.toLowerCase().replace("-", "").replaceAll("[^a-z0-9]", "");
    }
     private HttpHeaders createHeaders() { /* ... önceki kod ... */
          HttpHeaders headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);
          return headers;
     }
}