package com.ecommerceserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("电商RAG智能助手项目接口文档")
                        .version("1.0")
                        .description("电商RAG智能助手项目接口文档")
                        .contact(new Contact().name("developer"))
                        .license(new License().name("Apache 2.0")));
    }

}