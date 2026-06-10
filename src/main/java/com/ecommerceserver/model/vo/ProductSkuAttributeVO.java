// 路径: C:\Users\asus\Desktop\Dawn\project\RAG-E-commerce-agent\E-commerceServer\src\main\java\com\ecommerceserver\model\entity\ProductSkuAttribute.java

package com.ecommerceserver.model.vo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSkuAttributeVO implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "属性ID")
    private Long id;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "属性名称")
    private String attrName;

    @Schema(description = "属性值")
    private String attrValue;
}