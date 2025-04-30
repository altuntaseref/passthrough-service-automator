package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterRequestModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterResponseModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.results.KongRegistrationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.regex.Pattern; // formatAppName için
import java.util.regex.Matcher;  // formatAppName için


@Service
@Slf4j
@RequiredArgsConstructor
public class KongIntegrationService {

    private final RestTemplate restTemplate;

    @Value("${kong.register.api.url}")
    private String kongRegisterApiUrl;

    @Value("${kong.base.domain}")
    private String kongBaseDomain;

    // --- YENİ: Simülasyon için flag ---
    @Value("${kong.simulate.success:false}") // Varsayılan olarak simülasyon kapalı (false)
    private boolean simulateKongSuccess;
    // ---------------------------------

    private static final String KONG_ENV = "UAT"; // Kong username için sabit
    private static final boolean KONG_BASIC_AUTH = true; // Gönderilecek istek için sabit
    private static final int PASSWORD_LENGTH = 16; // Simüle şifre uzunluğu

    /**
     * Yeni oluşturulan servisi Kong'a kaydeder veya simüle eder.
     *
     * @param automatorRequest Passthrough automator'ın orijinal isteği.
     * @return Kong kayıt işleminin sonucunu içeren KongRegistrationResult nesnesi.
     */
    public KongRegistrationResult registerServiceOnKong(RestProjectRequest automatorRequest) {
        if (!automatorRequest.isRegisterOnKong()) {
            log.info("Kong kaydı istekte belirtilmediği için atlanıyor.");
            return KongRegistrationResult.skipped();
        }

        // --- Simülasyon Kontrolü ---
        if (simulateKongSuccess) {
            log.warn("!!! KONG SİMÜLASYONU AKTİF !!! Gerçek API çağrısı yapılmayacak.");
            return simulateSuccessfulKongRegistration(automatorRequest);
        }
        // --- Simülasyon Kontrolü Sonu ---

        // Gerçek API Çağrısı Mantığı (Simülasyon kapalıysa çalışır)
        KongRegistrationResult.KongRegistrationResultBuilder resultBuilder = KongRegistrationResult.builder().attempted(true);
        log.info("Kong kaydı başlatılıyor (Gerçek API): Proje Adı={}", automatorRequest.getProjectName());

        try {
            OceanServiceRegisterRequestModel kongRequest = buildKongRequest(automatorRequest); // Request modelini oluştur
            log.debug("Oluşturulan Kong İsteği (Gerçek): {}", kongRequest); // Şifreye dikkat!

            HttpHeaders headers = createHeaders(); // Headerları oluştur
            HttpEntity<OceanServiceRegisterRequestModel> requestEntity = new HttpEntity<>(kongRequest, headers);

            log.info("Kong Register API'sine istek gönderiliyor: URL={}", kongRegisterApiUrl);
            ResponseEntity<OceanServiceRegisterResponseModel> responseEntity = restTemplate.postForEntity(
                    kongRegisterApiUrl,
                    requestEntity,
                    OceanServiceRegisterResponseModel.class
            );

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null && "Success".equalsIgnoreCase(responseEntity.getBody().getResult())) {
                OceanServiceRegisterResponseModel apiResponse = responseEntity.getBody();
                resultBuilder.success(true)
                        .serviceUrl(apiResponse.getServiceLink())
                        .username(apiResponse.getServiceUsername());
                log.info("Kong kaydı başarılı (Gerçek API).");
            } else {
                String errorMsg = "Kong API returned failure or invalid response. Status: " + responseEntity.getStatusCode();
                log.error(errorMsg);
                resultBuilder.success(false).errorMessage(errorMsg);
            }
        } catch (RestClientException e) {
            log.error("Kong API'sine bağlanırken hata (Gerçek API): {}", e.getMessage());
            resultBuilder.success(false).errorMessage("Kong API Connection Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Kong kaydı sırasında beklenmedik hata (Gerçek API).", e);
            resultBuilder.success(false).errorMessage("Unexpected Kong Error: " + e.getMessage());
        }
        return resultBuilder.build();
    }

    /**
     * Başarılı bir Kong kaydını simüle eder ve sahte verilerle sonuç döndürür.
     */
    private KongRegistrationResult simulateSuccessfulKongRegistration(RestProjectRequest automatorRequest) {
        log.info("Simüle edilmiş başarılı Kong yanıtı oluşturuluyor...");
        String appName = formatAppName(automatorRequest.getProjectName()); // veya systemName
        String simulatedUrl = String.format("https://oceangwuay.yildizdoiam.com.tr/%s.%s/", appName, kongBaseDomain);
        String simulatedUsername = appName + "-" + KONG_ENV.toLowerCase();
        // Simüle edilmiş başarılı şifre (döndürülmeyecek ama loglanabilir)
        // String simulatedPassword = RandomStringUtils.randomAlphanumeric(PASSWORD_LENGTH);
        // log.debug("Simüle Edilmiş Kong Şifresi (DÖNDÜRÜLMEYECEK): {}", simulatedPassword);

        return KongRegistrationResult.builder()
                .attempted(true) // Denendi (simülasyonla)
                .success(true)   // Başarılı varsay
                .serviceUrl(simulatedUrl)
                .username(simulatedUsername)
                .errorMessage(null) // Hata yok
                .build();
    }


    // --- Özel Yardımcı Metotlar ---

    /** Kong API'sine gönderilecek request modelini oluşturur. */
    private OceanServiceRegisterRequestModel buildKongRequest(RestProjectRequest automatorRequest) {
        OceanServiceRegisterRequestModel kongRequest = new OceanServiceRegisterRequestModel();
        String appName = formatAppName(automatorRequest.getProjectName()); // veya systemName

        kongRequest.setServiceLink(String.format("https://%s.%s/", appName, kongBaseDomain));
        kongRequest.setUsername(appName + "-" + KONG_ENV.toLowerCase());
        kongRequest.setPassword(RandomStringUtils.randomAlphanumeric(PASSWORD_LENGTH)); // Gerçek istekte şifre üretilir
        kongRequest.setKongServer(KONG_ENV);
        kongRequest.setBasicAuth(KONG_BASIC_AUTH);
        return kongRequest;
    }

    /** Proje adını URL/Username için uygun formata getirir. */
    private String formatAppName(String projectName) {
        if (!StringUtils.hasText(projectName)) return "unknown-app";
        String formatted = projectName.toLowerCase();
        formatted = formatted.replaceAll("\\s+", "-"); // Boşlukları tire yap
        formatted = formatted.replaceAll("[^a-z0-9\\-]+", ""); // Alfanumerik ve tire dışındakileri sil
        formatted = formatted.replaceAll("-+", "-"); // Çoklu tireleri tek yap
        formatted = formatted.replaceAll("^-+|-+$", "");
        return StringUtils.hasText(formatted) ? formatted : "unknown-app";
    }

    /** Kong API isteği için HTTP başlıklarını oluşturur. */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Gerekirse Kong Register API için ek auth header'ları buraya ekleyin
        // headers.setBearerAuth("KONG_REGISTER_API_TOKEN");
        return headers;
    }
}