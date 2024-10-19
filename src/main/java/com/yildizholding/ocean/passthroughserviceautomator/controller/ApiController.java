package com.yildizholding.ocean.passthroughserviceautomator.controller;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.ResponseModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.UserModel;
import com.yildizholding.ocean.passthroughserviceautomator.service.ProjectServie;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final ProjectServie projectServie;

    @PostMapping("/generate")
    public String generateProject(@RequestBody ProjectRequest request) {
     return projectServie.generateProject(request);
    }

    @PostMapping("/getResponse")
    public ResponseModel getResponse(@RequestBody UserModel userModel){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setCustomers("customer123");
        responseModel.setDetails("NoDetail");
        return responseModel;
    }

}
