package com.ecommerceserver.config;

import com.ecommerceserver.constants.JwtConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static com.ecommerceserver.constants.JwtConstant.*;

/**
 * JWT 配置类：读取 E-commerceServer.jwt 节点下的配置
 */
@Configuration
public class JwtConfig {

    // 读取 application.yml 中 e-commerce-server.jwt.user-secret-key 的值
    @Value(JwtConstant.USER_SECRET_KEY_CONFIG)
    private String userSecretKey;

    // 读取 application.yml 中 tech-pilot-server.jwt.user-ttl 的值（之前硬编码为 86400，也需修复）
    @Value("${tech-pilot-server.jwt.user-ttl}")
    private long userTtl;

    // 读取 application.yml 中 tech-pilot-server.jwt.user-token-name 的值
    @Value(JwtConstant.TOKEN_NAME)
    private String userTokenName;


    // Getter 方法（供外部调用）
    public String getUserSecretKey() {
        return userSecretKey;
    }

    // 转换过期时间为毫秒（JWT 工具类常用毫秒）
    public long getUserTtlMillis() {
        return userTtl * 1000;
    }

    public String getUserTokenName() {
        return userTokenName;
    }
}