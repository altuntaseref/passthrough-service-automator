package com.yildizholding.ocean.passthroughserviceautomator.util;

import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;

public class FileUtils {

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String decapitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    public static String getModulePath(ProjectRequest request, String subPackage) {
        String packagePath = request.getPackageName().replace(".", "\\");
        return request.generateProjectSrcMain() + "java\\" + packagePath + "\\" + subPackage;
    }
}
