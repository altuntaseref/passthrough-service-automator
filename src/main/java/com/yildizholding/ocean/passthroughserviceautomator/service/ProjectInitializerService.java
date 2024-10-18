package com.yildizholding.ocean.passthroughserviceautomator.service;


import com.yildizholding.ocean.passthroughserviceautomator.model.ProjectRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.List;

@Service
public class ProjectInitializerService {


    public void generateProject(ProjectRequest request) throws IOException {

        String url = "https://start.spring.io/starter.zip";
        request.generatePackageName();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("type", "maven-project")
                .queryParam("language", "java")
                .queryParam("bootVersion", request.getSpringBootVersion())
                .queryParam("javaVersion", request.getJavaVersion())
                .queryParam("groupId", request.getGroupId())
                .queryParam("artifactId", request.getArtifactId())
                .queryParam("name", request.getProjectName())
                .queryParam("packageName", request.getPackageName())
                .queryParam("dependencies", String.join(",", request.getDependencies()));

        URI uri = builder.build().encode().toUri();

        // Spring Initializr API'sine istek gönder ve zip dosyasını al
        RestTemplate restTemplate = new RestTemplate();
        byte[] zipBytes = restTemplate.getForObject(uri, byte[].class);

        // Zip dosyasını geçici bir konuma kaydet
        Path zipPath = Paths.get(System.getProperty("java.io.tmpdir"), request.getProjectName() + ".zip");
        Files.write(zipPath, zipBytes);

        // Projenin çıkarılacağı dizini belirleme (outputPath)
        Path extractDir = Paths.get(request.getOutputPath(), request.getProjectName());
        if (!Files.exists(extractDir)) {
            Files.createDirectories(extractDir); // Klasörü oluştur
        }
        // Zip dosyasını aç ve hedef dizine çıkar
        unzip(zipPath, extractDir);

        // Zip dosyasını sildikten sonra
        Files.delete(zipPath);

        System.out.println("Proje oluşturuldu: " + extractDir.toAbsolutePath());
    }

    private void unzip(Path zipFilePath, Path destDir) throws IOException {
        if (!Files.exists(destDir)) {
            Files.createDirectories(destDir);
        }

        try (FileSystem fs = FileSystems.newFileSystem(zipFilePath, (ClassLoader) null)) {
            for (Path root : fs.getRootDirectories()) {
                Files.walk(root).forEach(source -> {
                    try {
                        Path destination = destDir.resolve(root.relativize(source).toString());
                        if (Files.isDirectory(source)) {
                            if (!Files.exists(destination)) {
                                Files.createDirectories(destination);
                            }
                        } else {
                            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Dosya kopyalama hatası", e);
                    }
                });
            }
        }
    }
    public void createPackageStructure(ProjectRequest request) {
        try {
            // Projenin src/main/java dizini
            Path srcMainJava = Paths.get(request.generateProjectPath(), "src", "main", "java");
            String packagePath = request.getPackageName().replace(".", File.separator);

            // Paket isimleri
            String[] packages = {"controller", "service", "config", "model"};

            // Her paket için dizin oluşturma
            for (String pkg : packages) {
                Path packageDir = srcMainJava.resolve(Paths.get(packagePath, pkg));
                if (!Files.exists(packageDir)) {
                    Files.createDirectories(packageDir);
                    System.out.println("Paket oluşturuldu: " + packageDir.toString());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}