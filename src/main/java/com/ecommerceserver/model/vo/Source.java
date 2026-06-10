package com.ecommerceserver.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Source {
    @Schema(description = "文档标题")
    private String title;

    @Schema(description = "文档来源类型")
    private String sourceType;

    @Schema(description = "数据库文档内容")
    private String content;
}
