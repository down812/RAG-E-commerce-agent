package com.ecommerceserver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerceserver.Enum.AIRspEnum;
import com.ecommerceserver.mapper.ChatSummaryMapper;
import com.ecommerceserver.model.entity.ChatSummary;
import com.ecommerceserver.model.entity.Log;
import com.ecommerceserver.model.vo.AIChatResponse;
import com.ecommerceserver.model.vo.AIImageSearchVO;
import com.ecommerceserver.model.vo.AIRecommendationVO;
import com.ecommerceserver.model.vo.AISearchResultVO;
import com.ecommerceserver.service.ChatSummaryService;
import com.ecommerceserver.service.LogService;
import com.ecommerceserver.utils.ExtractUtils;
import jdk.jfr.Label;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatSummaryServiceImpl extends ServiceImpl<ChatSummaryMapper, ChatSummary> implements ChatSummaryService {
    @Autowired
    @Lazy
    private ChatClient summaryChatClient;

    private final LogService logService;

    private static final String TEXT_TAG = "【TEXT】";
    private static final String RESULT_START_TAG = "【RESULT】";
    private static final String RESULT_END_TAG = "【/RESULT】";


    // 摘要生成节流：每累计 SUMMARY_EVERY_N 条助手消息才重新生成一次摘要，
    // 避免每轮对话都触发一次完整 LLM 调用，降低整体负载与延迟。
    // 不影响上下文质量：DatabaseChatMemory 每轮始终加载“最近6条消息 + 上一份摘要”，跳过轮次的近期消息仍在。
    private static final long SUMMARY_EVERY_N = 3;

    @Override
    @Async
    public void generateSummaryAsync(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("[会话摘要] sessionId为空，跳过摘要生成");
            return;
        }

        try {
            // 节流判断：仅当本会话助手消息数为 SUMMARY_EVERY_N 的整数倍时才生成
            long assistantCount = logService.count(new LambdaQueryWrapper<Log>()
                    .eq(Log::getSessionId, sessionId)
                    .eq(Log::getStatus, 1)
                    .eq(Log::getMessageType, MessageType.ASSISTANT));
            if (assistantCount > 0 && assistantCount % SUMMARY_EVERY_N != 0) {
                log.debug("[会话摘要] sessionId={} 助手消息数={}，未达节流阈值，跳过本轮摘要", sessionId, assistantCount);
                return;
            }

            //获取最新的6条对话
            LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Log::getSessionId, sessionId)
                    .eq(Log::getStatus, 1)
                    .orderByAsc(Log::getCreatedAt)
                    .last("LIMIT " + 6);
            List<Log> logs = logService.list(wrapper);

            if (logs == null || logs.isEmpty()) {
                log.warn("[会话摘要] sessionId={} 无会话记录，跳过摘要生成", sessionId);
                return;
            }


            String conversationHistory = logs.stream()
                    .map(log -> {
                        String role = log.getMessageType().name();
                        StringBuilder sb = new StringBuilder();
                        Object obj = null;

                        if (log.getMessageType() == MessageType.ASSISTANT) {
                            if (log.getText().contains(TEXT_TAG)) {
                                sb.append(ExtractUtils.extractPureText(log.getText()));
                            }
                            if (log.getText().contains(RESULT_START_TAG)) {
                                AIChatResponse result = ExtractUtils.extractStructuredResult(log.getText());
                                if (result != null) {
                                    switch (AIRspEnum.fromCode(result.getResponseType())) {
                                        case RECOMMENDATION -> {
                                            obj = BeanUtil.copyProperties(result, AIRecommendationVO.class);
                                            sb.append("推荐的物品的商品标题和基础价格：");
                                            if (obj != null && ((AIRecommendationVO) obj).getRecommendations() != null) {
                                                sb.append(((AIRecommendationVO) obj).getRecommendations().stream().map(product -> product.getProductName() + " " + product.getPrice() + "; ").collect(Collectors.joining()));
                                            }
                                        }
                                        case SEARCHRESULT -> {
                                            obj = BeanUtil.copyProperties(result, AISearchResultVO.class);
                                            sb.append("搜索结果：");
                                            if (obj != null && ((AISearchResultVO) obj).getProducts() != null) {
                                                sb.append(((AISearchResultVO) obj).getProducts().stream().map(product -> product.getProductName() + " " + product.getPrice() + "; ").collect(Collectors.joining()));
                                            }
                                        }
                                        case IMAGE_SEARCH -> {
                                            obj = BeanUtil.copyProperties(result, AIImageSearchVO.class);
                                            sb.append("图像搜索结果：");
                                            if (obj != null && ((AIImageSearchVO) obj).getImageSearchProducts() != null) {
                                                sb.append(((AIImageSearchVO) obj).getImageSearchProducts().stream().map(product -> product.getProductName() + " " + product.getPrice() + "; ").collect(Collectors.joining()));
                                            }
                                        }
                                        default -> obj = result;
                                    }
                                }
                            }
                            if (!log.getText().contains(TEXT_TAG) &&  log.getText().contains(RESULT_START_TAG) && log.getText().contains(RESULT_END_TAG)) {
                                sb.append(log.getText());
                            }
                        }
                        if (log.getMessageType() == MessageType.USER) {
                            sb.append(log.getText());
                        }
                        return role + ": " + sb.toString();
                    })
                    .collect(Collectors.joining("\n"));

            LambdaQueryWrapper<ChatSummary> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(ChatSummary::getSessionId, sessionId);
            ChatSummary existing = this.getOne(existWrapper);

            if (existing != null) {
                conversationHistory += "【会话摘要】\n" + existing.getSummary() + "\n";
            }

            int maxChars = 8000;
            if (conversationHistory.length() > maxChars) {
                int preserveCount = (int) (logs.size() * 0.7);
                List<Log> preservedLogs = logs.subList(Math.max(0, logs.size() - preserveCount), logs.size());
                String recentHistory = preservedLogs.stream()
                        .map(log -> log.getMessageType().name() + ": " + log.getText())
                        .collect(Collectors.joining("\n"));
                int recentLength = recentHistory.length();
                if (recentLength > maxChars) {
                    conversationHistory = recentHistory.substring(0, maxChars) + "\n...(会话后半部分已截断)";
                } else {
                    conversationHistory = recentHistory;
                }
                log.warn("[会话摘要] sessionId={} 会话内容过长，保留最近{}条消息进行摘要", sessionId, preservedLogs.size());
            }

            String summary = summaryChatClient.prompt()
                    .user("请根据以下对话历史生成摘要：\n" + conversationHistory)
                    .call()
                    .content();

            if (summary == null || summary.isBlank()) {
                log.warn("[会话摘要] sessionId={} 生成摘要为空，跳过保存", sessionId);
                return;
            }

            ChatSummary chatSummary = ChatSummary.builder()
                    .sessionId(sessionId)
                    .summary(summary)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();



            if (existing != null) {
                existing.setSummary(summary);
                existing.setUpdatedAt(LocalDateTime.now());
                this.updateById(existing);
                log.info("[会话摘要] sessionId={} 摘要已更新", sessionId);
            } else {
                this.save(chatSummary);
                log.info("[会话摘要] sessionId={} 摘要已保存", sessionId);
            }
        } catch (Exception e) {
            log.error("[会话摘要] sessionId={} 生成摘要失败", sessionId, e);
        }
    }
}
