package com.yildizholding.ocean.passthroughserviceautomator.util;

import lombok.extern.slf4j.Slf4j; // Logging ekleyelim
import org.springframework.stereotype.Service;

import java.io.File; // Eklendi
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap; // Eklendi
import java.util.List;
import java.util.Map; // Eklendi
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j // Logging eklendi
public class JavaCodeExtractor {

    // Regex pattern'ları aynı kalabilir
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```java\\s*?(.*?)\\s*?```", Pattern.DOTALL);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\b(?:public|protected|private|abstract|final|static)?\\s*(?:class|interface|enum)\\s+([a-zA-Z0-9_]+)", Pattern.MULTILINE);

    /**
     * Verilen metin içerisindeki Java kod bloklarını ayrıştırır ve
     * dosya yolu (Path) -> kod içeriği (String) eşlemesini döndürür.
     * Dosyaları diske YAZMAZ.
     *
     * @param rawInput        Java kod bloklarını içeren tam metin.
     * @param outputBaseDir   Hedef dosyaların bulunacağı ana dizin (örn: "project/src/main/java" veya "project/src/test/java").
     *                        Bu dizin, paketin BAŞLAYACAĞI yer olmalı.
     * @return Anahtar olarak mutlak dosya yolu (Path), değer olarak kod içeriği (String) içeren bir Map.
     */
    public Map<Path, String> extractCodeToMap(String rawInput, String outputBaseDir) {
        Map<Path, String> codeMap = new HashMap<>();
        if (rawInput == null || rawInput.isBlank()) {
            log.warn("extractCodeToMap çağrıldı ancak girdi metni boş veya null.");
            return codeMap; // Boş girdi için boş map döndür
        }
        if (outputBaseDir == null || outputBaseDir.isBlank()){
            log.error("extractCodeToMap çağrıldı ancak outputBaseDir boş veya null. Dosya yolları hesaplanamaz.");
            return codeMap; // Hedef dizin olmadan devam edilemez
        }

        Matcher blockMatcher = CODE_BLOCK_PATTERN.matcher(rawInput);
        int blockCount = 0;
        int extractedCount = 0;

        while (blockMatcher.find()) {
            blockCount++;
            String codeContent = blockMatcher.group(1).trim();
            if (codeContent.isEmpty()) {
                log.debug("Boş kod bloğu bulundu, atlanıyor.");
                continue;
            }

            String packageName = extractPackageName(codeContent);
            String className = extractClassName(codeContent);

            if (packageName == null) {
                log.warn("Paket ismi bulunamadı. Bu kod bloğu atlanıyor (ilk 100 char): {}", codeContent.substring(0, Math.min(100, codeContent.length())).replace("\n", " "));
                continue;
            }
            if (className == null) {
                log.warn("Sınıf/Interface/Enum ismi bulunamadı (Paket: {}). Bu kod bloğu atlanıyor (ilk 100 char): {}", packageName, codeContent.substring(0, Math.min(100, codeContent.length())).replace("\n", " "));
                continue;
            }

            try {
                // Hedef dosya yolunu hesapla (outputBaseDir + package + className)
                Path packageAsSubPath = Paths.get(packageName.replace('.', File.separatorChar));
                // outputBaseDir'in "src/main/java" gibi bir şey olduğunu varsayıyoruz.
                Path targetFilePath = Paths.get(outputBaseDir).resolve(packageAsSubPath).resolve(className + ".java").toAbsolutePath();

                codeMap.put(targetFilePath, codeContent);
                extractedCount++;
                log.debug("Kod bloğu ayrıştırıldı ve haritaya eklendi: {}", targetFilePath);

            } catch (Exception e) {
                // Genellikle Paths.get ile ilgili bir sorun olursa (geçersiz karakter vb.)
                log.error("Dosya yolu hesaplanırken hata oluştu. Paket: {}, Sınıf: {}, Hata: {}", packageName, className, e.getMessage());
            }
        }

        if (blockCount > 0 && extractedCount == 0) {
            log.warn("{} adet kod bloğu bulundu ancak hiçbiri geçerli paket/sınıf adı içermediği için ayrıştırılamadı.", blockCount);
        } else if (blockCount == 0) {
            log.warn("Metin içinde '```java ... ```' formatında kod bloğu bulunamadı.");
        } else {
            log.info("{} adet kod bloğundan {} tanesi başarıyla ayrıştırıldı ve haritaya eklendi.", blockCount, extractedCount);
        }

        return codeMap;
    }

    // extractPackageName ve extractClassName metotları aynı kalabilir
    private String extractPackageName(String codeBlock) {
        Matcher matcher = PACKAGE_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractClassName(String codeBlock) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // --- extractAndSaveFiles Metodu ---
    // Bu metodu artık ProjectService kullanmıyor.
    // Eğer başka yerde kullanılmıyorsa kaldırılabilir.
    // Şimdilik burada bırakıyorum, karar size ait.
    /**
     * @deprecated ProjectService artık extractCodeToMap kullanıyor. Bu metot başka yerde kullanılmıyorsa kaldırılabilir.
     * Verilen metin içerisindeki Java kod bloklarını ayrıştırır ve
     * belirtilen ana dizin altına paket yapısına uygun olarak kaydeder.
     */
    @Deprecated
    public List<Path> extractAndSaveFiles(String rawInput, String outputBaseDir) throws IOException {
        // ... (Metodun eski içeriği burada) ...
        log.warn("Deprecated metot extractAndSaveFiles çağrıldı!");
        // ... (Eski kod) ...
        // ...
        Map<Path, String> codeMap = extractCodeToMap(rawInput, outputBaseDir);
        List<Path> savedFiles = new ArrayList<>();
        if (!codeMap.isEmpty()) {
            log.info("[Deprecated] {} adet kod bloğu bulundu, dosyalar yazılıyor...", codeMap.size());
            for(Map.Entry<Path, String> entry : codeMap.entrySet()) {
                Path outputFile = entry.getKey();
                String codeContent = entry.getValue();
                try {
                    Files.createDirectories(outputFile.getParent());
                    Files.writeString(outputFile, codeContent, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    log.info("[Deprecated] Kaydedildi: {}", outputFile.toAbsolutePath());
                    savedFiles.add(outputFile);
                } catch (IOException e) {
                    log.error("[Deprecated] Dosya yazma hatası: {}", outputFile, e);
                    throw e; // Hata durumunda dışarı fırlat
                }
            }
        } else {
            log.info("[Deprecated] Yazılacak geçerli kod bloğu bulunamadı.");
        }
        return savedFiles;
    }
}