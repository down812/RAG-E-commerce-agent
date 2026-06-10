// 路径: C:\Users\asus\Desktop\Dawn\project\RAG-E-commerce-agent\E-commerceServer\src\main\java\com\ecommerceserver\model\entity\ProductSku.java

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
import java.util.List;

@TableName(value = "product_sku")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品SKU实体")
public class ProductSku implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "SKU ID")
    private Long id;

    @Schema(description = "SKU编码")
    private String skuCode;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "SKU价格")
    private BigDecimal price;

    @Schema(description = "SKU状态：0-下架，1-上架")
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    @Schema(description = "属性列表")
    private List<ProductSkuAttribute> attributeList;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
