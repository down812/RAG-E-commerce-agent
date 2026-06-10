package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "SKU属性DTO")
public class ProductSkuAttributeDTO {

    @Schema(description = "属性ID")
    private Long id;

    @Schema(description = "SKU ID")
    private Long skuId;

    @Schema(description = "属性名称")
    private String attrName;

    @Schema(description = "属性值")
    private String attrValue;
}