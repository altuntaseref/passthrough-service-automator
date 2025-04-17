package com.yildizholding.ocean.passthroughserviceautomator.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class JavaCodeExtractor {

    // Regex pattern'ları
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```java\\s*?(.*?)\\s*?```", Pattern.DOTALL);
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([a-zA-Z0-9_.]+);", Pattern.MULTILINE);
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\b(?:public|protected|private|abstract|final|static)?\\s*(?:class|interface|enum)\\s+([a-zA-Z0-9_]+)", Pattern.MULTILINE);

    /**
     * Verilen metin içerisindeki Java kod bloklarını ayrıştırır ve
     * dosya yolu (Path) -> kod içeriği (String) eşlemesini döndürür.
     * Döndürülen Path'ler outputBaseDir'e göre hesaplanır ve KÖK proje dizinine
     * göre OLACAĞINDAN ProjectService tarafından yeniden hesaplanmalıdır.
     * Dosyaları diske YAZMAZ.
     *
     * @param rawInput        Java kod bloklarını içeren tam metin.
     * @param outputBaseDir   Dosya yollarının hesaplanacağı temel dizin (Genellikle projenin kök dizini).
     * @return Anahtar olarak (yanlış olabilecek) mutlak dosya yolu (Path), değer olarak kod içeriği (String) içeren bir Map.
     */
    public Map<Path, String> extractCodeToMap(String rawInput, String outputBaseDir) {
        Map<Path, String> codeMap = new HashMap<>();
        if (rawInput == null || rawInput.isBlank()) {
            log.warn("extractCodeToMap çağrıldı ancak girdi metni boş veya null.");
            return codeMap;
        }
        if (outputBaseDir == null || outputBaseDir.isBlank()){
            log.error("extractCodeToMap çağrıldı ancak outputBaseDir boş veya null. Dosya yolları hesaplanamaz.");
            return codeMap;
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

            String packageName = extractPackageNameInternal(codeContent); // İç yardımcıyı kullan
            String className = extractClassNameInternal(codeContent);     // İç yardımcıyı kullan

            if (packageName == null || className == null) {
                // Hata logları iç yardımcılarda yapılıyor
                continue;
            }

            try {
                // Paket yolunu oluştur (işletim sisteminden bağımsız)
                String packageAsPathString = packageName.replace('.', File.separatorChar);
                // outputBaseDir'e göre yolu hesapla (Bu yol ProjectService tarafından düzeltilecek)
                Path targetFilePath = Paths.get(outputBaseDir).resolve(packageAsPathString).resolve(className + ".java").toAbsolutePath();

                codeMap.put(targetFilePath, codeContent);
                extractedCount++;
                log.debug("Kod bloğu ayrıştırıldı ve haritaya eklendi (geçici yol): {}", targetFilePath);

            } catch (Exception e) {
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

    // --- ProjectService tarafından kullanılacak PUBLIC yardımcı metotlar ---

    /**
     * Verilen kod bloğundan paket adını çıkarır.
     * @param codeBlock Java kodunu içeren String.
     * @return Paket adı veya bulunamazsa null.
     */
    public String extractPackageName(String codeBlock) {
        return extractPackageNameInternal(codeBlock);
    }

    /**
     * Verilen kod bloğundan ana sınıf/interface/enum adını çıkarır.
     * @param codeBlock Java kodunu içeren String.
     * @return Sınıf adı veya bulunamazsa null.
     */
    public String extractClassName(String codeBlock) {
        return extractClassNameInternal(codeBlock);
    }

    // --- Dahili (private) yardımcı metotlar ---

    private String extractPackageNameInternal(String codeBlock) {
        if (codeBlock == null || codeBlock.isBlank()) return null;
        Matcher matcher = PACKAGE_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("Paket ismi bulunamadı. Kod bloğu (ilk 100 char): {}", codeBlock.substring(0, Math.min(100, codeBlock.length())).replace("\n", " "));
        return null;
    }

    private String extractClassNameInternal(String codeBlock) {
        if (codeBlock == null || codeBlock.isBlank()) return null;
        Matcher matcher = CLASS_NAME_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("Sınıf/Interface/Enum ismi bulunamadı. Kod bloğu (ilk 100 char): {}", codeBlock.substring(0, Math.min(100, codeBlock.length())).replace("\n", " "));
        return null;
    }


    /**
     * @deprecated ProjectService artık extractCodeToMap ve public helper metotları kullanıyor.
     * Bu metot başka yerde kullanılmıyorsa kaldırılabilir.
     */
    @Deprecated
    public List<Path> extractAndSaveFiles(String rawInput, String outputBaseDir) throws IOException {
        log.warn("Deprecated metot extractAndSaveFiles çağrıldı!");
        Map<Path, String> codeMap = extractCodeToMap(rawInput, outputBaseDir);
        List<Path> savedFiles = new ArrayList<>();
        if (!codeMap.isEmpty()) {
            log.info("[Deprecated] {} adet kod bloğu bulundu, dosyalar yazılıyor...", codeMap.size());
            for(Map.Entry<Path, String> entry : codeMap.entrySet()) {
                Path outputFile = entry.getKey();
                String codeContent = entry.getValue();
                try {
                    // Bu eski metot da dizin oluşturmalıydı
                    Path parentDir = outputFile.getParent();
                    if(parentDir != null) Files.createDirectories(parentDir);

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