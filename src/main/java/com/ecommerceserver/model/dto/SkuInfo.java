package com.ecommerceserver.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品 SKU（规格）信息，供加入购物车前“按需查规格”使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkuInfo {
    /** SKU ID，加入购物车时作为 skuId 传入 */
    private Long skuId;
    /** SKU 编码 */
    private String skuCode;
    /** SKU 价格 */
    private BigDecimal price;
    /** 规格描述，如“颜色:黑色, 容量:128GB”；无属性时回退为“SKU:编码” */
    private String spec;
}
