package com.ecommerceserver.utils;

import com.ecommerceserver.model.vo.AIChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIResponseChunkParser {

    private final ObjectMapper objectMapper;

    public AIChatResponse parseChunks(List<ChatResponse> chunks, String sessionId, String messageId) {
        if (chunks == null || chunks.isEmpty()) {
            return AIChatResponse.textResponse(sessionId, messageId, "");
        }

        StringBuilder fullContent = new StringBuilder();
        for (ChatResponse chunk : chunks) {
            String content = extractContent(chunk);
            if (content != null && !content.isBlank()) {
                fullContent.append(content);
            }
        }

        String content = fullContent.toString();
        if (content.isBlank()) {
            return AIChatResponse.textResponse(sessionId, messageId, "");
        }

        log.debug("【Chunk解析】完整内容长度={}", content.length());

        String jsonContent = extractJson(content);
        String responseType = detectResponseType(jsonContent);
        List<String> sources = extractSources(content);

        return buildResponse(responseType, jsonContent, sessionId, messageId, sources);
    }

    private String extractContent(ChatResponse chunk) {
        if (chunk == null || chunk.getResult() == null) {
            return null;
        }
        try {
            var output = chunk.getResult().getOutput();
            if (output != null) {
                java.lang.reflect.Method getContentMethod = output.getClass().getMethod("getContent");
                Object content = getContentMethod.invoke(output);
                return content != null ? content.toString() : null;
            }
        } catch (Exception e) {
            log.debug("提取内容失败: {}", e.getMessage());
        }
        return null;
    }

    private String extractJson(String content) {
        if (content == null) return "";
        String trimmed = content.trim();
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String detectResponseType(String json) {
        if (json.contains("\"type\":\"recommendation\"")) {
            return "recommendation";
        } else if (json.contains("\"type\":\"search_result\"")) {
            return "search_result";
        } else if (json.contains("\"type\":\"image_search\"") || json.contains("\"responseType\":\"image_search\"")) {
            return "image_search";
        }
        return "text";
    }

    private List<String> extractSources(String content) {
        List<String> sources = new ArrayList<>();
        if (content != null && content.contains("【来源：")) {
            String[] parts = content.split("【来源：");
            for (int i = 1; i < parts.length; i++) {
                int endIndex = parts[i].indexOf("】");
                if (endIndex > 0) {
                    sources.add(parts[i].substring(0, endIndex));
                }
            }
        }
        return sources;
    }

    private AIChatResponse buildResponse(String type, String json, String sessionId,
                                         String messageId, List<String> sources) {
        return switch (type) {
            case "recommendation" -> parseRecommendation(json, sessionId, messageId, sources);
            case "search_result" -> parseSearchResult(json, sessionId, messageId, sources);
            case "image_search" -> parseImageSearch(json, sessionId, messageId, sources);
            default -> AIChatResponse.textResponse(sessionId, messageId, json);
        };
    }

    private AIChatResponse parseRecommendation(String json, String sessionId,
                                               String messageId, List<String> sources) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return AIChatResponse.builder()
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .responseType("recommendation")
                    .sourcesStr(sources)
                    .timestamp(System.currentTimeMillis())
                    .queryAnalysis(parseQueryAnalysis(node.get("query_analysis")))
                    .recommendations(parseRecommendations(node.get("recommendations")))
                    .build();
        } catch (Exception e) {
            log.warn("推荐响应解析失败: {}", e.getMessage());
            return AIChatResponse.textResponse(sessionId, messageId, json);
        }
    }

    private AIChatResponse.QueryAnalysis parseQueryAnalysis(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        return AIChatResponse.QueryAnalysis.builder()
                .detectedCategory(getTextValue(node, "detected_category"))
                .budget(getTextValue(node, "budget"))
                .specialRequirements(parseStringList(node.get("special_requirements")))
                .build();
    }

    private List<AIChatResponse.RecommendedProduct> parseRecommendations(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        List<AIChatResponse.RecommendedProduct> list = new ArrayList<>();
        for (JsonNode item : node) {
            list.add(AIChatResponse.RecommendedProduct.builder()
                    .productId(getLongValue(item, "product_id"))
                    .productName(getTextValue(item, "product_name"))
                    .price(getDoubleValue(item, "price"))
                    .brand(getTextValue(item, "brand"))
                    .category(getTextValue(item, "category"))
                    .keyFeatures(parseStringList(item.get("key_features")))
                    .reason(getTextValue(item, "reason"))
                    .applicableScenario(getTextValue(item, "applicable_scenario"))
                    .rating(getTextValue(item, "rating"))
                    .salesCount(getIntValue(item, "sales_count"))
                    .mainImageUrl(getTextValue(item, "main_image_url"))
                    .build());
        }
        return list;
    }

    private AIChatResponse parseSearchResult(String json, String sessionId,
                                             String messageId, List<String> sources) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return AIChatResponse.builder()
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .responseType("search_result")
                    .sourcesStr(sources)
                    .timestamp(System.currentTimeMillis())
                    .searchCriteria(parseSearchCriteria(node.get("search_criteria")))
                    .totalCount(getIntValue(node, "total_count"))
                    .products(parseSearchProducts(node.get("products")))
                    .build();
        } catch (Exception e) {
            log.warn("搜索响应解析失败: {}", e.getMessage());
            return AIChatResponse.textResponse(sessionId, messageId, json);
        }
    }

    private AIChatResponse.SearchCriteria parseSearchCriteria(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        return AIChatResponse.SearchCriteria.builder()
                .keyword(getTextValue(node, "keyword"))
                .brand(getTextValue(node, "brand"))
                .priceRange(getTextValue(node, "price_range"))
                .category(getTextValue(node, "category"))
                .build();
    }

    private List<AIChatResponse.SearchProduct> parseSearchProducts(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        List<AIChatResponse.SearchProduct> list = new ArrayList<>();
        for (JsonNode item : node) {
            list.add(AIChatResponse.SearchProduct.builder()
                    .productId(getLongValue(item, "product_id"))
                    .productCode(getTextValue(item, "product_code"))
                    .productName(getTextValue(item, "product_name"))
                    .brand(getTextValue(item, "brand"))
                    .category(getTextValue(item, "category"))
                    .price(getDoubleValue(item, "price"))
                    .status(getTextValue(item, "status"))
                    .salesCount(getIntValue(item, "sales_count"))
                    .stockStatus(getTextValue(item, "stock_status"))
                    .highlight(getTextValue(item, "highlight"))
                    .mainImageUrl(getTextValue(item, "main_image_url"))
                    .build());
        }
        return list;
    }

    private AIChatResponse parseImageSearch(String json, String sessionId,
                                             String messageId, List<String> sources) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return AIChatResponse.builder()
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .responseType("image_search")
                    .sourcesStr(sources)
                    .timestamp(System.currentTimeMillis())
                    .imageAnalysis(parseImageAnalysis(node.get("image_analysis")))
                    .imageSearchProducts(parseImageSearchProducts(node.get("image_search_products")))
                    .build();
        } catch (Exception e) {
            log.warn("图片识别响应解析失败: {}", e.getMessage());
            return AIChatResponse.textResponse(sessionId, messageId, json);
        }
    }

    private AIChatResponse.ImageAnalysis parseImageAnalysis(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        return AIChatResponse.ImageAnalysis.builder()
                .detectedCategory(getTextValue(node, "detected_category"))
                .detectedBrand(getTextValue(node, "detected_brand"))
                .visualFeatures(parseStringList(node.get("visual_features")))
                .colorDescription(getTextValue(node, "color_description"))
                .shapeDescription(getTextValue(node, "shape_description"))
                .textOnProduct(getTextValue(node, "text_on_product"))
                .build();
    }

    private List<AIChatResponse.ImageSearchProduct> parseImageSearchProducts(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        List<AIChatResponse.ImageSearchProduct> list = new ArrayList<>();
        for (JsonNode item : node) {
            list.add(AIChatResponse.ImageSearchProduct.builder()
                    .productId(getLongValue(item, "product_id"))
                    .productCode(getTextValue(item, "product_code"))
                    .productName(getTextValue(item, "product_name"))
                    .brand(getTextValue(item, "brand"))
                    .category(getTextValue(item, "category"))
                    .price(getDoubleValue(item, "price"))
                    .mainImageUrl(getTextValue(item, "main_image_url"))
                    .salesCount(getIntValue(item, "sales_count"))
                    .similarity(getDoubleValue(item, "similarity"))
                    .matchReason(getTextValue(item, "match_reason"))
                    .build());
        }
        return list;
    }

    private String getTextValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    private Long getLongValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asLong() : null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asInt() : null;
    }

    private Double getDoubleValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) return null;
        if (fieldNode.isNumber()) {
            return fieldNode.asDouble();
        }
        try {
            return Double.parseDouble(fieldNode.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> parseStringList(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        node.forEach(n -> {
            if (!n.isNull()) list.add(n.asText());
        });
        return list;
    }

    private Map<String, String> parseStringMap(JsonNode node) {
        if (node == null || !node.isObject()) return Map.of();
        Map<String, String> map = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!field.getValue().isNull()) {
                map.put(field.getKey(), field.getValue().asText());
            }
        }
        return map;
    }
}