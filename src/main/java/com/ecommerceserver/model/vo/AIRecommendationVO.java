package com.ecommerceserver.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "商品推荐的返回值")
public class AIRecommendationVO extends BaseVO{

    private AIChatResponse.QueryAnalysis queryAnalysis;

    private List<AIChatResponse.RecommendedProduct> recommendations;

    /*@Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueryAnalysis {
        private String detectedCategory;
        private String budget;
        private List<String> specialRequirements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedProduct {
        private Long productId;
        private String productName;
        private String price;
        private String brand;
        private String category;
        private List<String> keyFeatures;
        private String reason;
        private String applicableScenario;
        private String rating;
        private Integer salesCount;
    }*/
}