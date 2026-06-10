package com.ecommerceserver.service;

import com.ecommerceserver.model.vo.AIChatResponse;
import com.ecommerceserver.model.vo.SSEStreamEvent;
import com.ecommerceserver.utils.StructuredOutputConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIStreamProcessor {

    private final StructuredOutputConverter converter;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    private static class SessionContext {
        final StringBuilder textBuffer = new StringBuilder();
        final AtomicReference<AIChatResponse> structuredData = new AtomicReference<>();
        final AtomicBoolean jsonExtracted = new AtomicBoolean(false);
        final Sinks.Many<SSEStreamEvent> sink;
        long lastUpdateTime;

        SessionContext(String sessionId, String messageId) {
            this.sink = Sinks.many().multicast().onBackpressureBuffer();
            this.lastUpdateTime = System.currentTimeMillis();
        }
    }

    public Flux<SSEStreamEvent> createStream(String sessionId, String messageId) {
        SessionContext context = sessionContexts.computeIfAbsent(
                sessionId + ":" + messageId,
                k -> new SessionContext(sessionId, messageId)
        );
        return context.sink.asFlux();
    }

    public void processChunk(String sessionId, String messageId, String chunk) {
        String key = sessionId + ":" + messageId;
        SessionContext context = sessionContexts.get(key);
        
        if (context == null) {
            context = sessionContexts.computeIfAbsent(key,
                    k -> new SessionContext(sessionId, messageId));
        }

        String previousText = context.textBuffer.toString();
        context.textBuffer.append(chunk);
        context.lastUpdateTime = System.currentTimeMillis();

        SSEStreamEvent textEvent = SSEStreamEvent.text(sessionId, messageId, chunk);
        context.sink.tryEmitNext(textEvent);

        if (!context.jsonExtracted.get()) {
            checkAndExtractStructuredData(context, sessionId, messageId, previousText);
        }
    }

    private void checkAndExtractStructuredData(SessionContext context, 
            String sessionId, String messageId, String previousText) {
        
        String currentText = context.textBuffer.toString();
        
        if (converter.isJsonComplete(currentText)) {
            Optional<AIChatResponse> parsed = converter.tryParseStructuredResponse(currentText);
            
            if (parsed.isPresent()) {
                context.structuredData.set(parsed.get());
                context.jsonExtracted.set(true);

                SSEStreamEvent dataEvent = SSEStreamEvent.structuredData(
                        sessionId,
                        messageId,
                        parsed.get().getResponseType(),
                        parsed.get()
                );
                context.sink.tryEmitNext(dataEvent);
                
                log.info("【结构化数据提取成功】sessionId={}, type={}", 
                        sessionId, parsed.get().getResponseType());
            }
        }
    }

    public void complete(String sessionId, String messageId) {
        String key = sessionId + ":" + messageId;
        SessionContext context = sessionContexts.get(key);
        
        if (context != null) {
            context.sink.tryEmitNext(SSEStreamEvent.complete(sessionId, messageId));
            context.sink.tryEmitComplete();
            sessionContexts.remove(key);
        }
    }

    public void error(String sessionId, String messageId, String error) {
        String key = sessionId + ":" + messageId;
        SessionContext context = sessionContexts.get(key);
        
        if (context != null) {
            context.sink.tryEmitNext(SSEStreamEvent.error(sessionId, messageId, error));
            context.sink.tryEmitComplete();
            sessionContexts.remove(key);
        }
    }

    public AIChatResponse getStructuredData(String sessionId, String messageId) {
        String key = sessionId + ":" + messageId;
        SessionContext context = sessionContexts.get(key);
        return context != null ? context.structuredData.get() : null;
    }

    public void cleanupExpiredSessions(long expirationMs) {
        long now = System.currentTimeMillis();
        sessionContexts.entrySet().removeIf(entry -> 
                now - entry.getValue().lastUpdateTime > expirationMs
        );
    }
}