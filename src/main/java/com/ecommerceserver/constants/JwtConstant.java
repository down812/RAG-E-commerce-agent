package com.ecommerceserver.constants;

/**
 * @author dawn
 * @date 2024-09-06 21:22
 */
public class JwtConstant {

    // 配置文件中 user-secret-key 的实际路径（键名）
    public static final String USER_SECRET_KEY_CONFIG = "${tech-pilot-server.jwt.user-secret-key}";
    public static final int TOKEN_TTL = 86400;
    public static final String TOKEN_NAME = "token";


}
