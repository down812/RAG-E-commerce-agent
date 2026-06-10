package com.ecommerceserver.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author dawn
 * @date 2024-09-05 15:58
 * jwt
 */
@Component
@ConfigurationProperties(prefix = "tech-pilot-server.jwt")
@Data
public class JwtProperties {

    /**
     * pc端管理员生成jwt令牌相关配置
     */
    private String adminSecretKey;
    private long adminTtl;
    private String adminTokenName;

    /**
     * 小程序端用户生成jwt令牌相关配置
     */
    private String studentSecretKey;
    private long studentTtl;
    private String studentTokenName;

}
