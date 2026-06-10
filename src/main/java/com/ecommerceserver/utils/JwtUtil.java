package com.ecommerceserver.utils;

import com.ecommerceserver.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static com.ecommerceserver.constants.CommonConstant.LOGIN_USER_ID;
import static com.ecommerceserver.constants.CommonConstant.LOGIN_USER_TYPE;

/**
 * JWT 工具类：统一生成/解析登录用户和免登录游客的令牌
 * 支持：
 * 1. 正式登录用户（type=0/1/2）
 * 2. 免登录游客（type=3）
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtConfig jwtConfig;

    /**
     * 统一生成令牌（通用方法，适用于登录用户和免登录游客）
     * @param claims 载荷（必须包含 LOGIN_USER_ID 和 LOGIN_USER_TYPE）
     * @return JWT令牌
     */
    public String createToken(Map<String, Object> claims) {
        // 签名算法：HS256
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        // 过期时间 = 当前时间 + 配置的TTL（毫秒）
        long expMillis = System.currentTimeMillis() + jwtConfig.getUserTtlMillis();
        Date expiration = new Date(expMillis);

        JwtBuilder builder = Jwts.builder()
                .setClaims(claims) // 载荷：包含userId、userType等
                .signWith(signatureAlgorithm, jwtConfig.getUserSecretKey().getBytes(StandardCharsets.UTF_8)) // 签名
                .setExpiration(expiration); // 过期时间

        return builder.compact();
    }

    /**
     * 生成带自定义头部的令牌（兼容原有登录逻辑）
     * @param claims 载荷
     * @param header 自定义头部（如{"type":"jwt"}）
     * @return JWT令牌
     */
    public String createTokenWithHeader(Map<String, Object> claims, Map<String, Object> header) {
        long expMillis = System.currentTimeMillis() + jwtConfig.getUserTtlMillis();
        Date expiration = new Date(expMillis);

        return Jwts.builder()
                .setHeader(header) // 自定义头部
                .setClaims(claims) // 载荷
                .signWith(SignatureAlgorithm.HS256, jwtConfig.getUserSecretKey().getBytes(StandardCharsets.UTF_8))
                .setExpiration(expiration)
                .compact();
    }

    /**
     * 解析令牌，获取完整载荷
     * @param token JWT令牌
     * @return 载荷Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtConfig.getUserSecretKey().getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从令牌中提取用户ID（通用方法，支持所有用户类型）
     * @param token JWT令牌
     * @return 用户ID（Long类型，兼容大用户量场景）
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get(LOGIN_USER_ID, Long.class); // 统一返回Long，避免Integer溢出
        } catch (Exception e) {
            return null; // 解析失败返回null
        }
    }

    /**
     * 从令牌中提取用户类型（通用方法，支持所有用户类型）
     * @param token JWT令牌
     * @return 用户类型（0/1/2/3）
     */
    public Integer getUserTypeFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get(LOGIN_USER_TYPE, Integer.class);
        } catch (Exception e) {
            return null; // 解析失败返回null
        }
    }

    /**
     * 从令牌中提取指定key的载荷（泛型方法，灵活获取其他信息）
     * @param token 令牌
     * @param claimKey 载荷key
     * @param claimType 载荷值类型
     * @return 载荷值
     */
    public <T> T getClaimFromToken(String token, String claimKey, Class<T> claimType) {
        try {
            Claims claims = parseToken(token);
            return claims.get(claimKey, claimType);
        } catch (Exception e) {
            return null;
        }
    }

    // 保留原有方法别名（兼容历史调用）
    public String createUserToken(Map<String, Object> claims) {
        return createToken(claims);
    }

    public Claims parseUserToken(String token) {
        return parseToken(token);
    }

    public String createJWT(Map<String, Object> claims, Map<String, Object> header) {
        return createTokenWithHeader(claims, header);
    }
}