package com.ecommerceserver.exception;

/**
 * 权限不足异常：当用户无权限访问接口时抛出
 */
public class PermissionDeniedException extends RuntimeException {


    // 带错误信息的构造方法
    public PermissionDeniedException(String message) {
        super(message); // 调用父类 RuntimeException 的构造方法，传递错误信息
    }

}