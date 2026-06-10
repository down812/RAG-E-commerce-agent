package com.ecommerceserver.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "图像搜索的返回值")
public class AIImageSearchVO extends BaseVO {

    private AIChatResponse.ImageAnalysis imageAnalysis;

    private List<AIChatResponse.ImageSearchProduct> imageSearchProducts;
}