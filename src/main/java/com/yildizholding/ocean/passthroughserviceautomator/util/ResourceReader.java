package com.yildizholding.ocean.passthroughserviceautomator.util;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourceReader {

    public static String readResourceAsString(String resourceName) {
        try {
            // 1) Kaynağın URL’ini al
            var url = ResourceReader.class.getClassLoader().getResource(resourceName);
            if (url == null) {
                throw new IllegalArgumentException("Kaynak bulunamadı: " + resourceName);
            }

            // 2) URL → Path
            Path path = Path.of(url.toURI());

            // 3) Dosyayı tek hamlede String’e oku
            return Files.readString(path, StandardCharsets.UTF_8);

        } catch (URISyntaxException | java.io.IOException e) {
            // Uygulamanızın hata yönetimi politikasına göre yeniden atın veya sarın
            throw new RuntimeException("Kaynak okunamadı: " + resourceName, e);
        }
    }
}
