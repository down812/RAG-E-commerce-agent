package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName(value = "chat_summart")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话摘要实体")
public class ChatSummary {
    @Schema(description = "会话摘要ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "session_id")
    @Schema(description = "会话ID")
    private String sessionId;

    @TableField(value = "summary")
    @Schema(description = "会话摘要")
    private String summary;

    @TableField(value = "created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
