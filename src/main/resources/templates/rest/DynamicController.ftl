package ${packageName}.controller;

import ${packageName}.service.${serviceClassName};
import ${packageName}.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ${className} {

private final ${serviceClassName} ${serviceVarName};

@Autowired
public ${className}(${serviceClassName} ${serviceVarName}) {
this.${serviceVarName} = ${serviceVarName};
}

<#list apiRequests as api>
    @${api.httpMethod?capitalize}Mapping("${api.uri}")
    public ResponseEntity<${api.responseClassName}> ${api.methodName}(
<#-- Metot Parametreleri Başlangıcı -->
<#-- Path Variables -->
    <#if api.pathParams?? && (api.pathParams?size > 0)>
        <#list api.pathParams?keys as param>
            @PathVariable("${param}") String ${param}<#if param_has_next || (api.queryParams?? && (api.queryParams?size > 0)) || api.requestClassName??>, </#if>
        </#list>
    </#if>
<#-- Query Parameters -->
    <#if api.queryParams?? && (api.queryParams?size > 0)>
        <#list api.queryParams?keys as param>
            @RequestParam("${param}") String ${param}<#if param_has_next || api.requestClassName??>, </#if>
        </#list>
    </#if>
<#-- Request Body -->
    <#if api.requestClassName??>
        @RequestBody ${api.requestClassName} request<#if true>, </#if>
    </#if>
<#-- Metot Parametreleri Sonu -->
    ) {
<#-- Servis Metodu Argümanları -->
    return ResponseEntity.ok(
    ${serviceVarName}.${api.methodName}(
<#-- Path Variables -->
    <#if api.pathParams?? && (api.pathParams?size > 0)>
        <#list api.pathParams?keys as param>
            ${param}<#if param_has_next || (api.queryParams?? && (api.queryParams?size > 0)) || api.requestClassName??>, </#if>
        </#list>
    </#if>
<#-- Query Parameters -->
    <#if api.queryParams?? && (api.queryParams?size > 0)>
        <#list api.queryParams?keys as param>
            ${param}<#if param_has_next || api.requestClassName??>, </#if>
        </#list>
    </#if>
<#-- Request Body -->
    <#if api.requestClassName??>
        request
    </#if>
    )
    );
    }
</#list>
}