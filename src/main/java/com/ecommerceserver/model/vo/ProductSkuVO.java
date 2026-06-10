// 路径: C:\Users\asus\Desktop\Dawn\project\RAG-E-commerce-agent\E-commerceServer\src\main\java\com\ecommerceserver\model\entity\ProductSku.java

package com.ecommerceserver.model.vo;

import com.baomidou.mybatisplus.annotation.*;
import com.ecommerceserver.model.entity.ProductSkuAttribute;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSkuVO implements Serializable {

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

    @Schema(description = "属性列表")
    private List<ProductSkuAttributeVO> attributeList;
}
