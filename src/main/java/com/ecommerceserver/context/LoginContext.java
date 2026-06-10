package com.ecommerceserver.context;

import lombok.Data;

/**
 * 登录用户上下文：存储当前请求的用户信息（ThreadLocal确保线程安全）
 */
public class LoginContext {

    // 线程本地存储：每个请求独立线程独立独立存储用户信息
    private static final ThreadLocal<LoginUserDTO> USER_HOLDER = new ThreadLocal<>();

    /**
     * 存储用户信息到上下文
     */
    public static void setUser(LoginUserDTO user) {
        USER_HOLDER.set(user);
    }

    /**
     * 获取当前登录用户信息
     */
    public static LoginUserDTO getUser() {
        return USER_HOLDER.get();
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        LoginUserDTO user = getUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户类型（用于权限判断）
     */
    public static Integer getUserType() {
        LoginUserDTO user = getUser();
        return user != null ? user.getUserType() : null;
    }

    /**
     * 清除上下文（必须在请求结束时调用，防止内存泄漏）
     */
    public static void clear() {
        USER_HOLDER.remove();
    }

    /**
     * 登录用户DTO：存储从Token中解析的核心信息
     */
    @Data
    public static class LoginUserDTO {
        private Long userId;      // 用户ID
        private Integer userType;    // 用户类型（0-超级管理员、1-普通管理员、2-普通用户）
        private String loginType;    // 登录方式（可选，如"password"、"code"）
    }
}