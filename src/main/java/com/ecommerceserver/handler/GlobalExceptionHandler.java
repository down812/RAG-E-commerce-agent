package com.ecommerceserver.handler;

import com.ecommerceserver.exception.GlobalException;
import com.ecommerceserver.result.Result;
import jakarta.servlet.http.HttpServletRequest;
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
    public Result<String> exceptionHandler(HttpServletRequest request, Throwable ex){
        if(ex.getMessage() != null){
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex, ex.getCause());
            return Result.error(SYSTEM_ERROR);
        }
        log.error("[{}] {} ", request.getMethod(), getUrl(request), ex);

        return Result.error(SYSTEM_ERROR);
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
