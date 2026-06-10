package com.ecommerceserver.interceptor;

import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.context.LoginContext.LoginUserDTO;
import com.ecommerceserver.exception.PermissionDeniedException;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static com.ecommerceserver.constants.CommonConstant.*;
import static com.ecommerceserver.constants.MessageConstant.*;

/**
 * 登录拦截器：基于JWT令牌解析用户信息，存入上下文并做权限校验
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 1. 获取 JwtInterceptor 传递的令牌和 claims
            String token = (String) request.getAttribute("validToken");
            Claims claims = (Claims) request.getAttribute("jwtClaims");

            // 新增：如果 claims 为 null，说明是无需拦截的接口，直接放行
            if (claims == null) {
                return true;
            }

            // 2. 从 claims 提取用户信息
            Long userId = claims.get(LOGIN_USER_ID, Long.class);
            Integer userType = claims.get(LOGIN_USER_TYPE, Integer.class);
            if (userId == null || userType == null) {
                sendError(response, 401, "令牌中缺少用户信息");
                return false;
            }

            // 3. 存入线程上下文（供后续业务使用）
            LoginUserDTO userDTO = new LoginUserDTO();
            userDTO.setUserId(userId);
            userDTO.setUserType(userType);
            LoginContext.setUser(userDTO);



            // 4. 放行
            return true;
        } catch (PermissionDeniedException e) {
            sendError(response, 403, e.getMessage());
            return false;
        } catch (Exception e) {
            sendError(response, 500, "登录态处理失败：" + e.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后，清理线程上下文（避免 ThreadLocal 内存泄漏）
        LoginContext.clear();
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