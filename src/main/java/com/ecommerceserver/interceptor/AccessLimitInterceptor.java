package com.ecommerceserver.interceptor;

import com.ecommerceserver.aop.AccessLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
@EnableAsync
@Component
public class AccessLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //自定义线程池
    private static final Executor accessLimitExecutor = java.util.concurrent.Executors.newFixedThreadPool(10);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 异步执行访问限制检查
        CompletableFuture.runAsync(() -> doAccessLimitCheck((HttpServletRequest) request, (HttpServletResponse) response, handler), accessLimitExecutor);
        // 直接放行请求，不等待异步任务完成
        return true;
    }


    private void doAccessLimitCheck(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            AccessLimit accessLimit = handlerMethod.getMethodAnnotation(AccessLimit.class);
            if (accessLimit == null) {
                return;
            }
            int seconds = accessLimit.seconds();
            int maxCount = accessLimit.maxCount();

            String ip = request.getRemoteAddr();
            String key = ip + ":" + request.getServletPath();
            String count = stringRedisTemplate.opsForValue().get(key);
            if (count == null || count.equals("-1")) {
                stringRedisTemplate.opsForValue().set(key, "1", Long.valueOf(seconds), TimeUnit.SECONDS);
            } else if (Integer.parseInt(count) < maxCount) {
                stringRedisTemplate.opsForValue().increment(key);
            } else {
                try {
                    response.setContentType("text/html;charset=UTF-8");
                    response.getWriter().write("请求过于频繁，请稍后再试");
                    response.setStatus(429); // 设置HTTP状态码为429 Too Many Requests
                } catch (Exception e) {
                    // 记录异常日志
                    java.util.logging.Logger.getLogger(AccessLimitInterceptor.class.getName()).log(java.util.logging.Level.SEVERE, "写入响应失败", e);
                }
            }
        }
    }

}
