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
@Schema(description = "购物车VO")
public class CartVO implements Serializable {
    @Schema(description = "购物车ID")
    private Long cartId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "购物车项列表")
    private List<CartItemVO> cartItems;

    @Schema(description = "商品总数")
    private Integer totalItems;

    @Schema(description = "购物车总金额")
    private BigDecimal totalAmount;
}