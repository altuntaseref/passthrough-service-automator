package com.yildizholding.ocean.sappladisanlikker.service;

import com.yildizholding.ocean.sappladisanlikker.config.SapServiceConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SapServiceTest {

    @Mock
    private SapServiceConfig serviceConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SapService sapService;

    @Test
    void pladisAnlik_shouldReturnResponseFromRestTemplate() {
        Object request = new Object();
        Object expectedResponse = new Object();
        String baseUrl = "https://example.com";
        String url = baseUrl + "/pladispaperik/PladisPaperIk?sap-client=500";

        when(serviceConfig.getBaseUrl()).thenReturn(baseUrl);
        when(serviceConfig.getUsername()).thenReturn("testUser");
        when(serviceConfig.getPassword()).thenReturn("testPass");
        when(restTemplate.exchange(eq(url), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        ResponseEntity<Object> response = sapService.pladisAnlik(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void flexo_shouldReturnResponseFromRestTemplate() {
        Object request = new Object();
        Object expectedResponse = new Object();
        String baseUrl = "https://example.com";
        String url = baseUrl + "/one_rest_aodf/CreateAodf";

        when(serviceConfig.getBaseUrl()).thenReturn(baseUrl);
        when(serviceConfig.getUsername()).thenReturn("testUser");
        when(serviceConfig.getPassword()).thenReturn("testPass");
        when(restTemplate.exchange(eq(url), eq(HttpMethod.POST), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        ResponseEntity<Object> response = sapService.flexo(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }
}