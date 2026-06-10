package com.ecommerceserver.model.dto;

import com.ecommerceserver.model.entity.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductToolResult {
    private Long id;
    private String productCode;
    private String title;
    private String brand;
    private String category;
    private String subCategory;
    private BigDecimal basePrice;
    private Integer salesCount;
    private Integer status;
    private String mainImageUrl;

    public static ProductToolResult from(Product src) {
        if (src == null) return null;
        return ProductToolResult.builder()
                .id(src.getId())
                .productCode(src.getProductCode())
                .title(src.getTitle())
                .brand(src.getBrand())
                .category(src.getCategory())
                .subCategory(src.getSubCategory())
                .basePrice(src.getBasePrice())
                .status(src.getStatus())
                .mainImageUrl(src.getMainImageUrl())
                .build();
    }
}