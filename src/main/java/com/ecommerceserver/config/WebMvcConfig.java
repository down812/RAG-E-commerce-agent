package com.ecommerceserver.config;

import com.google.common.collect.Lists;
import com.ecommerceserver.interceptor.AccessLimitInterceptor;
import com.ecommerceserver.interceptor.JwtInterceptor;
import com.ecommerceserver.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {



    @Autowired
    private AccessLimitInterceptor accessLimitInterceptor;

    // 【新增】注入自定义的日期转换器
    @Autowired
    private StringToLocalDateTimeConverter dateTimeConverter;

    private List<String> IGNORE_URI = Lists.newArrayList(
            "/swagger-resources/",
            "/v3/**",
            "/v2/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/swagger-ui/**",
            "/js/**",
            "/swagger-ui.html/**",
            "/doc.html"
    );

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("*")
                .maxAge(86400)
                .allowedOriginPatterns("*")
                .exposedHeaders("Content-Disposition"); // 允许前端获取响应头中的Content-Disposition,用于处理文件下载
    }

    @Bean
    public JwtInterceptor jwtInterceptor() {
        return new JwtInterceptor();
    }

    @Bean
    public LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(IGNORE_URI)
                .order(0);

        registry.addInterceptor(loginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(IGNORE_URI)
                .order(1);

        registry.addInterceptor(accessLimitInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/image/**")
                .addResourceLocations("file:/usr/local/project/smart-campus-service-assistant/data/");
    }

    // 【新增】注册自定义转换器
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(dateTimeConverter);
    }

    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(asyncTaskExecutor());
    }
}