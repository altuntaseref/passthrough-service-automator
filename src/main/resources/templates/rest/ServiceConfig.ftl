package ${packageName}.config;

import  ${packageName}.config.RequestLoggingConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;



@Data
@Configuration
@ConfigurationProperties("${systemName}")
public class ${systemClassName}ServiceConfig {

private String baseUrl;
private String username;
private String password;
private Integer connectTimeout;
private Integer requestTimeout;
@Autowired
private RequestLoggingConfiguration requestLoggingConfiguration;

@Bean
public RestTemplate restTemplate(RestTemplateBuilder builder) {
return requestLoggingConfiguration.loggingRestTemplate();
}
}