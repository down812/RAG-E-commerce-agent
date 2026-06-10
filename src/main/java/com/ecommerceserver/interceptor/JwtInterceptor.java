package com.ecommerceserver.interceptor;

import com.ecommerceserver.config.JwtConfig;
import com.ecommerceserver.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static com.ecommerceserver.constants.MessageConstant.*;
import static com.ecommerceserver.constants.RedisConstant.LOGIN_USER_TOKEN;

/**
 * JWT令牌校验拦截器：仅校验token有效性，不处理登录态的业务逻辑
 */
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private JwtConfig jwtConfig;

    // 无需拦截的接口
    private static final String[] EXCLUDE_PATHS = {
            "/auth/login",
            "/user/guest/login", "/user/auth", "/user/code",
            "/doc.html", "/webjars/**", "/swagger-resources/**"
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {




        try {
            // 1. 跳过无需拦截的接口
            String requestUri = request.getRequestURI();
            for (String path : EXCLUDE_PATHS) {
                if (matchPath(requestUri, path)) {
                    return true;
                }
            }

            // 2. 从请求头获取 token
            String token = request.getHeader("token");
            log.info("接收到的 token：{}", token);
            if (!StringUtils.hasText(token)) {
                sendError(response, 401, "请在 token 头中携带令牌");
                return false;
            }
            token = token.trim();

            // 3. 校验 Redis 中令牌有效性
            String redisKey = LOGIN_USER_TOKEN + token;
            String userInfoJson = redisTemplate.opsForValue().get(redisKey);

            log.info("Redis key for token: {}", redisKey);

            if (!StringUtils.hasText(userInfoJson)) {
                sendError(response, 401, "令牌已过期或未登录，请重新获取令牌");
                return false;
            }

            // 4. 解析 JWT 令牌（校验签名、过期时间）
            Claims claims;
            try {
                claims = jwtUtil.parseToken(token);
            } catch (ExpiredJwtException e) {
                sendError(response, 401, "令牌已过期，请重新获取");
                return false;
            } catch (Exception e) {
                sendError(response, 401, "令牌无效（签名错误或格式异常）");
                return false;
            }

            // 5. 【关键】将 token 和 claims 存入请求属性，供后续 LoginInterceptor 使用
            request.setAttribute("validToken", token);
            request.setAttribute("jwtClaims", claims);

            // 6. 放行
            return true;
        } catch (Exception e) {
            sendError(response, 401, "令牌校验失败：" + e.getMessage());
            return false;
        }
    }

    // 路径匹配工具（支持 ** 通配符）
    private boolean matchPath(String requestUri, String pattern) {
        if (requestUri.equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return requestUri.startsWith(prefix);
        }
        return false;
    }

    // 发送 JSON 格式的错误响应
    private void sendError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write("{\"code\":" + code + ",\"msg\":\"" + msg + "\"}");
        out.flush();
        out.close();
    }
}