package com.ecommerceserver.exception;

import com.ecommerceserver.result.Result;

/**
 * 登录失败异常类
 */
public class LoginFailException extends GlobalException {

    /**
     * 携带的失败结果
     */
    private Result<Object> result;

    /**
     * 通过错误信息创建异常（最常用）
     * @param msg 错误描述（如"账号不存在"、"密码错误"）
     */
    public LoginFailException(String msg) {
        super(msg);
        // 调用 Result.error() 生成失败结果（code=0，msg=自定义信息）
        this.result = Result.error(msg); // 关键修正：添加这一行，生成 Result 类型的错误结果
    }

    /**
     * 直接通过 Result 对象创建异常（灵活场景）
     * @param result 已构建的失败结果
     */
    public LoginFailException(Result<Object> result) {
        super(result.getMsg());
        this.result = result;
    }

    // 获取异常携带的结果（供全局处理器使用）
    public Result<Object> getResult() {
        return result;
    }
}