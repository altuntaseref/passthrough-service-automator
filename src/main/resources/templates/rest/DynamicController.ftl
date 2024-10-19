package ${packageName}.controller;

import ${packageName}.service.${serviceClassName};
import ${packageName}.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ${className} {

private final ${serviceClassName} ${serviceVarName};

@Autowired
public ${className}(${serviceClassName} ${serviceVarName}) {
this.${serviceVarName} = ${serviceVarName};
}

<#list apiRequests as api>
    @${api.httpMethod?capitalize}Mapping("${api.uri}")
    public Mono<ResponseEntity<${api.responseClassName}>> ${api.methodName}(
    <#if api.requestClassName??>@RequestBody ${api.requestClassName} request</#if>) {
    return ${serviceVarName}.${api.methodName}(<#if api.requestClassName??>request</#if>)
    .map(response -> ResponseEntity.ok(response));
    }
</#list>
}
