package com.yildizholding.ocean.sappladisanlikker.controller;

import com.yildizholding.ocean.sappladisanlikker.service.SapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SapController {

    private final SapService sapService;

    @PostMapping("/pladis-anlik")
    public ResponseEntity<Object> pladisAnlik(@RequestBody Object pladisAnlikRequest) {
        return sapService.pladisAnlik(pladisAnlikRequest);
    }

    @PostMapping("/flexo")
    public ResponseEntity<Object> flexo(@RequestBody Object flexoRequest) {
        return sapService.flexo(flexoRequest);
    }
}