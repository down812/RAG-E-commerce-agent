package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "商品SKU DTO")
public class ProductSkuDTO {

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
    private List<ProductSkuAttributeDTO> attributeList;
}