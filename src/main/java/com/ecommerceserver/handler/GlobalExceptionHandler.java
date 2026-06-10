package com.ecommerceserver.handler;

import com.ecommerceserver.exception.GlobalException;
import com.ecommerceserver.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.sql.SQLIntegrityConstraintViolationException;

import static com.ecommerceserver.constants.MessageConstant.*;


/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    /**
     * 处理sql异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")){
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username + ACCOUNT_ALREADY_EXIST;
            return Result.error(msg);
        }else{
            return Result.error(UNKNOWN_ERROR);
        }
    }

    @ExceptionHandler(GlobalException.class)
    public Result<String> handleGlobalException(GlobalException ex) {
        log.error(BUSINESS_ERROR, ex);
        return Result.error(ex.getResult().getMsg()); // 返回异常中携带的Result信息
    }

    /**
     * 拦截未捕获异常
     */
    @ExceptionHandler(value = Throwable.class)
    public void exceptionHandler(HttpServletRequest request, HttpServletResponse response, Throwable ex){
        String accept = request.getHeader("Accept");
        boolean isSSE = (accept != null && accept.contains("text/event-stream"));

        if (isSSE) {
            if (ex instanceof java.io.IOException && ex.getMessage() != null && ex.getMessage().contains("中止了一个已建立的连接")) {
                log.warn("[SSE客户端断开连接] [{}] {}", request.getMethod(), request.getRequestURL().toString());
            } else {
                log.error("[SSE异常] [{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex.getMessage());
            }
            return;
        }

        if(ex.getMessage() != null){
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex, ex.getCause());
        } else {
            log.error("[{}] {} ", request.getMethod(), getUrl(request), ex);
        }

        response.setStatus(500);
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write("{\"code\":500,\"msg\":\"" + SYSTEM_ERROR + "\"}");
            response.getWriter().flush();
        } catch (java.io.IOException e) {
            log.error("写入错误响应失败", e);
        }
    }

    private String getUrl(HttpServletRequest request) {
        if (StringUtils.isEmpty(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }
}
