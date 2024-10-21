package ${packageName}.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ${packageName}.config.${systemName}ServiceConfig;
import ${packageName}.model.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class ${className} {

private final ${systemName}ServiceConfig serviceConfig;
private final RestTemplate restTemplate;

@Autowired
public ${className}(${systemName}ServiceConfig serviceConfig, RestTemplate restTemplate) {
this.serviceConfig = serviceConfig;
this.restTemplate = restTemplate;
}

<#list apiRequests as api>
    public ${api.responseClassName} ${api.methodName}(
<#-- Metot Parametreleri Başlangıcı -->
<#-- Path Variables -->
    <#if api.pathParams?? && (api.pathParams?size > 0)>
        <#list api.pathParams?keys as param>
            String ${param}<#if param_has_next || (api.queryParams?? && (api.queryParams?size > 0)) || api.requestClassName??>, </#if>
        </#list>
    </#if>
<#-- Query Parameters -->
    <#if api.queryParams?? && (api.queryParams?size > 0)>
        <#list api.queryParams?keys as param>
            String ${param}<#if param_has_next || api.requestClassName??>, </#if>
        </#list>
    </#if>
<#-- Request Body -->
    <#if api.requestClassName??>
        ${api.requestClassName} request<#if true>, </#if>
    </#if>
<#-- Metot Parametreleri Sonu -->
    ) {
    String url = serviceConfig.getBaseUrl() + "${api.uri}";

<#-- Query Parameters -->
    <#if api.queryParams?? && (api.queryParams?size > 0)>
        StringBuilder queryString = new StringBuilder("?");
        <#list api.queryParams?keys as param>
            queryString.append("${param}=").append(${param}).append("&");
        </#list>
        url += queryString.toString().substring(0, queryString.length() - 1);
    </#if>

    HttpHeaders headers = new HttpHeaders();
<#-- Authentication ve Headers -->
    <#assign authType = (api.authType)!defaultAuthType>
<#-- Bearer Authentication -->
    <#if authType == "Bearer">
        headers.set("Authorization", "Bearer " + serviceConfig.getApiKey());
    </#if>
<#-- Basic Authentication -->
    <#if authType == "Basic">
        String auth = serviceConfig.getUsername() + ":" + serviceConfig.getPassword();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
    </#if>

<#-- No Authentication -->
    <#if authType == "None">
        // No authentication header
    </#if>

<#-- Custom Headers -->
    <#if api.headers??>
        <#list api.headers?keys as header>
            headers.set("${header}", "${api.headers[header]}");
        </#list>
    </#if>

    HttpEntity<?> entity = new HttpEntity<>(<#if api.requestClassName??>request<#else>null</#if>, headers);

    ResponseEntity<${api.responseClassName}> response = restTemplate.exchange(
    url,
    HttpMethod.${api.httpMethod?upper_case},
    entity,
    ${api.responseClassName}.class
    );

    return response.getBody();
    }
</#list>
}