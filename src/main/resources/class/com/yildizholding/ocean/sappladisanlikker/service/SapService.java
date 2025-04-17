package com.yildizholding.ocean.sappladisanlikker.service;

import com.yildizholding.ocean.sappladisanlikker.config.SapServiceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SapService {

    private final SapServiceConfig serviceConfig;
    private final RestTemplate restTemplate;

    public ResponseEntity<Object> pladisAnlik(Object pladisAnlikRequest) {
        String url = serviceConfig.getBaseUrl() + "/pladispaperik/PladisPaperIk?sap-client=500";
        return sendPostRequest(url, pladisAnlikRequest, serviceConfig.getUsername(), serviceConfig.getPassword());
    }

    public ResponseEntity<Object> flexo(Object flexoRequest) {
        String url = serviceConfig.getBaseUrl() + "/one_rest_aodf/CreateAodf";
        return sendPostRequest(url, flexoRequest, serviceConfig.getUsername(), serviceConfig.getPassword());
    }

    private ResponseEntity<Object> sendPostRequest(String url, Object requestBody, String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(username, password);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);
        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, Object.class);
        } catch (HttpStatusCodeException e) {
            HttpStatus statusCode = e.getStatusCode();
            String errorResponse = e.getResponseBodyAsString();
            return ResponseEntity.status(statusCode).body(errorResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error: " + e.getMessage());
        }
    }
}