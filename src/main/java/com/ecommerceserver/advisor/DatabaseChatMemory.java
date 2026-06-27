package com.ecommerceserver.advisor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerceserver.model.entity.ChatSummary;
import com.ecommerceserver.model.entity.Log;
import com.ecommerceserver.service.ChatSummaryService;
import com.ecommerceserver.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DatabaseChatMemory implements ChatMemory {

    private final LogService logService;
    private final ChatSummaryService chatSummaryService;

    public static final ConcurrentHashMap<String, ContextData> contextDataCache = new ConcurrentHashMap<>();
    private final Set<String> stoppedMessages = ConcurrentHashMap.newKeySet();

    public static class ContextData {
        public Long userId;
        public String messageId;

        public ContextData(Long userId, String messageId) {
            this.userId = userId;
            this.messageId = messageId;
        }
    }

    @Autowired
    public DatabaseChatMemory(LogService logService, ChatSummaryService chatSummaryService) {
        this.logService = logService;
        this.chatSummaryService = chatSummaryService;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        for (Message message : messages) {
            Log logEntry = new Log(message);

            logEntry.setSessionId(conversationId);
            logEntry.setCreatedAt(new java.util.Date());

            ContextData ctx = contextDataCache.get(conversationId);
            if (ctx != null) {
                logEntry.setUserId(ctx.userId);
                logEntry.setMessageId(ctx.messageId);
            }

            String stopKey = (ctx != null && ctx.messageId != null) ? conversationId + ":" + ctx.messageId : null;
            if (stopKey != null && stoppedMessages.contains(stopKey)
                    && message.getMessageType() == MessageType.ASSISTANT) {
                logEntry.setStatus(0);
                log.info("【停止对话】助手消息标记为中断，sessionId={}, messageId={}", conversationId, ctx.messageId);
            } else {
                logEntry.setStatus(1);
            }

            logService.save(logEntry);
        }
    }

    public void markAsStopped(String sessionId, String messageId) {
        if (sessionId == null || messageId == null) {
            return;
        }
        String stopKey = sessionId + ":" + messageId;
        stoppedMessages.add(stopKey);
        logService.lambdaUpdate()
                .eq(Log::getSessionId, sessionId)
                .eq(Log::getMessageId, messageId)
                .eq(Log::getMessageType, MessageType.ASSISTANT)
                .eq(Log::getStatus, 1)
                .set(Log::getStatus, 0)
                .update();
        log.info("【停止对话】标记消息为中断，sessionId={}, messageId={}", sessionId, messageId);
    }

    public void removeStoppedMark(String sessionId, String messageId) {
        if (sessionId != null && messageId != null) {
            stoppedMessages.remove(sessionId + ":" + messageId);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> messages = new ArrayList<>();

        LambdaQueryWrapper<ChatSummary> summaryWrapper = new LambdaQueryWrapper<>();
        summaryWrapper.eq(ChatSummary::getSessionId, conversationId);
        ChatSummary chatSummary = chatSummaryService.getOne(summaryWrapper);

        if (chatSummary != null && chatSummary.getSummary() != null && !chatSummary.getSummary().isBlank()) {
            String summaryText = "【会话历史摘要】以下是之前对话的核心内容摘要，请结合此摘要理解当前对话：\n" + chatSummary.getSummary();
            messages.add(new SystemMessage(summaryText));
            log.debug("【会话记忆】加载摘要，sessionId={}", conversationId);
        }

        ContextData ctx = contextDataCache.get(conversationId);
        Long userId = (ctx != null) ? ctx.userId : null;

        int limit = Math.min(lastN, 6);

        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Log::getSessionId, conversationId)
                .eq(Log::getStatus, 1)
                .orderByDesc(Log::getCreatedAt)
                .last("LIMIT " + limit);
        if (userId != null) {
            wrapper.eq(Log::getUserId, userId);
        }

        List<Log> logs = logService.list(wrapper);

        java.util.Collections.reverse(logs);

        List<Message> recentMessages = logs.stream()
                .map(Log::toMessage)
                .collect(Collectors.toList());
        messages.addAll(recentMessages);

        return messages;
    }

    @Override
    public void clear(String conversationId) {
        logService.lambdaUpdate()
                .eq(Log::getSessionId, conversationId)
                .remove();
    }
}