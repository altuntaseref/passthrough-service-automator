package com.yildizholding.ocean.passthroughserviceautomator.model;

import lombok.Data;

import java.util.HashMap;

@Data
public class FreeMakeModel {
    private String templateFilePath;
    private HashMap<String, Object> args;
    private String fileName;
}
