package com.ecommerceserver.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@Data
@Getter
@Setter
@NoArgsConstructor
@Builder
public class EvaluatePageDTO {
    //会话ID
    @Schema(description = "会话ID")
    private String sessionId;

    //消息ID
    @Schema(description = "消息ID")
    private String messageId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户评价（点赞1、点踩-1，只有这2种值）")
    private Integer rating;

    @Schema(description = "当前页码")
    private Integer current;

    @Schema(description = "每页数量")
    private Integer size;
}
