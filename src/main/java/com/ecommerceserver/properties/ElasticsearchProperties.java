package com.ecommerceserver.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.elasticsearch")
public class ElasticsearchProperties {

    private String host ;
    private int port ;
    private String username ;
    private String password ;
}