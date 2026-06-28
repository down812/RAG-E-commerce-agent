package com.ecommerceserver.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionInfo {
    private String sessionId;

    private String messageId;

    private String content;

    private Object result;

    private String messageType;

    private Date createdAt;

    private Map<String, Object> metadata;
}
