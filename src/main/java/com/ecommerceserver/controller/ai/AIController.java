package com.ecommerceserver.controller.ai;

import com.ecommerceserver.aop.AccessLimit;
import com.ecommerceserver.model.vo.*;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.ChatService;
import com.ecommerceserver.utils.StructuredOutputConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import java.util.List;

@Tag(name = "智慧助手", description = "提供商品推荐、商品对比、商品搜索等AI对话服务")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private StructuredOutputConverter structuredOutputConverter;

    @Autowired
    private ChatService chatService;

    @AccessLimit
    @Operation(summary = "RAG增强对话", description = "基于向量知识库和商品数据库的智能对话服务，支持商品推荐、对比、搜索")
    @PostMapping(value = "/chat/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<org.springframework.http.codec.ServerSentEvent<AiMessage>> chat(
             @RequestParam(required = true) String sessionId,
             @RequestParam(required = true) String messageId,
             @RequestParam(required = false) String content,
             List<MultipartFile> files) {

        return chatService
                .analyse(sessionId, messageId, content, files)
                .map(msg ->
                        org.springframework.http.codec.ServerSentEvent
                                .<AiMessage>builder()
                                .event(msg.type())
                                .data(msg)
                                .build()
                );
    }

    @Operation(summary = "清除会话历史")
    @DeleteMapping("/chat/sessions/{sessionId}")
    public Result<String> clearHistory(@PathVariable String sessionId) {
        chatService.clearHistory(sessionId);
        return Result.success("对话历史已清除");
    }
    
    @Operation(summary = "获取会话列表")
    @GetMapping("/chat/sessions")
    public Result<List<SessionList>> getSessions() {
        return Result.success(chatService.getSessions());
    }
    
    
    @Operation(summary = "获取会话详情")
    @GetMapping("/chat/sessions/{sessionId}")
    public Result<List<SessionInfo>> getSession(@PathVariable String sessionId) {
        List<SessionInfo> sessionInfos = chatService.getSession(sessionId);
        return Result.success(sessionInfos);
    }

    @Operation(summary = "停止对话")
    @PostMapping("/chat/messages/{sessionId}/{messageId}/stop")
    public Result<String> stopChat(@PathVariable String sessionId, @PathVariable String messageId) {
        chatService.stopChat(sessionId, messageId);
        return Result.success("对话已停止");
    }
}