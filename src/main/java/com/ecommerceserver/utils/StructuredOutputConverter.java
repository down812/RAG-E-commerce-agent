package com.ecommerceserver.utils;

import com.ecommerceserver.model.vo.AIChatResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class StructuredOutputConverter {

    private final ObjectMapper objectMapper;

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```(?:json)?\\s*([\\s\\S]*?)```"
    );
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "\\{[^{}]*\"type\"\\s*:\\s*\"(recommendation|comparison|search_result)\"[^{}]*\\}",
            Pattern.MULTILINE
    );

    public StructuredOutputConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /*public Object convertToStructuredResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        try {
            String jsonContent = extractJsonFromMarkdown(content);

            if (jsonContent.contains("\"type\":\"recommendation\"")) {
                return objectMapper.readValue(jsonContent, AIChatResponse.RecommendationData.class);
            } else if (jsonContent.contains("\"type\":\"comparison\"")) {
                return objectMapper.readValue(jsonContent, AIChatResponse.ComparisonData.class);
            } else if (jsonContent.contains("\"type\":\"search_result\"")) {
                return objectMapper.readValue(jsonContent, AIChatResponse.SearchResultData.class);
            }

            return content;
        } catch (JsonProcessingException e) {
            log.warn("JSON解析失败，返回原始文本: {}", e.getMessage());
            return content;
        }
    }*/

    public String extractJsonFromMarkdown(String content) {
        if (content == null) {
            return "";
        }

        String trimmed = content.trim();

        if (trimmed.startsWith("```json") || trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n") + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                return trimmed.substring(start, end).trim();
            }
        }

        int jsonStart = trimmed.indexOf("{");
        int jsonEnd = trimmed.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return trimmed.substring(jsonStart, jsonEnd + 1);
        }

        return trimmed;
    }

    public String detectResponseType(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "text";
        }

        try {
            String jsonContent = extractJsonFromMarkdown(content);

            if (jsonContent.contains("\"type\":\"recommendation\"")) {
                return "recommendation";
            } else if (jsonContent.contains("\"type\":\"comparison\"")) {
                return "comparison";
            } else if (jsonContent.contains("\"type\":\"search_result\"")) {
                return "search_result";
            }
        } catch (Exception e) {
            log.debug("类型检测失败: {}", e.getMessage());
        }

        return "text";
    }

    public Optional<AIChatResponse> tryParseStructuredResponse(String accumulatedText) {
        if (accumulatedText == null || accumulatedText.length() < 50) {
            return Optional.empty();
        }

        try {
            String extractedJson = extractJsonFromMarkdown(accumulatedText);

            if (extractedJson.length() < 50 || !extractedJson.contains("\"type\"")) {
                return Optional.empty();
            }

            AIChatResponse response = objectMapper.readValue(extractedJson, AIChatResponse.class);

            if (response.getResponseType() != null &&
                    !response.getResponseType().equals("text")) {
                return Optional.of(response);
            }

            return Optional.empty();
        } catch (JsonProcessingException e) {
            log.debug("流式JSON解析中，尚未完整: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isJsonComplete(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String trimmed = text.trim();

        if (trimmed.startsWith("```")) {
            return trimmed.endsWith("```") && trimmed.indexOf("```") != trimmed.lastIndexOf("```");
        }

        int braceCount = 0;
        for (char c : trimmed.toCharArray()) {
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
        }

        return braceCount == 0 && trimmed.contains("\"type\"");
    }
}