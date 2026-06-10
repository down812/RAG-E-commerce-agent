package com.ecommerceserver.model.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.ecommerceserver.model.entity.ProductSku;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@AllArgsConstructor
@Data
@Getter
@Setter
@NoArgsConstructor
@Builder
public class ProductDTO {
    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品编码（对应JSON中的product_id）")
    private String productCode;

    @NonNull
    @Schema(description = "商品标题")
    private String title;

    @Schema(description = "品牌名称")
    private String brand;

    @Schema(description = "一级分类")
    private String category;

    @Schema(description = "二级分类")
    private String subCategory;

    @Schema(description = "基础价格")
    private BigDecimal basePrice;

    @Schema(description = "商品主图OSS URL")
    private String mainImageUrl;

    @Schema(description = "本地图片路径（原始数据）")
    private String localImagePath;

    @Schema(description = "商品状态：0-下架，1-上架")
    private Integer status;

    @TableField(exist = false)
    @Schema(description = "SKU列表")
    private List<ProductSkuDTO> skuList;
}