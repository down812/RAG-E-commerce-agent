package com.ecommerceserver.exception;


import com.ecommerceserver.result.Result;

/**
 * @author dawn
 * @date 2024-09-11 21:10
 * 验证码异常类
 */
public class CodeException extends GlobalException{

//    public CodeException(String msg){
//        super(msg);
//    }


    /**
     * 统一结果类
     */
    private Result<Object> result;

    public CodeException(Result<Object> result) {
        super(result);
        this.result = result;
    }
}
