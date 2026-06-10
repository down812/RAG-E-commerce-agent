package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "cart_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车商品项实体")
public class CartItem implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "购物车项ID")
    private Long id;

    @Schema(description = "购物车ID")
    private Long cartId;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "商品数量")
    private Integer quantity;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "小计金额")
    private BigDecimal subtotal;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}