package ${packageName}.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ${packageName}.config.ServiceConfig;

import java.util.Map;

@Service
public class ${className} {

private final ServiceConfig serviceConfig;
private final WebClient webClient;

@Autowired
public ${className}(ServiceConfig serviceConfig) {
this.serviceConfig = serviceConfig;
this.webClient = WebClient.builder()
.baseUrl(serviceConfig.getBaseUrl())
<#if authType == "Basic">
    .defaultHeaders(headers -> headers.setBasicAuth(serviceConfig.getUsername(), serviceConfig.getPassword()))
</#if>
<#if authType == "Bearer">
    .defaultHeaders(headers -> headers.setBearerAuth(serviceConfig.getApiKey()))
</#if>
.build();
}

<#list apiRequests as api>
    public Mono<${api.responseClassName}> ${api.methodName}(<#if api.requestClassName??>${api.requestClassName} request</#if>) {
    WebClient.RequestBodySpec requestSpec = webClient
    .method(HttpMethod.${api.httpMethod?upper_case})
    .uri(uriBuilder -> {
    uriBuilder.path("${api.uri}");
    <#if api.queryParams??>
        <#list api.queryParams?keys as param>
            uriBuilder.queryParam("${param}", ${api.queryParams[param]});
        </#list>
    </#if>
    return uriBuilder.build(<#if api.pathParams??><#list api.pathParams?keys as param>"${api.pathParams[param]}"<#if param_has_next>, </#if></#list></#if>);
    })
    .headers(headers -> {
    <#if api.headers??>
        <#list api.headers?keys as header>
            headers.add("${header}", "${api.headers[header]}");
        </#list>
    </#if>
    });
    <#if api.requestBody??>
        requestSpec.bodyValue(request);
    </#if>

    return requestSpec.retrieve()
    .onStatus(HttpStatus::isError, clientResponse -> {
    // Hata yönetimi
    return Mono.error(new RuntimeException("İstek sırasında hata oluştu"));
    })
    .bodyToMono(${api.responseClassName}.class);
    }
</#list>
}
