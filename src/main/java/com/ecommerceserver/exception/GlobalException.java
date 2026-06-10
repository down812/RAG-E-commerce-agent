package com.ecommerceserver.exception;

import com.ecommerceserver.result.Result;
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
public class GlobalException extends RuntimeException {

    private Result<Object> result;

    // 接收 String 的构造方法
    public GlobalException(String message) {
        super(message);
    }

    // 接收 Result 的构造方法
    public GlobalException(Result<Object> result) {
        super(result.getMsg());
        this.result = result;
    }

    public Result<Object> getResult() {
        return result;
    }
}