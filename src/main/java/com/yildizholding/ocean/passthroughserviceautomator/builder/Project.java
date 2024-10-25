package com.yildizholding.ocean.passthroughserviceautomator.builder;

import com.yildizholding.ocean.passthroughserviceautomator.model.RegisterServiceResponse;
import lombok.Data;

import java.util.List;

@Data
public class Project {
    private String projectPath;
    private List<RegisterServiceResponse> registerServiceResponses;
}
