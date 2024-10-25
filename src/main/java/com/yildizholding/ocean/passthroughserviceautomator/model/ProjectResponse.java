package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

import java.util.List;

@Data
public class ProjectResponse {
    private String projectPath;
    private List<RegisterServiceResponse> registerServiceResponses;
    private String message;
}
