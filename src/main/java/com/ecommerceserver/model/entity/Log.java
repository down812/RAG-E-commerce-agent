package com.ecommerceserver.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author dawn
 * @date 2025-04-10 17:04
 */
@TableName(value = "chat_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log implements Serializable {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    @TableField(value = "user_id")
    private Long userId;
    @TableField(value = "session_id")
    private String sessionId; // 会话ID
    @TableField(value = "message_id")
    private String messageId;    // 消息ID
    @TableField(value = "text")
    private String text;
    @TableField(value = "status")
    private Integer status; // 1-成功 0-被中断
    @TableField(value = "message_type")
    private MessageType messageType;
    @TableField(value = "metadata", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;
    @TableField(value = "created_at")
    private Date createdAt;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public Log(Message message) {
        this.messageType = message.getMessageType();
        this.text = message.getText();
        this.metadata = message.getMetadata();
    }

    public Message toMessage() {
        return switch (messageType) {
            case SYSTEM -> new SystemMessage(text);
            case USER -> new UserMessage(text, List.of(), metadata != null ? metadata : Collections.emptyMap());
            case ASSISTANT -> new AssistantMessage(text, metadata != null ? metadata : Collections.emptyMap(), List.of(), List.of());
            default -> throw new IllegalArgumentException("Unsupported message type: " + messageType);
        };
    }
}