# Passthrough Service Automator

## Genel Bakış

**Passthrough Service Automator**, verilen bir Postman Collection ve temel proje bilgileriyle, otomatik olarak Spring Boot tabanlı bir REST servis projesi oluşturan, dokümantasyonunu ve Postman Collection'ını üreten, isteğe bağlı olarak Kong API Gateway'e kaydeden ve GitLab'a push eden bir otomasyon sistemidir. Ayrıca LLM (Large Language Model) desteğiyle kodun otomatik olarak geliştirilmesini sağlar.

## Özellikler

- **Otomatik Proje Oluşturma:** Postman Collection ve proje bilgileriyle, Spring Boot projesi iskeleti ve kodları otomatik oluşturulur.
- **LLM Destekli Kod Geliştirme:** Gemini API (veya OpenAI uyumlu) ile prompt tabanlı kod üretimi.
- **Kong API Gateway Entegrasyonu:** Oluşturulan servis otomatik olarak Kong'a kaydedilebilir.
- **Otomatik Dokümantasyon:** README.md ve Postman Collection JSON dosyası otomatik üretilir.
- **GitLab Entegrasyonu:** Oluşturulan proje ve dokümantasyon, belirlenen GitLab reposuna otomatik push edilir.
- **Swagger/OpenAPI Desteği:** Otomatik API dokümantasyonu için Springdoc entegrasyonu.

## Kullanılan Teknolojiler

- **Java 17**
- **Spring Boot 3.x**
- **Spring AI (LLM entegrasyonu için)**
- **Freemarker & Thymeleaf (şablonlama)**
- **OpenAPI/Swagger**
- **Kong API Gateway**
- **GitLab (JGit ile otomasyon)**
- **Postman Collection desteği**
- **Lombok, Jackson, Unirest, JavaParser**

## Kurulum

### Gereksinimler

- Java 17+
- Maven 3.8+
- GitLab erişimi (Personal Access Token gereklidir)
- Kong API Gateway (opsiyonel)
- Gemini veya OpenAI API anahtarı

### Yapılandırma

`src/main/resources/application.properties` ve `project-config.yaml` dosyalarını ihtiyacınıza göre düzenleyin:

```properties
spring.application.name=passthrough-service-automator
spring.ai.openai.chat.base-url=https://generativelanguage.googleapis.com
spring.ai.openai.api-key=YOUR_GEMINI_API_KEY
spring.ai.openai.chat.options.model=gemini-2.0-flash

spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

```yaml
projectName: demo-project
groupId: com.yildizholding.ocean
artifactId: demo-project
packageName: com.example.demo
dependencies:
  - web
  - data-jpa
javaVersion: 17
springBootVersion: 3.4.4
outputPath: C:\Users\seraf\Desktop\passthroguh-projects\
```

Ek olarak, Kong ve GitLab için gerekli environment değişkenlerini veya application.properties ayarlarını ekleyin:

```properties
kong.register.api.url=KONG_REGISTER_API_URL
kong.base.domain=KONG_BASE_DOMAIN
kong.simulate.success=true # Test için simülasyon
gitlab.pat=YOUR_GITLAB_PERSONAL_ACCESS_TOKEN
```

## Kullanım

### 1. Proje Oluşturma API'si

#### Endpoint

```
POST /api/automate/generate
Content-Type: multipart/form-data
```

#### Parametreler

- `request`: JSON formatında proje bilgileri (`RestProjectRequest`)
- `postmanFile`: Postman Collection dosyası (JSON)

#### Örnek `RestProjectRequest` JSON

```json
{
  "projectName": "ornek-proje",
  "systemName": "ornek-sistem",
  "baseUrl": "https://ornek.api.com",
  "username": "kullanici",
  "password": "sifre",
  "apiKey": "apikey",
  "sslCerIsRequired": false,
  "gitlabRepoUrl": "https://gitlab.com/kullanici/ornek-proje.git",
  "registerOnKong": true
}
```

#### Yanıt

Başarılıysa, proje yolu, mesaj, Kong ve GitLab linkleri gibi bilgiler döner.

### 2. LLM Prompt API'si

```
POST /api/automate/promt
Content-Type: text/plain
```

Serbest bir prompt ile LLM'den kod üretimi tetiklenebilir.

## Otomasyon Akışı

1. **Kullanıcıdan Proje Bilgisi ve Postman Collection alınır.**
2. **Proje iskeleti ve kodları otomatik oluşturulur.**
3. **Kodlar LLM ile zenginleştirilir (isteğe bağlı).**
4. **Kong API Gateway'e servis kaydı yapılır (isteğe bağlı).**
5. **README.md ve Postman Collection otomatik üretilir.**
6. **Tüm proje ve dokümantasyon GitLab'a push edilir (isteğe bağlı).**

## Geliştirici Notları

- Kod üretiminde kullanılan prompt şablonu: `src/main/resources/templates/prompts/llm_code_generation_prompt.ftl`
- Proje şablonları ve referans dosyalar: `src/main/resources/templates/`
- Tüm ana iş akışı `ProjectServie.java` ve ilgili servislerde yönetilir.
- Kong ve GitLab entegrasyonları için ilgili servislerde environment/config ayarlarını kontrol edin.

## Katkı ve Lisans

Bu proje Yıldız Holding için özel olarak geliştirilmiştir. Katkı sağlamak için lütfen proje yöneticinizle iletişime geçin. 