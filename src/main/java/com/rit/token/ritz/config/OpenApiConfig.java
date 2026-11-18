package com.rit.token.ritz.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ritzWalletOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RITZ Wallet REST API")
                        .description("Centralized campus wallet and payment management infrastructure for Rajalakshmi Institute of Technology (RIT). " +
                                "Powers student wallet balances, immutable double-entry ledger, Razorpay recharges, and external application integrations (Tillo, ERP, Stationery, Transport).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("RIT Center for Digital Innovation")
                                .email("wallet@ritchennai.edu.in")
                                .url("https://ritchennai.org"))
                        .license(new License().name("Proprietary - Rajalakshmi Institute of Technology")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth").addList("ApiKeyAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .name("BearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token for Student, Merchant and Administrator operations"))
                        .addSecuritySchemes("ApiKeyAuth",
                                new SecurityScheme()
                                        .name("X-API-KEY")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("API key for external application integrations (Tillo, ERP, etc.)")));
    }
}
