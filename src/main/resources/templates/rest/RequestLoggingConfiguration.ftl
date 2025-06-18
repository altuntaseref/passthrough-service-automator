package ${packageName}.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* Tüm loglama konfigürasyonu ve bileşenlerini içeren sınıf.
* Bu sınıf şunları içerir:
* 1. RequestCachingFilter - İstek ve yanıt gövdelerinin okunabilmesi için önbelleğe alır
* 2. IncomingRequestLoggingInterceptor - Gelen HTTP isteklerini ve yanıtları loglar
* 3. RestTemplateLoggingInterceptor - RestTemplate ile yapılan dış servis çağrılarını loglar
* 4. WebMvcConfigurer yapılandırması - Interceptor'ların eklenmesi
*/
@Configuration
public class RequestLoggingConfiguration implements WebMvcConfigurer {

private static final Logger log = LoggerFactory.getLogger(RequestLoggingConfiguration.class);
private static final String REQUEST_ID_ATTRIBUTE = "requestId";

/**
* Request ve Response gövdelerinin birden fazla kez okunabilmesi için önbelleğe alan filtre.
*/
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCachingFilter implements Filter {
@Override
public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
throws IOException, ServletException {
ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper((HttpServletRequest) request);
ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper((HttpServletResponse) response);

chain.doFilter(wrappedRequest, wrappedResponse);

// Response body'sini kopyala (önemli - aksi takdirde yanıt boş olur)
wrappedResponse.copyBodyToResponse();
}
}

/**
* Gelen HTTP isteklerini ve yanıtlarını loglayan interceptor.
*/
@Component
public class IncomingRequestLoggingInterceptor implements HandlerInterceptor {
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
// İstekleri takip etmek için benzersiz bir ID oluştur
String requestId = UUID.randomUUID().toString();
request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);

if (request instanceof ContentCachingRequestWrapper) {
logRequest((ContentCachingRequestWrapper) request, requestId);
} else {
log.warn("[{}] Gelen İstek: Wrapper bulunamadı. Method: {}, URI: {}",
requestId, request.getMethod(), request.getRequestURI());
}
return true;
}

@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
String requestId = (String) request.getAttribute(REQUEST_ID_ATTRIBUTE);

if (response instanceof ContentCachingResponseWrapper) {
logResponse((ContentCachingResponseWrapper) response, requestId);
} else {
log.warn("[{}] Giden Yanıt: Wrapper bulunamadı. Status: {}",
requestId, response.getStatus());
}

if (ex != null) {
log.error("[{}] İstek işlenirken hata oluştu: ", requestId, ex);
}
}

private void logRequest(ContentCachingRequestWrapper request, String requestId) {
String requestBody = getContentAsString(request.getContentAsByteArray(), request.getCharacterEncoding());

StringBuilder requestString = new StringBuilder("External Request >> ");
requestString.append(request.getRequestURI())
.append(", Method: ").append(request.getMethod())
.append(", Headers: ").append(getRequestHeaders(request))
.append(", Remote Addr: ").append(request.getRemoteAddr())
.append(", RequestId:[").append(requestId).append("]");

if (!requestBody.isEmpty()) {
requestString.append(", Body: ").append(requestBody);
}

log.info(requestString.toString());
}

private void logResponse(ContentCachingResponseWrapper response, String requestId) {
String responseBody = getContentAsString(response.getContentAsByteArray(), response.getCharacterEncoding());

StringBuilder responseString = new StringBuilder("External Response << ");
responseString.append("Status: ").append(response.getStatus())
.append(", Headers: ").append(getResponseHeaders(response))
.append(", RequestId:[").append(requestId).append("] << ");

if (!responseBody.isEmpty()) {
responseString.append(", Body: ").append(responseBody);
}

log.info(responseString.toString());
}

