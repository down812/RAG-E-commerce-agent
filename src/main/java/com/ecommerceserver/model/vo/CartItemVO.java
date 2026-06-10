package com.ecommerceserver.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车项VO")
public class CartItemVO implements Serializable {
    @Schema(description = "购物车项ID")
    private Long cartItemId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品主图URL")
    private String productImage;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "SKU规格描述")
    private String skuSpec;

    @Schema(description = "SKU属性列表")
    private List<ProductSkuAttributeVO> skuAttributes;

    @Schema(description = "商品数量")
    private Integer quantity;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "小计金额")
    private BigDecimal subtotal;
}