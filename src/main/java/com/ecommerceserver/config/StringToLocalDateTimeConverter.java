package com.ecommerceserver.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@Component
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

    // 支持两种格式：yyyy-MM-dd HH:mm:ss 和 yyyy-MM-dd
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            // 可选的时分秒部分：如果有则解析，没有则默认00:00:00
            .optionalStart()
            .appendPattern(" HH:mm:ss")
            .optionalEnd()
            // 对缺失的时分秒字段，默认填充0
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .toFormatter();

    @Override
    public LocalDateTime convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(source, FORMATTER);
    }
}