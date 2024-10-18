package ${packageName}.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {
@Bean
public OpenAPI customOpenAPI() {
return new OpenAPI()
.info(new Info().title("Ocean ${projectName}")
.description("Ocean ${projectName} Rest Service")
.version("1.0.0"));
}

@Bean
public GroupedOpenApi api() {
return GroupedOpenApi.builder()
.group("default")
.pathsToMatch("/**")
.build();
}
}
