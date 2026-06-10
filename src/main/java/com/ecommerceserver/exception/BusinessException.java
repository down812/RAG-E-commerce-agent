package com.ecommerceserver.exception;

/**
 * 自定义业务异常类，用于处理业务逻辑中的异常情况
 */
public class BusinessException extends RuntimeException {

    // 可以添加错误码等字段，方便更细粒度的异常处理和返回
    private Integer code;

    public BusinessException() {
        super();
    }

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}