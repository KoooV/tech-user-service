package com.kov.techuserservice.observability;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * P2 Observability: OpenAPI-группировка.
 * Группы дублируют {@code springdoc.group-configs} из application.properties
 * программными бинами — так группы видны и при внешнем конфиге, и по умолчанию:
 * /v3/api-docs/users, /v3/api-docs/auth, /v3/api-docs/addresses, /v3/api-docs/actuator.
 */
@Configuration
public class OpenApiConfig {

    @Value("${info.app.version:0.0.1-SNAPSHOT}")
    private String appVersion;

    @Bean
    public OpenAPI techUserServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("tech-user-service API")
                        .description("User management microservice: auth, users, addresses")
                        .version(appVersion))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    @Bean
    public GroupedOpenApi usersGroup() {
        return GroupedOpenApi.builder()
                .group("users")
                .pathsToMatch("/api/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi addressesGroup() {
        return GroupedOpenApi.builder()
                .group("addresses")
                .pathsToMatch("/api/users/*/addresses/**")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorGroup() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
