package com.ecommerceserver.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SSEStreamEvent {

    public enum EventType {
        message,
        data,
        complete,
        error
    }

    private EventType eventType;
    private String sessionId;
    private String messageId;
    
    private String textChunk;
    private String fullText;
    
    private String responseType;
    private AIChatResponse structuredData;
    
    private Long timestamp;
    private Boolean isComplete;
    private String errorMessage;

    public static SSEStreamEvent text(String sessionId, String messageId, String textChunk) {
        return SSEStreamEvent.builder()
                .eventType(EventType.message)
                .sessionId(sessionId)
                .messageId(messageId)
                .textChunk(textChunk)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static SSEStreamEvent structuredData(String sessionId, String messageId, 
            String responseType, AIChatResponse data) {
        return SSEStreamEvent.builder()
                .eventType(EventType.data)
                .sessionId(sessionId)
                .messageId(messageId)
                .responseType(responseType)
                .structuredData(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static SSEStreamEvent complete(String sessionId, String messageId) {
        return SSEStreamEvent.builder()
                .eventType(EventType.complete)
                .sessionId(sessionId)
                .messageId(messageId)
                .isComplete(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static SSEStreamEvent error(String sessionId, String messageId, String errorMessage) {
        return SSEStreamEvent.builder()
                .eventType(EventType.error)
                .sessionId(sessionId)
                .messageId(messageId)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
