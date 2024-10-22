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

    public static String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // "-" ve "_" karakterlerine göre stringi böl
        String[] parts = input.split("[-_]");
        StringBuilder camelCaseString = new StringBuilder(parts[0].toLowerCase());

        // İlk kelimeden sonra gelen kelimelerin ilk harfini büyük yap
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                camelCaseString.append(parts[i].substring(0, 1).toUpperCase());
                camelCaseString.append(parts[i].substring(1).toLowerCase());
            }
        }

        return camelCaseString.toString();
    }
}
