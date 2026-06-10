package com.ecommerceserver.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIChatResponse {

    private String sessionId;
    private String messageId;
    private String responseType;
    private String answer;
    private List<String> sourcesStr;
    private List<Source> sources;
    private Long timestamp;

    // 推荐响应 (responseType = "recommendation")
    private QueryAnalysis queryAnalysis;
    private List<RecommendedProduct> recommendations;

    // 搜索响应 (responseType = "search_result")
    private SearchCriteria searchCriteria;
    private Integer totalCount;
    private List<SearchProduct> products;

    // 图片识别搜索响应 (responseType = "image_search")
    private ImageAnalysis imageAnalysis;
    private List<ImageSearchProduct> imageSearchProducts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "商品推荐的查询分析")
    public static class QueryAnalysis {
        private String detectedCategory;
        private String budget;
        private List<String> specialRequirements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "商品推荐的结果")
    public static class RecommendedProduct {
        private Long productId;
        private String productName;
        private Double price;
        private String brand;
        private String category;
        private List<String> keyFeatures;
        private String reason;
        private String applicableScenario;
        private String rating;
        private Integer salesCount;
        private String mainImageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "商品搜索的查询条件")
    public static class SearchCriteria {
        private String keyword;
        private String brand;
        private String priceRange;
        private String category;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "商品搜索的结果")
    public static class SearchProduct {
        private Long productId;
        private String productCode;
        private String productName;
        private String brand;
        private String category;
        private Double price;
        private String status;
        private Integer salesCount;
        private String stockStatus;
        private String highlight;
        private String mainImageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "图像搜索的查询分析")
    public static class ImageAnalysis {
        private String detectedCategory;
        private String detectedBrand;
        private List<String> visualFeatures;
        private String colorDescription;
        private String shapeDescription;
        private String textOnProduct;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "图像搜索的结果")
    public static class ImageSearchProduct {
        private Long productId;
        private String productCode;
        private String productName;
        private String brand;
        private String category;
        private Double price;
        private String mainImageUrl;
        private Integer salesCount;
        private Double similarity;
        private String matchReason;
    }

    public static AIChatResponse textResponse(String sessionId, String messageId, String content) {
        return AIChatResponse.builder()
                .sessionId(sessionId)
                .messageId(messageId)
                .responseType("text")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static AIChatResponse recommendationResponse(
            String sessionId, String messageId, List<String> sources,
            QueryAnalysis queryAnalysis, List<RecommendedProduct> recommendations) {

        return AIChatResponse.builder()
                .sessionId(sessionId)
                .messageId(messageId)
                .responseType("recommendation")
                .sourcesStr(sources)
                .queryAnalysis(queryAnalysis)
                .recommendations(recommendations)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}