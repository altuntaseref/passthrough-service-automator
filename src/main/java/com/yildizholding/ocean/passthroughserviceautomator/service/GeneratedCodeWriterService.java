package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.util.JavaCodeExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratedCodeWriterService {

    private final JavaCodeExtractor javaCodeExtractor; // Paket/Sınıf adı çıkarmak için

    /**
     * Verilen kod haritasındaki dosyaları DOĞRU projenin kaynak/test dizinlerine yazar.
     */
    public void writeCodeFiles(Map<Path, String> codeMap, RestProjectRequest request) {
        if (request == null) {
            log.error("GeneratedCodeWriterService: RestProjectRequest null.");
            return;
        }
        String mainJavaPath = request.generateProjectSrcMain();
        String testJavaPath = request.generateProjectSrcTest();

        if (mainJavaPath == null || testJavaPath == null) {
            log.error("Proje ana veya test kaynak yolları null.");
            return;
        }
        Path mainSrcJavaBase = Paths.get(mainJavaPath, "java");
        Path testSrcJavaBase = Paths.get(testJavaPath, "java");

        log.info("{} adet ayrıştırılmış kod dosyası doğru konumlara yazılacak...", codeMap.size());
        int successCount = 0;
        int errorCount = 0;

        for (Map.Entry<Path, String> entry : codeMap.entrySet()) {
            String codeContent = entry.getValue();
            Path targetPath = null;

            try {
                String packageName = javaCodeExtractor.extractPackageName(codeContent);
                String className = javaCodeExtractor.extractClassName(codeContent);

                if (packageName == null || className == null) {
                    log.warn("Kod bloğundan paket/sınıf adı çıkarılamadı, atlanıyor.");
                    errorCount++;
                    continue;
                }

                boolean isTestClass = className.endsWith("Test");
                Path correctBaseDir = isTestClass ? testSrcJavaBase : mainSrcJavaBase;

                Path packageAsSubPath = Paths.get(packageName.replace('.', File.separatorChar));
                targetPath = correctBaseDir.resolve(packageAsSubPath).resolve(className + ".java").normalize();
                log.debug("Hesaplanan doğru hedef yol: {}", targetPath);

                Path parentDir = targetPath.getParent();
                if(parentDir != null) Files.createDirectories(parentDir);

                Files.writeString(targetPath, codeContent, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                log.info("Dosya başarıyla yazıldı/güncellendi: {}", targetPath);
                successCount++;

            } catch (IOException | InvalidPathException e) {
                log.error("Dosya yazılamadı veya geçersiz yol ({}) : {}", targetPath, e.getMessage());
                errorCount++;
            } catch (Exception e) {
                log.error("Dosya yazılırken beklenmedik hata ({}) : {}", targetPath, e.getMessage(), e);
                errorCount++;
            }
        }
        log.info("Dosya yazma işlemi tamamlandı. Başarılı: {}, Hatalı: {}", successCount, errorCount);
    }
}