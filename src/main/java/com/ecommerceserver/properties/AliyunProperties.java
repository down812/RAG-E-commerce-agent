package com.ecommerceserver.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author dawn
 * @date 2024-09-11 17:16
 * 阿里云配置类
 */
@Data
@Component
@ConfigurationProperties("tech-pilot-server.aliyun")
public class AliyunProperties {
    /**
     * 阿里云accessKey
     */
    private String accessKey;

    /**
     * 阿里云accessSecret
     */
    private String accessSecret;

    /**
     * 地域ID
     */
    private String regionId;

    /**
     * api域名
     */
    private String sysDomain;

    /**
     * api版本
     */
    private String sysVersion;
}
