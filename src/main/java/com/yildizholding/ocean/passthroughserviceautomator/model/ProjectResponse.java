package com.yildizholding.ocean.passthroughserviceautomator.model;

// Results modellerini import etmeye GEREK YOK (artık içinde tutmuyoruz)
// import com.yildizholding.ocean.passthroughserviceautomator.model.results.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProjectResponse {

    /** İşlemin genel başarı durumu. */
    private boolean overallSuccess;

    /** Kullanıcıya gösterilecek özet durum mesajı. */
    private String message;

    // --- Ana Çıktı Bilgileri ---
    /** Oluşturulan projenin adı. */
    private String projectName;

    /** Oluşturulan projenin ana paket adı. */
    private String packageName;

    /** Projenin oluşturulduğu yerel dosya yolu (Debug/Loglama için). */
    private String projectLocalPath;

    /** Kodun pushlandığı GitLab deposunun URL'si (Başarılıysa). */
    private String gitlabRepoUrl;

    /** Servisin Kong üzerinden erişileceği URL (Başarılıysa). */
    private String kongServiceUrl;

    /** GitLab üzerindeki README dosyasının URL'si (Başarılıysa). */
    private String gitlabReadmeUrl;

    /** GitLab üzerindeki Postman Collection dosyasının URL'si (Başarılıysa). */
    private String gitlabPostmanUrl;
    // -----------------------------

    /** Süreçte kritik bir hata oluştuysa, teknik hata detayı (opsiyonel). */
    private String errorDetails;

}