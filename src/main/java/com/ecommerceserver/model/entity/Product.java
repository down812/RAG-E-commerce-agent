package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ecommerceserver.model.dto.ProductToolResult;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@TableName(value = "product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品实体")
public class Product implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品编码（对应JSON中的product_id）")
    private String productCode;

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

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    @Schema(description = "SKU列表")
    private List<ProductSku> skuList;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public ProductToolResult toToolResult() {
        return ProductToolResult.from(this);
    }

    public static List<ProductToolResult> toToolResults(List<Product> products) {
        if (products == null) return java.util.Collections.emptyList();
        return products.stream().map(ProductToolResult::from).collect(Collectors.toList());
    }
}