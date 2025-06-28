package com.esprit.formation.config;
import io.swagger.v3.oas.models.Components;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tavail realisé par Houssem Eddine NASRI")
                        .version("1.0")
                        .description("API de gestion des formations avec Spring Boot et PostgreSQL")
                        .contact(new Contact()
                                .name("Nasri Houssem Eddine")
                                .email("houcemnasri77@gmail.com")
                                .url("https://www.linkedin.com/in/houcem-nasri")
                        )
                )
                // Ajout de la sécurité avec JWT (si besoin)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}
