package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class EvaluateDTO {
    //会话ID
    @Schema(description = "会话ID")
    private String sessionId;

    //消息ID
    @Schema(description = "消息ID")
    private String messageId;

    @Schema(description = "用户评价（点赞1、点踩-1，只有这2种值）")
    private Integer rating;

    @Schema(description = "消息反馈的具体信息")
    private String comment;
}
