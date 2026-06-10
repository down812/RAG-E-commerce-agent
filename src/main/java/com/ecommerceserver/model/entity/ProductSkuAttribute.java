// 路径: C:\Users\asus\Desktop\Dawn\project\RAG-E-commerce-agent\E-commerceServer\src\main\java\com\ecommerceserver\model\entity\ProductSkuAttribute.java

package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName(value = "product_sku_attribute")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "SKU属性实体")
public class ProductSkuAttribute implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "属性ID")
    private Long id;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "属性名称")
    private String attrName;

    @Schema(description = "属性值")
    private String attrValue;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}