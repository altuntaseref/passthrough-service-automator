package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class FreeMakeModel {
    private String templateFilePath;
    private Map<String, Object> args;
    private String fileName;
}
