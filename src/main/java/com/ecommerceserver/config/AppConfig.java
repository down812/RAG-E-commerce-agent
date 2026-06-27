package com.ecommerceserver.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    // 1. 定义一个通用的 HttpClient，设置连接超时
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30)) // 设置连接超时，例如 30 秒
                .build();
    }

    // 2. 配置 RestClient.Builder（用于非流式请求）
    @Bean
    public RestClient.Builder restClientBuilder(HttpClient httpClient) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(120)); // 设置读取超时为 2 分钟

        return RestClient.builder().requestFactory(factory);
    }

    /*// 3. 配置 WebClient.Builder（用于流式请求，如 SSE）
    @Bean
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        JdkClientHttpConnector connector = new JdkClientHttpConnector(httpClient);
        connector.wait(Duration.ofSeconds(120)); // 设置读取超时为 2 分钟

        return WebClient.builder().clientConnector(connector);
    }*/
}