private String getRequestHeaders(HttpServletRequest request) {
return request.getHeaderNames() != null ?
Collections.list(request.getHeaderNames()).stream()
.map(headerName -> headerName + "=[" + Collections.list(request.getHeaders(headerName)).stream().collect(Collectors.joining(", ")) + "]")
.collect(Collectors.joining(", ")) : "";
}

private String getResponseHeaders(HttpServletResponse response) {
return response.getHeaderNames().stream()
.map(headerName -> headerName + "=[" + response.getHeaders(headerName).stream().collect(Collectors.joining(", ")) + "]")
.collect(Collectors.joining(", "));
}
}

/**
* RestTemplate ile yapılan dış servis çağrılarını loglayan interceptor.
*/
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {
@Override
public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
// Mevcut thread'deki istek ID'sini almaya çalış (MDC veya ThreadLocal kullanılabilir)
String requestId = getCurrentRequestId();

logOutgoingRequest(request, body, requestId);
ClientHttpResponse response = execution.execute(request, body);
logOutgoingResponse(response, requestId);

return response;
}

private void logOutgoingRequest(HttpRequest request, byte[] body, String requestId) {
StringBuilder responseString = new StringBuilder("Internal Request >> ");
responseString.append(request.getURI()).append(", Method:").append(request.getMethod())
.append(", Headers:").append(request.getHeaders())
.append(", RequestId:[").append(requestId).append("]");

if (body != null && body.length > 0) {
responseString.append(", Body:").append(new String(body, StandardCharsets.UTF_8));
}

log.info(responseString.toString());
}

private void logOutgoingResponse(ClientHttpResponse response, String requestId) throws IOException {
StringBuilder responseString = new StringBuilder("Internal Response << ");
responseString.append("Status: ").append(response.getStatusCode())
.append(", Headers:").append(response.getHeaders())
.append(", RequestId:[").append(requestId).append("]");

// Body'yi oku - BufferingClientHttpRequestFactory kullandığımız için stream tüketilmeyecek
String charset = response.getHeaders().getContentType() != null &&
response.getHeaders().getContentType().getCharset() != null ?
response.getHeaders().getContentType().getCharset().name() :
StandardCharsets.UTF_8.name();

try {
byte[] bodyBytes = StreamUtils.copyToByteArray(response.getBody());
if (bodyBytes.length > 0) {
String bodyContent = new String(bodyBytes, charset);
responseString.append(", Body:").append(bodyContent);
}
} catch (Exception e) {
log.warn("[{}] Response body okunamadı: {}", requestId, e.getMessage());
}

log.info(responseString.toString());
}
}

/**
* Mevcut HTTP isteğine ait requestId'yi döndürür.
* Gerçek uygulamada MDC veya ThreadLocal kullanılabilir.
*/
private String getCurrentRequestId() {
// Basit bir implementasyon - gerçek uygulamada MDC veya ThreadLocal kullanın
return "OUTGOING-" + UUID.randomUUID().toString();
}

/**
* Byte dizisini string'e dönüştüren yardımcı metot.
*/
private String getContentAsString(byte[] content, String encoding) {
if (content == null || content.length == 0) {
return "";
}
try {
return new String(content, encoding != null ? encoding : "UTF-8");
} catch (UnsupportedEncodingException e) {
log.error("Desteklenmeyen encoding", e);
return "[Desteklenmeyen encoding]";
}
}

/**
* IncomingRequestLoggingInterceptor'ı ekleyen yapılandırma.
*/
@Override
public void addInterceptors(InterceptorRegistry registry) {
registry.addInterceptor(new IncomingRequestLoggingInterceptor());
}

/**
* Loglama özelliği eklenmiş RestTemplate bean'i.
*/
@Bean
public RestTemplate loggingRestTemplate() {
ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(
new SimpleClientHttpRequestFactory()
);

RestTemplate restTemplate = new RestTemplate(factory);
restTemplate.setInterceptors(List.of(new RestTemplateLoggingInterceptor()));
return restTemplate;
}

// İhtiyaç duyulabilecek diğer yardımcı metotlar...
}