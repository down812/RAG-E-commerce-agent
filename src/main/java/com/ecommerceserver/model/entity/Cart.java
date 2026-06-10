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

@TableName(value = "cart")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "购物车实体")
public class Cart implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "购物车ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "购物车总金额")
    private BigDecimal totalAmount;

    @Schema(description = "商品总数")
    private Integer totalItems;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}