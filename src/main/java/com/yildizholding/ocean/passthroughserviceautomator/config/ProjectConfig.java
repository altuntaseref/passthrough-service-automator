package com.yildizholding.ocean.passthroughserviceautomator.config;

import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Data
public class ProjectConfig {

    private String projectName;
    private String groupId;
    private String artifactId;
    private String packageName;
    private String outputPath;
    private List<String> dependencies;
    private String javaVersion;
    private String springBootVersion;

    private static ProjectConfig instance;


    public static ProjectConfig getInstance() throws IOException {
        if (instance == null) {
            synchronized (ProjectConfig.class) {
                if (instance == null) {
                    Yaml yaml = new Yaml();
                    InputStream inputStream = ProjectConfig.class.getClassLoader().getResourceAsStream("project-config.yaml");
                    if (inputStream == null) {
                        throw new IOException("project-config.yaml sınıf yolunda bulunamadı");
                    }
                    instance = yaml.loadAs(inputStream, ProjectConfig.class);
                }
            }
        }
        return instance;
    }
}
