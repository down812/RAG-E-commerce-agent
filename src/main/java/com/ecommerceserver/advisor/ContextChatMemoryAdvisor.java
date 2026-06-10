package com.ecommerceserver.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class ContextChatMemoryAdvisor implements StreamAroundAdvisor {

    @Override
    public String getName() {
        return "context-memory-advisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        Object userId = request.advisorParams().get("userId");
        Object messageId = request.advisorParams().get("messageId");
        String sessionId = (String) request.advisorParams().get(CHAT_MEMORY_CONVERSATION_ID_KEY);

        if (userId != null && sessionId != null) {
            Long uid = (userId instanceof Long) ? (Long) userId : Long.parseLong(userId.toString());
            String mid = messageId != null ? messageId.toString() : "";
            log.debug("设置上下文, sessionId={}, userId={}, messageId={}", sessionId, uid, mid);
            DatabaseChatMemory.contextDataCache.put(
                    sessionId,
                    new DatabaseChatMemory.ContextData(uid, mid)
            );
        }

        return chain.nextAroundStream(request)
                .doFinally(signalType -> {
                    if (sessionId != null) {
                        log.debug("流终止，清理上下文, signal={}, sessionId={}", signalType, sessionId);
                        DatabaseChatMemory.contextDataCache.remove(sessionId);
                    }
                });
    }
}