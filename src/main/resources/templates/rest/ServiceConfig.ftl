package ${packageName}.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Data
@Configuration
@ConfigurationProperties("${systemName}")
public class ${systemClassName}}ServiceConfig {

private String baseUrl;
private String username;
private String password;
private Integer connectTimeout;
private Integer requestTimeout;

    @Bean
    public RestTemplate restTemplate() {

    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(requestTimeout);
    return new RestTemplate(factory);

    }
}