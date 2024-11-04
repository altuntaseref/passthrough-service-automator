package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

import java.util.List;

@Data
public class Project {
    private String projectPath;
    private List<RegisterServiceResponse> registerServiceResponses;
}
