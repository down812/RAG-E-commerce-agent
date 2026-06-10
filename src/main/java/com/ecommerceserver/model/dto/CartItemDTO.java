package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车项DTO")
public class CartItemDTO {
    @NotNull(message = "购物车项ID不能为空")
    @Schema(description = "购物车项ID")
    private Long cartItemId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "商品数量")
    private Integer quantity;
}