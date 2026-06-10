package com.ecommerceserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 键、值都用 String 序列化器（明文存储字符串，匹配你的存储逻辑）
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }


    /**
     * 自定义 RedisTemplate<String, Long> 实例，专门用于处理 Long 类型的值
     * 解决 UserActivityUtil 中注入时的类型匹配问题
     */
    @Bean
    public RedisTemplate<String, Long> redisTemplateStringLong(RedisConnectionFactory factory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 序列化：使用 String 序列化器（避免 key 乱码）
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Value 序列化：使用 GenericToStringSerializer 处理 Long 类型
        // （相比 Jackson 序列化，更适合纯数字类型，避免额外的 JSON 格式开销）
        GenericToStringSerializer<Long> valueSerializer = new GenericToStringSerializer<>(Long.class);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        // 初始化模板
        template.afterPropertiesSet();
        return template;
    }
}