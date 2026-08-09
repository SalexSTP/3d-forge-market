package com.aleksandar.customprintservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customPrintServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Custom Print Service API")
                        .description("REST API for managing custom 3D print offer requests.")
                        .version("v1"));
    }
}
