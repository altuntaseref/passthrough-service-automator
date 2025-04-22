package com.yildizholding.ocean.passthroughserviceautomator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.yildizholding.ocean.passthroughserviceautomator.model.RestProjectRequest;
import com.yildizholding.ocean.passthroughserviceautomator.model.kong.OceanServiceRegisterResponseModel;
import com.yildizholding.ocean.passthroughserviceautomator.model.results.DocumentationGenerationResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentationGeneratorService {

    private final ResourceReaderService resourceReaderService; // Controller dosyasını okumak için
    private final ObjectMapper objectMapper; // JSON oluşturmak için (pretty print açık olmalı)

    public static final String README_FILENAME = "README.md";
    public static final String POSTMAN_COLLECTION_FILENAME = "postman_collection.json"; // Sabit dosya adı

    /**
     * Controller'ı tarar, Readme ve Postman Collection oluşturur, projenin kök dizinine yazar
     * ve işlemin sonucunu döndürür.
     *
     * @param request      Automator isteği.
     * @param kongResponse Kong API'sinden dönen başarılı yanıt (null olabilir).
     * @param projectPath  Projenin yerel dosya yolu.
     * @return Dokümantasyon oluşturma işleminin sonucunu içeren DocumentationGenerationResult nesnesi.
     */
    public DocumentationGenerationResult generateDocumentationFiles(RestProjectRequest request, OceanServiceRegisterResponseModel kongResponse, String projectPath) {
        log.info("Dokümantasyon dosyaları oluşturuluyor ve yazılıyor: Proje={}", request.getProjectName());
        if (projectPath == null) {
            log.error("Proje yolu null, dokümantasyon dosyaları oluşturulamaz.");
            return DocumentationGenerationResult.failure("Proje yolu sağlanmadı.");
        }

        List<String> generatedFiles = new ArrayList<>(); // Göreli yolları tutacak liste

        try {
            // 1. Controller içeriğini oku
            Optional<String> controllerContentOpt = resourceReaderService.readGeneratedTemplateContent(request, "controller");
            if (controllerContentOpt.isEmpty()) {
                log.error("Controller dosyası okunamadı.");
                // Controller olmadan endpoint çıkaramayız, ama belki boş dokümanlar oluşturulabilir?
                // Şimdilik hata olarak kabul edelim.
                return DocumentationGenerationResult.failure("Controller dosyası okunamadı.");
            }
            String controllerContent = controllerContentOpt.get();

            // 2. Endpoint bilgilerini çıkar
            List<EndpointInfo> endpoints = parseControllerForEndpoints(controllerContent);
            if (endpoints.isEmpty()) {
                log.warn("Controller'dan endpoint bilgisi çıkarılamadı.");
            }

            // 3. Readme içeriğini oluştur
            String readmeContent = buildReadmeContent(request, kongResponse, endpoints);

            // 4. Postman Collection JSON içeriğini oluştur
            String postmanJsonContent = buildPostmanCollection(request, kongResponse, endpoints);

            // 5. Dosyaları proje kök dizinine yaz
            Path readmePath = Paths.get(projectPath, README_FILENAME);
            Path postmanPath = Paths.get(projectPath, POSTMAN_COLLECTION_FILENAME);

            writeToFile(readmePath, readmeContent);
            generatedFiles.add(README_FILENAME); // Göreli yolu ekle
            log.info("README.md dosyası başarıyla oluşturuldu/güncellendi: {}", readmePath);

            writeToFile(postmanPath, postmanJsonContent);
            generatedFiles.add(POSTMAN_COLLECTION_FILENAME); // Göreli yolu ekle
            log.info("Postman Collection dosyası başarıyla oluşturuldu/güncellendi: {}", postmanPath);

            // Başarılı sonuç nesnesini oluştur ve dön
            return DocumentationGenerationResult.success(generatedFiles);

        } catch (IOException e) {
            log.error("Dokümantasyon dosyaları yazılırken G/Ç hatası oluştu.", e);
            return DocumentationGenerationResult.failure("Dosya yazma hatası: " + e.getMessage());
        } catch (Exception e) {
            log.error("Dokümantasyon oluşturma sırasında beklenmedik hata oluştu.", e);
            return DocumentationGenerationResult.failure("Beklenmedik hata: " + e.getMessage());
        }
    }



    /**
     * Controller'ı tarar, Readme ve Postman Collection oluşturur ve projenin kök dizinine yazar.
     *
     * @param request      Automator isteği.
     * @param kongResponse Kong API'sinden dönen başarılı yanıt (credential'lar için). Kong kaydı yapılmadıysa veya başarısızsa null olabilir.
     * @param projectPath  Projenin yerel dosya yolu.
     * @return Oluşturulan doküman dosyalarının proje köküne göreli yollarını içeren liste. Hata durumunda boş liste döner.
     */
    public List<String> generateDocumentation(RestProjectRequest request, OceanServiceRegisterResponseModel kongResponse, String projectPath) {
        log.info("Dokümantasyon oluşturma başlatılıyor: Proje={}", request.getProjectName());
        if (projectPath == null) {
            log.error("Proje yolu null, dokümantasyon oluşturulamaz.");
            return List.of();
        }

        List<String> generatedFiles = new ArrayList<>();

        try {
            // 1. Controller dosyasının içeriğini oku
            Optional<String> controllerContentOpt = resourceReaderService.readGeneratedTemplateContent(request, "controller");
            if (controllerContentOpt.isEmpty()) {
                log.error("Dokümantasyon oluşturma için Controller dosyası okunamadı.");
                return List.of(); // Controller olmadan endpoint çıkaramayız
            }
            String controllerContent = controllerContentOpt.get();

            // 2. Controller'ı ayrıştırarak endpoint bilgilerini çıkar
            List<EndpointInfo> endpoints = parseControllerForEndpoints(controllerContent);
            if (endpoints.isEmpty()) {
                log.warn("Controller'dan endpoint bilgisi çıkarılamadı, dokümantasyon eksik olabilir.");
            }

            // 3. Readme içeriğini oluştur
            String readmeContent = buildReadmeContent(request, kongResponse, endpoints);

            // 4. Postman Collection JSON içeriğini oluştur
            String postmanJsonContent = buildPostmanCollection(request, kongResponse, endpoints);

            // 5. Dosyaları proje kök dizinine yaz
            Path readmePath = Paths.get(projectPath, README_FILENAME);
            Path postmanPath = Paths.get(projectPath, POSTMAN_COLLECTION_FILENAME);

            writeToFile(readmePath, readmeContent);
            generatedFiles.add(README_FILENAME); // Göreli yolu ekle
            log.info("README.md dosyası başarıyla oluşturuldu/güncellendi: {}", readmePath);

            writeToFile(postmanPath, postmanJsonContent);
            generatedFiles.add(POSTMAN_COLLECTION_FILENAME); // Göreli yolu ekle
            log.info("Postman Collection dosyası başarıyla oluşturuldu/güncellendi: {}", postmanPath);

        } catch (IOException e) {
            log.error("Dokümantasyon dosyaları yazılırken G/Ç hatası oluştu.", e);
            // Hata durumunda boş liste dönebiliriz veya exception fırlatabiliriz
            return List.of();
        } catch (Exception e) {
            log.error("Dokümantasyon oluşturma sırasında beklenmedik hata oluştu.", e);
            return List.of();
        }
        return generatedFiles; // Başarılıysa oluşturulan dosyaların listesi
    }


    // --- Özel Yardımcı Metotlar ---

    /** Controller kodunu JavaParser ile ayrıştırır ve endpoint bilgilerini çıkarır. */
    private List<EndpointInfo> parseControllerForEndpoints(String code) {
        List<EndpointInfo> endpoints = new ArrayList<>();
        try {
            CompilationUnit cu = StaticJavaParser.parse(code);
            AtomicReference<String> classLevelPath = new AtomicReference<>("");

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
                // Sınıf seviyesi @RequestMapping
                c.getAnnotationByName("RequestMapping").ifPresent(an ->
                    extractPathFromAnnotation(an).ifPresent(p -> classLevelPath.set(p))
                );
                // Metotları işle
                c.getMethods().forEach(m ->
                    extractEndpointInfoFromMethod(m, classLevelPath.get()).ifPresent(endpoints::add)
                );
            });
        } catch (Exception e) {
            log.error("JavaParser ile Controller ayrıştırılırken hata oluştu. Endpoint listesi boş olabilir.", e);
        }
        return endpoints;
    }

    /** Bir metottan endpoint bilgilerini çıkarır. */
    private Optional<EndpointInfo> extractEndpointInfoFromMethod(MethodDeclaration method, String classLevelPath) {
        AtomicReference<String> httpMethod = new AtomicReference<>("GET"); // Varsayılan
        AtomicReference<String> methodPath = new AtomicReference<>("");
        AtomicReference<Boolean> isEndpoint = new AtomicReference<>(false);

        // Anotasyonları kontrol et ve bilgileri çıkar
        checkMappingAnnotation(method, "PostMapping", "POST", httpMethod, methodPath, isEndpoint);
        checkMappingAnnotation(method, "GetMapping", "GET", httpMethod, methodPath, isEndpoint);
        checkMappingAnnotation(method, "PutMapping", "PUT", httpMethod, methodPath, isEndpoint);
        checkMappingAnnotation(method, "DeleteMapping", "DELETE", httpMethod, methodPath, isEndpoint);
        checkMappingAnnotation(method, "RequestMapping", httpMethod.get(), httpMethod, methodPath, isEndpoint); // RequestMapping için özel kontrol gerekebilir

        if (!isEndpoint.get()) {
            return Optional.empty(); // Endpoint metodu değil
        }

        String fullPath = buildFullPath(classLevelPath, methodPath.get());
        // TODO: Metod parametrelerini (RequestBody, RequestParam, PathVariable) ayrıştırıp EndpointInfo'ya ekle
        return Optional.of(new EndpointInfo(method.getNameAsString(), httpMethod.get(), fullPath));
    }

    /** Belirli bir mapping anotasyonunu kontrol eder ve bilgileri çıkarır. */
    private void checkMappingAnnotation(MethodDeclaration method, String annotationName, String defaultHttpMethod,
                                        AtomicReference<String> httpMethod, AtomicReference<String> methodPath, AtomicReference<Boolean> isEndpoint) {
        method.getAnnotationByName(annotationName).ifPresent(an -> {
            isEndpoint.set(true);
            httpMethod.set(defaultHttpMethod); // Varsayılanı ayarla (RequestMapping için override edilebilir)
            extractPathFromAnnotation(an).ifPresent(methodPath::set);

            // RequestMapping için metodu da oku (varsa)
            if ("RequestMapping".equals(annotationName) && an instanceof NormalAnnotationExpr) {
                ((NormalAnnotationExpr) an).getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("method"))
                    .findFirst()
                    .ifPresent(p -> {
                        String methodVal = p.getValue().toString(); // Örn: RequestMethod.POST
                        if (methodVal.contains(".")) { // Enum referansı ise
                             httpMethod.set(methodVal.substring(methodVal.lastIndexOf('.') + 1));
                        } else {
                             // Direkt isim verilmişse (nadiren kullanılır)
                             httpMethod.set(methodVal.replace("\"", ""));
                        }
                    });
            }
        });
    }


    /** @XxxMapping anotasyonundan path değerini çıkarır. */
    private Optional<String> extractPathFromAnnotation(AnnotationExpr annotation) {
        // Hem @Mapping("/path") hem de @Mapping(value="/path") veya @Mapping(path="/path") destekler
        if (annotation instanceof SingleMemberAnnotationExpr) {
            return Optional.ofNullable(((SingleMemberAnnotationExpr) annotation).getMemberValue())
                           .map(expr -> expr.toString().replace("\"", ""));
        } else if (annotation instanceof NormalAnnotationExpr) {
            return ((NormalAnnotationExpr) annotation).getPairs().stream()
                    .filter(pair -> pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path"))
                    .findFirst()
                    .map(pair -> pair.getValue().toString().replace("\"", ""));
        }
        return Optional.empty();
    }

    /** Sınıf ve metot path'lerini birleştirir. */
    private String buildFullPath(String classPath, String methodPath) {
        String p1 = classPath.replaceAll("^/|/$", "");
        String p2 = methodPath.replaceAll("^/|/$", "");
        String full = "/" + (p1.isEmpty() ? "" : p1 + "/") + p2;
        return full.equals("/") && (!p1.isEmpty() || !p2.isEmpty()) ? full : full.replaceAll("/+", "/"); // Çift slash düzelt
    }

    /** Readme içeriğini oluşturur. */
    private String buildReadmeContent(RestProjectRequest request, OceanServiceRegisterResponseModel kong, List<EndpointInfo> endpoints) {
        StringBuilder md = new StringBuilder();
        md.append("# ").append(request.getProjectName()).append(" Service\n\n");
        md.append("Bu servis, otomatik olarak oluşturulmuş bir passthrough veya entegrasyon servisidir.\n\n"); // Daha iyi açıklama eklenebilir

        md.append("## API Endpoint Bilgileri\n\n");
        if (kong != null && "Success".equalsIgnoreCase(kong.getResult())) {
            md.append("**Kong Gateway Üzerinden Erişim:**\n");
            md.append("- **Base URL:** `").append(kong.getServiceLink()).append("`\n");

                md.append("- **Kimlik Doğrulama:** Basic Auth\n");
                md.append("  - **Username:** `").append(kong.getServiceUsername()).append("`\n");
                md.append("  - **Password:** `").append(kong.getServicePassword()).append("` (Bu bilgiyi güvenli tutun!)\n");

        } else {
            md.append("**API Erişimi:**\n");
            md.append("- Kong kaydı başarısız oldu veya yapılmadı.\n");
            md.append("- Servise doğrudan erişim gerekebilir (konuşlandırma detaylarına bakın).\n");
        }
        md.append("\n");

        md.append("### Mevcut Endpointler:\n\n");
        if (endpoints.isEmpty()) {
            md.append("- Henüz endpoint tanımlanmamış veya ayrıştırılamadı.\n");
        } else {
            md.append("| Metot | Path                       | Açıklama (Metot Adı) |\n");
            md.append("|-------|----------------------------|----------------------|\n");
            for (EndpointInfo ep : endpoints) {
                md.append("| `").append(ep.getHttpMethod()).append("` ");
                md.append("| `").append(ep.getFullPath()).append("` ");
                md.append("| ").append(ep.getMethodName()).append(" |\n");
                // TODO: Parametre detayları (RequestParam, PathVariable, RequestBody) eklenebilir
            }
        }
        md.append("\n");

        md.append("## Kurulum ve Çalıştırma\n\n");
        md.append("```bash\n");
        md.append("# Bağımlılıkları yükle\nmvn clean install\n\n");
        md.append("# Uygulamayı çalıştır\njava -jar target/").append(request.getProjectName()).append("-*.jar\n");
        md.append("```\n\n");

        md.append("## Postman Collection\n\n");
        md.append("Projenin kök dizininde bulunan `").append(POSTMAN_COLLECTION_FILENAME).append("` dosyasını Postman'e import edebilirsiniz.\n");

        return md.toString();
    }

    /** Postman Collection v2.1 JSON içeriğini oluşturur. */
    private String buildPostmanCollection(RestProjectRequest request, OceanServiceRegisterResponseModel kong, List<EndpointInfo> endpoints) throws IOException {
        ObjectNode collection = objectMapper.createObjectNode();
        // ... (Info, Auth, Item blocklarını doldurma - önceki yanıttaki gibi) ...
        // ... (Info block) ...
        ObjectNode info = collection.putObject("info");
        info.put("_postman_id", UUID.randomUUID().toString());
        info.put("name", request.getProjectName() + " API Collection");
        info.put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json");

        // ... (Auth block - önceki yanıttaki gibi) ...
        ObjectNode auth = collection.putObject("auth");
        if (kong != null  && "Success".equalsIgnoreCase(kong.getResult())) {
            auth.put("type", "basic");
            ArrayNode basicAuthArray = auth.putArray("basic");
            basicAuthArray.addObject().put("key", "password").put("value", kong.getServicePassword()).put("type", "string");
            basicAuthArray.addObject().put("key", "username").put("value", kong.getServiceUsername()).put("type", "string");
        } else {
            auth.put("type", "noauth");
        }
        // ... (Item block - endpoint döngüsü - önceki yanıttaki gibi) ...
        ArrayNode itemArray = collection.putArray("item");
        String baseUrl = (kong != null && "Success".equalsIgnoreCase(kong.getResult()))
                ? kong.getServiceLink()
                : "{{baseUrl}}"; // Kong yoksa değişken kullan

        for (EndpointInfo ep : endpoints) {
            ObjectNode requestItem = itemArray.addObject();
            // ... (requestItem içeriğini doldur - önceki yanıttaki gibi) ...
            requestItem.put("name", ep.getMethodName() + " (" + ep.getFullPath() + ")");
            ObjectNode requestNode = requestItem.putObject("request");
            requestNode.put("method", ep.getHttpMethod());
            // ... (Header, URL, Body - önceki yanıttaki gibi) ...
            requestItem.putArray("response"); // Boş yanıt
        }

        // ... (Variables block - önceki yanıttaki gibi) ...
        if ("{{baseUrl}}".equals(baseUrl)) {
            collection.putArray("variable").addObject()
                    .put("key", "baseUrl")
                    .put("value", "http://localhost:8080") // Varsayılan değer
                    .put("type", "string");
        }

        return objectMapper.writeValueAsString(collection); // JSON string olarak döndür
    }

    /** Dosyaya içerik yazar. */
    private void writeToFile(Path path, String content) throws IOException {
        try {
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8,
                              StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("Dosya yazılamadı: {}", path, e);
            throw e;
        }
    }

    // Endpoint bilgilerini tutmak için basit bir iç sınıf
    @Data // Lombok getter/setter/toString vb. için
    @AllArgsConstructor // Constructor için
    private static class EndpointInfo {
        private String methodName;
        private String httpMethod;
        private String fullPath;
        // TODO: Gelecekte parametre listesi eklenebilir (List<ParameterInfo>)
    }

    @Data
    @AllArgsConstructor
    public static class DocumentationResult {
        private String readmeContent;
        private String postmanCollectionJson;
    }
}

