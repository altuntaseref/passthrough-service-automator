package com.yildizholding.ocean.passthroughserviceautomator.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metin içerisindeki Java kod bloklarını ('```java ... ```') ayrıştırıp,
 * paket yapısına uygun olarak ayrı .java dosyaları halinde kaydeden sınıf.
 */
@Service
public class JavaCodeExtractor {

    // ```java bloğunu ve içeriğini yakalamak için regex
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```java\\s*?(.*?)\\s*?```", Pattern.DOTALL);
    // package satırını yakalamak için regex
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
    // public class/interface/enum ismini yakalamak için regex (basit hali)
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\b(?:public|protected|private|abstract|final|static)?\\s*(?:class|interface|enum)\\s+([a-zA-Z0-9_]+)", Pattern.MULTILINE);

    /**
     * Verilen metin içerisindeki Java kod bloklarını ayrıştırır ve
     * belirtilen ana dizin altına paket yapısına uygun olarak kaydeder.
     *
     * @param rawInput        Java kod bloklarını içeren tam metin (dinamik olarak sağlanabilir).
     * @param outputBaseDir   Dosyaların kaydedileceği ana dizin yolu (örn: "src/main/java" veya "extracted_code").
     * @return Kaydedilen dosyaların tam yollarını içeren bir liste.
     * @throws IOException Dosya yazma veya dizin oluşturma sırasında hata oluşursa.
     */
    public List<Path> extractAndSaveFiles(String rawInput, String outputBaseDir) throws IOException {
        if (rawInput == null || rawInput.isBlank()) {
            System.out.println("Girdi metni boş veya null, işlem yapılmayacak.");
            return Collections.emptyList(); // Return an empty list for null/blank input
        }
        
        List<Path> savedFiles = new ArrayList<>();
        Matcher blockMatcher = CODE_BLOCK_PATTERN.matcher(rawInput);

        while (blockMatcher.find()) {
            String codeContent = blockMatcher.group(1).trim(); // ```java ve ``` arasındaki temiz kod

            if (codeContent.isEmpty()) {
                continue;
            }

            String packageName = extractPackageName(codeContent);
            String className = extractClassName(codeContent);

            if (packageName == null) {
                System.err.println("WARN: Paket ismi bulunamadı. Bu kod bloğu atlanıyor:\n---\n" + codeContent.substring(0, Math.min(100, codeContent.length())) + "...\n---");
                continue;
            }
            if (className == null) {
                System.err.println("WARN: Sınıf/Interface/Enum ismi bulunamadı. Bu kod bloğu atlanıyor:\n---\n" + codeContent.substring(0, Math.min(100, codeContent.length())) + "...\n---");
                continue;
            }

            // Paket ismini dosya yolu formatına çevir (örn: com.example -> com/example)
            String packagePath = packageName.replace('.', '/');
            Path outputDir = Paths.get(outputBaseDir, packagePath);
            Path outputFile = outputDir.resolve(className + ".java");

            // Gerekli dizinleri oluştur
            Files.createDirectories(outputDir);

            // Dosyayı UTF-8 olarak yaz
            Files.writeString(outputFile, codeContent, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, // Dosya yoksa oluştur
                    StandardOpenOption.TRUNCATE_EXISTING); // Dosya varsa içeriğini sil ve yeniden yaz

            System.out.println("Kaydedildi: " + outputFile.toAbsolutePath());
            savedFiles.add(outputFile);
        }

        if (savedFiles.isEmpty() && blockMatcher.reset().find()) {
             // Regex en az bir blok bulduysa ama hiç dosya kaydedilmediyse (paket/sınıf adı hatası vb.)
             System.out.println("Kod blokları bulundu ancak geçerli paket/sınıf adı içeren blok kaydedilemedi.");
        } else if (savedFiles.isEmpty()) {
             System.out.println("Metin içinde '```java ... ```' formatında geçerli kod bloğu bulunamadı.");
        }


        return savedFiles;
    }

    private String extractPackageName(String codeBlock) {
        Matcher matcher = PACKAGE_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(1); // Yakalanan grup (paket ismi)
        }
        return null; // Paket bulunamadı
    }

    private String extractClassName(String codeBlock) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(1); // Yakalanan grup (sınıf ismi)
        }
        return null; // Sınıf ismi bulunamadı
    }
}