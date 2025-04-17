package com.yildizholding.ocean.sappladisanlikker.controller;

import com.yildizholding.ocean.sappladisanlikker.service.SapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SapControllerTest {

    @Mock
    private SapService sapService;

    @InjectMocks
    private SapController sapController;

    @Test
    void pladisAnlik_shouldReturnResponseFromService() {
        Object request = new Object();
        Object expectedResponse = new Object();
        when(sapService.pladisAnlik(request)).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        ResponseEntity<Object> response = sapController.pladisAnlik(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void flexo_shouldReturnResponseFromService() {
        Object request = new Object();
        Object expectedResponse = new Object();
        when(sapService.flexo(request)).thenReturn(new ResponseEntity<>(expectedResponse, HttpStatus.OK));

        ResponseEntity<Object> response = sapController.flexo(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }
}