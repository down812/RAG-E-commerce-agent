package com.ecommerceserver.exception;

/**
 * 业务层：资源未找到异常（如对话记录不存在）
 */
public class BusinessNotFoundException extends RuntimeException {
    public BusinessNotFoundException(String message) {
        super(message);
    }
}