package com.ecommerceserver.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "商品搜索的返回值")
public class AISearchResultVO extends BaseVO {

    private AIChatResponse.SearchCriteria searchCriteria;

    private Integer totalCount;

    private List<AIChatResponse.SearchProduct> products;

    /*@Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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
    public static class SearchProduct {
        private Long productId;
        private String productCode;
        private String productName;
        private String brand;
        private String category;
        private String price;
        private String status;
        private Integer salesCount;
        private String stockStatus;
        private String highlight;
    }*/
}