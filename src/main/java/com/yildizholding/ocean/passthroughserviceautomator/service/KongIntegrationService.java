package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterRequestModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterResponseModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class KongIntegrationService {

    private final RestTemplate restTemplate; // Inject edilen RestTemplate

    @Value("${kong.register.api.url}")
    private String kongRegisterApiUrl;

    @Value("${kong.base.domain}")
    private String kongBaseDomain;

    private static final String KONG_ENV = "UAT";
    private static final boolean KONG_BASIC_AUTH = true;
    private static final int PASSWORD_LENGTH = 16;

    /**
     * Yeni oluşturulan servisi Kong'a kaydetmek için API isteği gönderir.
     * @param automatorRequest Passthrough automator'ın orijinal isteği (proje adı için).
     * @return Kong API'sinden dönen yanıt veya hata durumunda null.
     */
    public OceanServiceRegisterResponseModel registerServiceOnKong(RestProjectRequest automatorRequest) {
        log.info("Kong kaydı başlatılıyor: Proje Adı={}", automatorRequest.getProjectName());
        try {
            // Kong API isteğini oluştur
            OceanServiceRegisterRequestModel kongRequest = buildKongRequest(automatorRequest);
            log.debug("Oluşturulan Kong İsteği: {}", kongRequest); // Dikkat: Şifre loglanabilir

            // API'ye POST isteği gönder
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Gerekirse Kong Register API için ek auth header'ları buraya ekleyin
            // headers.setBearerAuth("REGISTER_API_TOKEN");

            HttpEntity<OceanServiceRegisterRequestModel> requestEntity = new HttpEntity<>(kongRequest, headers);

            ResponseEntity<OceanServiceRegisterResponseModel> responseEntity = restTemplate.postForEntity(
                kongRegisterApiUrl,
                requestEntity,
                OceanServiceRegisterResponseModel.class
            );

            // Yanıtı kontrol et ve döndür
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                log.info("Kong servis kaydı yanıtı alındı: {}", responseEntity.getBody());
                return responseEntity.getBody();
            } else {
                log.error("Kong Register API'den başarısız yanıt alındı: Status={}, Body={}",
                          responseEntity.getStatusCode(), responseEntity.getBody());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Kong Register API'sine bağlanırken hata oluştu: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Kong kaydı sırasında beklenmedik hata.", e);
            return null;
        }
    }

    /** Kong API'sine gönderilecek request modelini oluşturur. */
    private OceanServiceRegisterRequestModel buildKongRequest(RestProjectRequest automatorRequest) {
        OceanServiceRegisterRequestModel kongRequest = new OceanServiceRegisterRequestModel();
        String appName = formatAppName(automatorRequest.getProjectName()); // veya systemName

        // Service Link: https://uygulama-adi.apps.oceantest.yildizdomain/
        kongRequest.setServiceLink(String.format("https://%s.%s/", appName, kongBaseDomain));

        // Username: uygulama-adi-uat
        kongRequest.setUsername(appName + "-" + KONG_ENV.toLowerCase());

        // Password: Rastgele 16 haneli
        kongRequest.setPassword(RandomStringUtils.randomAlphanumeric(PASSWORD_LENGTH));

        // Diğer sabit değerler
        kongRequest.setKongServer(KONG_ENV);
        kongRequest.setBasicAuth(KONG_BASIC_AUTH);

        return kongRequest;
    }

    /** Proje adını URL/Username için uygun formata getirir. */
    private String formatAppName(String projectName) {
        if (projectName == null) return "unknown-app";
        return projectName.toLowerCase()
                          .replace("-", "") // Tireleri kaldır (veya kurala göre bırak)
                          .replaceAll("[^a-z0-9]", ""); // Sadece harf ve rakam bırak
    }
}