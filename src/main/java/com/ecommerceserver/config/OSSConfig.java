package com.ecommerceserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OSSConfig {
    
    /**
     * OSS服务端点
     */
    private String endpoint;
    
    /**
     * 访问密钥ID
     */
    private String accessKeyId;
    
    /**
     * 访问密钥密码
     */
    private String accessKeySecret;
    
    /**
     * Bucket名称
     */
    private String bucketName;
    
    /**
     * 基础路径
     */
    private String basePath;
    
    /**
     * 自定义域名（可选，用于CDN加速）
     */
    private String customDomain;
}