package ${packageName}.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("${systemName}")
public class SokServiceConfig {

private String baseUrl;
private String username;
private String password;
private Integer connectTimeout;
private Integer requestTimeout;

}