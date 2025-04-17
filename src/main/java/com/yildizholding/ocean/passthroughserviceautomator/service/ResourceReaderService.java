package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.util.FileUtils; // Gerekliyse
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceReaderService {

    private static final String CONTROLLER_TYPE = "controller";
    private static final String SERVICE_TYPE = "service";

    /**
     * Aşama 1'de oluşturulan Controller veya Service şablon dosyasının içeriğini okur.
     */
    public Optional<String> readGeneratedTemplateContent(RestProjectRequest request, String type) {
        String className = null;
        Path filePath = null;
        try {
            String systemNameCapitalized = FileUtils.capitalizeFirstLetter(request.getSystemName());
            className = systemNameCapitalized + (type.equals(CONTROLLER_TYPE) ? "Controller" : "Service");
            String subPackage = type;
            String mainSrcPath = request.generateProjectSrcMain();
            if (mainSrcPath == null) {
                log.error("'generateProjectSrcMain()' null döndü.");
                return Optional.empty();
            }
            filePath = Paths.get(mainSrcPath, "java",
                    request.getPackageName().replace('.', File.separatorChar),
                    subPackage, className + ".java");

            if (Files.exists(filePath)) {
                log.debug("Okunuyor (generated template): {}", filePath);
                return Optional.of(Files.readString(filePath, StandardCharsets.UTF_8));
            } else {
                log.error("Oluşturulmuş şablon dosyası bulunamadı: {}", filePath);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("{} tipi için oluşturulmuş şablon dosyası ({}) okunurken hata oluştu", type, filePath, e);
            return Optional.empty();
        }
    }

    /**
     * Classpath'teki bir resource dosyasının içeriğini okur.
     */
    public String readClasspathResourceFileContent(String resourcePath) throws IOException {
        log.debug("Classpath resource okunuyor: classpath:{}", resourcePath);
        Resource resource = new ClassPathResource(resourcePath);
        if (!resource.exists()) {
            log.error("Classpath resource bulunamadı: classpath:{}", resourcePath);
            throw new FileNotFoundException("Resource not found in classpath: " + resourcePath);
        }
        try (InputStream inputStream = resource.getInputStream();
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            log.error("Classpath resource okunurken hata: classpath:{}", resourcePath, e);
            throw e;
        }
    }
}
