package com.ecommerceserver.utils;

import com.ecommerceserver.model.vo.AIChatResponse;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class ExtractUtils {
    private static final String TEXT_TAG = "【TEXT】";
    private static final String RESULT_START_TAG = "【RESULT】";
    private static final String RESULT_END_TAG = "【/RESULT】";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static AIChatResponse extractStructuredResult(String raw) {
        int start = raw.indexOf(RESULT_START_TAG);
        int end = raw.indexOf(RESULT_END_TAG);
        if (start < 0) return null;

        String json = start >= 0 ? raw.substring(start + RESULT_START_TAG.length(),
                end > start ? end : raw.length()) : "";
        json = cleanJsonContent(json);

        try {
            return objectMapper.readValue(json, AIChatResponse.class);
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    private static String cleanJsonContent(String json) {
        if (json == null || json.isEmpty()) return json;
        String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "")
                .replaceAll("```", "").replaceAll("\\s*【/RESULT】\\s*", "").trim();
        int s = cleaned.indexOf("{");
        int e = cleaned.lastIndexOf("}");
        return s >= 0 && e > s ? cleaned.substring(s, e + 1) : cleaned.trim();
    }

    public static String extractPureText(String full) {
        if (full == null || full.isEmpty()) return "";
        String text = full;
        int textStart = text.indexOf(TEXT_TAG);
        if (textStart >= 0) {
            int resultStart = text.indexOf(RESULT_START_TAG);
            text = resultStart > textStart ?
                    text.substring(textStart + TEXT_TAG.length(), resultStart) :
                    text.substring(textStart + TEXT_TAG.length());
        }
        return text.replaceAll("```json\\s*", "").replaceAll("```", "")
                .replaceAll(RESULT_START_TAG, "").replaceAll(RESULT_END_TAG, "").trim();
    }
}
