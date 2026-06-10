package com.ecommerceserver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerceserver.Enum.AIRspEnum;
import com.ecommerceserver.Enum.SourceEnum;
import com.ecommerceserver.advisor.DatabaseChatMemory;
import com.ecommerceserver.constants.AIConstant;
import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.exception.GlobalException;
import com.ecommerceserver.mapper.ProductMapper;
import com.ecommerceserver.model.entity.Log;
import com.ecommerceserver.model.entity.Product;
import com.ecommerceserver.model.vo.*;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.ChatService;
import com.ecommerceserver.service.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.model.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ecommerceserver.Enum.AIRspEnum.*;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final LogService logService;
    private final DatabaseChatMemory databaseChatMemory;
    private final ProductMapper productMapper;

    private final ConcurrentHashMap<String, Sinks.One<Void>> stopSignals = new ConcurrentHashMap<>();

    private static final String TEXT_TAG = "【TEXT】";
    private static final String RESULT_START_TAG = "【RESULT】";
    private static final String RESULT_END_TAG = "【/RESULT】";
    private static final Duration WINDOW_SIZE = Duration.ofMillis(50);

    public Flux<AiMessage> analyse(String sessionId,
                                   String messageId,
                                   String content,
                                   List<MultipartFile> files) {
        //检查消息ID是否已存在
        if (StringUtils.isBlank(messageId)) {
            return Flux.just(new AiMessage("error", AIConstant.MESSAGE_ID_NOT_NULL));
        }
        if (StringUtils.isBlank(sessionId)) {
            return Flux.just(new AiMessage("error", AIConstant.CONVERSATION_ID_NOT_NULL));
        }
        Long logCount = logService.count(new LambdaQueryWrapper<Log>().eq(Log::getMessageId, messageId)
                .eq(Log::getUserId, LoginContext.getUserId())
                .eq(Log::getSessionId, sessionId));
        if (logCount > 0) {
            return Flux.just(new AiMessage("error", AIConstant.MESSAGE_ID_EXIST));
        }


        Long userId = LoginContext.getUserId();
        List<Media> mediaList = buildMediaList(files);

        Sinks.One<Void> stopSignal = Sinks.one();
        String stopKey = sessionId + ":" + messageId;
        stopSignals.put(stopKey, stopSignal);

        StringBuilder rawResponse = new StringBuilder();
        ParseState state = new ParseState();
        StringBuilder pendingBuffer = new StringBuilder();
        java.util.concurrent.atomic.AtomicBoolean resultSentFlag = new java.util.concurrent.atomic.AtomicBoolean(false);

        Flux<String> chunkFlux = chatClient
                .prompt()
                .user(p -> {
                    if (!mediaList.isEmpty()) {
                        p.text(content).media(mediaList.toArray(new Media[0]));
                    } else {
                        p.text(content);
                    }
                })
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
                        .param("userId", userId != null ? userId : 0L)
                        .param("messageId", messageId != null ? messageId : ""))
                .stream()
                .chatResponse()
                .filter(r -> r.getResult().getOutput().getText() != null)
                .map(r -> r.getResult().getOutput().getText())
                .doOnNext(text -> rawResponse.append(text))
                .filter(t -> !t.isEmpty());
        log.debug("【AI对话原始响应】sessionId={}, messageId={}, content={}", sessionId, messageId, rawResponse.toString());

        return chunkFlux
                .window(WINDOW_SIZE)
                .flatMap(window -> window.collectList().map(list -> String.join("", list)))
                .filter(combined -> !combined.isEmpty())
                .flatMap(combined -> processBatch(combined, pendingBuffer, state, sessionId, resultSentFlag))
                .concatWith(Flux.defer(() -> sendFinalResult(sessionId, messageId, rawResponse.toString(), resultSentFlag)))
                .takeUntilOther(stopSignal.asMono())
                .doFinally(signalType -> {
                    stopSignals.remove(stopKey);
                    databaseChatMemory.removeStoppedMark(sessionId, messageId);
                    log.info("【AI对话结束】sessionId={}, messageId={}, signal={}", sessionId, messageId, signalType);
                })
                .onErrorResume(e -> {
                    log.error("【AI对话异常】sessionId={}", sessionId, e);
                    return Flux.just(new AiMessage("error", "AI服务暂时不可用，原因：" + e.getMessage()));
                });
    }

    @Override
    public void clearHistory(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new GlobalException(Result.error(AIConstant.CONVERSATION_ID_NOT_NULL));
        }
        logService.remove(new LambdaQueryWrapper<Log>().eq(Log::getSessionId, sessionId));
    }

    @Override
    public List<SessionInfo> getSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new GlobalException(Result.error(AIConstant.CONVERSATION_ID_NOT_NULL));
        }

        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Log::getSessionId, sessionId).orderByAsc(Log::getCreatedAt);
        List<Log> logs = logService.list(wrapper);

        List<SessionInfo> sessionInfos = new ArrayList<>();
        for (Log log : logs) {
            if (log.getText().contains(TEXT_TAG) || log.getText().contains(RESULT_START_TAG)) {
                String content = extractPureText(log.getText());
                AIChatResponse result = extractStructuredResult(log.getText());
                Object obj = null;
                if (result != null ) {
                    result.setSessionId(log.getSessionId());
                    result.setMessageId(log.getMessageId());
                    result.setTimestamp(log.getCreatedAt().getTime());
                    result.setAnswer(content);
                    if (result.getSourcesStr() != null) {
                        List<Source> sources = extraceSources(sessionId, result);
                        result.setSources(sources);
                    }
                    switch (AIRspEnum.fromCode(result.getResponseType())) {
                        case RECOMMENDATION -> obj = BeanUtil.copyProperties(result, AIRecommendationVO.class);
                        case SEARCHRESULT ->   obj = BeanUtil.copyProperties(result, AISearchResultVO.class);
                        case IMAGE_SEARCH -> obj = BeanUtil.copyProperties(result, AIImageSearchVO.class);
                        default -> obj = result;
                    }
                }

                try {
                    sessionInfos.add(SessionInfo.builder()
                            .sessionId(sessionId)
                            .messageId(log.getMessageId())
                            .content(content)
                            .result(obj)
                            .messageType(log.getMessageType().name())
                            .createdAt(log.getCreatedAt()).build());
                } catch (Exception e) {
                    throw new GlobalException(Result.error(AIConstant.JSON_PARSE_FAILED));
                }
            }else {
                sessionInfos.add(SessionInfo.builder()
                        .sessionId(sessionId)
                        .messageId(log.getMessageId())
                        .content(log.getText())
                        .messageType(log.getMessageType().name())
                        .createdAt(log.getCreatedAt()).build());
            }
        }
        return sessionInfos;
    }

    @Override
    public List<SessionList> getSessions() {
        Long userId = LoginContext.getUserId();

        List<Log> logs = logService.list(new LambdaQueryWrapper<Log>().eq(Log::getUserId, userId).orderByAsc(Log::getCreatedAt));

        List<SessionList> sessionLists = new ArrayList<>();
        Map<String, SessionList> sessionMap = new HashMap<>();

        for (Log log : logs) {
            if (StringUtils.isEmpty(log.getSessionId()) || log.getMessageType() != MessageType.USER
            || sessionMap.containsKey(log.getSessionId())) {
                continue;
            }
            sessionMap.put(log.getSessionId(), SessionList.builder().sessionId(log.getSessionId()).title(log.getText()).createdAt(log.getCreatedAt()).build());
            sessionLists.add(SessionList.builder()
                    .sessionId(log.getSessionId())
                    .title(log.getText())
                    .createdAt(log.getCreatedAt()).build());
        }

        return sessionLists;
    }

    @Override
    public void stopChat(String sessionId, String messageId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new GlobalException(Result.error(AIConstant.CONVERSATION_ID_NOT_NULL));
        }
        if (StringUtils.isEmpty(messageId)) {
            throw new GlobalException(Result.error(AIConstant.MESSAGE_ID_NOT_NULL));
        }
        String stopKey = sessionId + ":" + messageId;
        Sinks.One<Void> stopSignal = stopSignals.get(stopKey);
        if (stopSignal == null) {
            log.warn("【停止对话】未找到活跃的对话流，sessionId={}, messageId={}", sessionId, messageId);
            return;
        }
        Sinks.EmitResult result = stopSignal.tryEmitEmpty();
        if (result.isFailure()) {
            log.warn("【停止对话失败】sessionId={}, messageId={}, result={}", sessionId, messageId, result);
            throw new GlobalException(Result.error(AIConstant.STOP_CONVERSATION_FAILED));
        }
        databaseChatMemory.markAsStopped(sessionId, messageId);
        stopSignals.remove(stopKey);
        log.info("【停止对话成功】sessionId={}, messageId={}", sessionId, messageId);
    }

    private Flux<AiMessage> processBatch(String combined,
                                         StringBuilder pendingBuffer,
                                         ParseState state,
                                         String sessionId,
                                         java.util.concurrent.atomic.AtomicBoolean resultSentFlag) {
        String accumulated = pendingBuffer.toString() + combined;
        pendingBuffer.setLength(0);
        List<AiMessage> messages = new ArrayList<>();
        StringBuilder textBuffer = new StringBuilder();

        while (!accumulated.isEmpty()) {
            if (!state.resultSectionStarted) {
                int textIdx = accumulated.indexOf(TEXT_TAG);
                int resultIdx = accumulated.indexOf(RESULT_START_TAG);

                if (textIdx == -1 && resultIdx == -1) {
                    int safeEnd = findSafeEnd(accumulated, TEXT_TAG, RESULT_START_TAG);
                    if (safeEnd > 0) {
                        textBuffer.append(accumulated, 0, safeEnd);
                        state.textSectionStarted = true;
                    }
                    if (safeEnd < accumulated.length()) {
                        pendingBuffer.append(accumulated.substring(safeEnd));
                    }
                    break;
                }

                if (resultIdx != -1 && (textIdx == -1 || resultIdx < textIdx)) {
                    state.resultSectionStarted = true;
                    if (textIdx >= 0 && state.textSectionStarted) {
                        textBuffer.append(accumulated, 0, resultIdx);
                    }
                    pendingBuffer.append(accumulated.substring(resultIdx + RESULT_START_TAG.length()));
                    break;
                }

                if (textIdx >= 0) {
                    state.textSectionStarted = true;
                    textBuffer.append(accumulated, 0, textIdx);
                    accumulated = accumulated.substring(textIdx + TEXT_TAG.length());
                }
            } else {
                int endIdx = accumulated.indexOf(RESULT_END_TAG);
                if (endIdx >= 0) {
                    String resultContent = accumulated.substring(0, endIdx);
                    pendingBuffer.append(resultContent);
                    accumulated = accumulated.substring(endIdx + RESULT_END_TAG.length());
                    state.resultSectionStarted = false;

                    if (!state.resultSent) {
                        String json = cleanJsonContent(pendingBuffer.toString());
                        try {
                            AIChatResponse earlyResult = objectMapper.readValue(json, AIChatResponse.class);
                            if (earlyResult != null) {
                                earlyResult = validateAndCorrectProductData(earlyResult);
                                List<Source> sources = extraceSources(sessionId, earlyResult);
                                earlyResult.setSources(sources);
                                earlyResult.setSessionId(sessionId);
                                state.resultSent = true;
                                resultSentFlag.set(true);
                                switch (AIRspEnum.fromCode(earlyResult.getResponseType())) {
                                    case RECOMMENDATION:
                                        messages.add(new AiMessage("result", BeanUtil.copyProperties(earlyResult, AIRecommendationVO.class)));
                                        break;
                                    case SEARCHRESULT:
                                        messages.add(new AiMessage("result", BeanUtil.copyProperties(earlyResult, AISearchResultVO.class)));
                                        break;
                                    case IMAGE_SEARCH:
                                        messages.add(new AiMessage("result", BeanUtil.copyProperties(earlyResult, AIImageSearchVO.class)));
                                        break;
                                    default:
                                        messages.add(new AiMessage("result", earlyResult));
                                }
                            }
                        } catch (Exception e) {
                            log.debug("提前解析RESULT失败，将在最终阶段重试: {}", e.getMessage());
                        }
                        pendingBuffer.setLength(0);
                    }
                } else {
                    pendingBuffer.append(accumulated);
                    break;
                }
            }
        }

        if (textBuffer.length() > 0) {
            messages.add(new AiMessage("content", textBuffer.toString()));
        }
        return messages.isEmpty() ? Flux.empty() : Flux.fromIterable(messages);
    }

    private Flux<AiMessage> sendFinalResult(String sessionId, String messageId, String raw,
                                            java.util.concurrent.atomic.AtomicBoolean resultSentFlag) {
        if (resultSentFlag.get()) {
            return Flux.just(new AiMessage("done", null));
        }

        AIChatResponse result = extractStructuredResult(raw);
        
        if (result != null) {
            result = validateAndCorrectProductData(result);
            List<Source> sources = extraceSources(sessionId, result);
            result.setAnswer(raw != null ? extractPureText(raw) : "");
            result.setSources(sources);
            result.setSessionId(sessionId);
            result.setMessageId(messageId);
            result.setTimestamp(System.currentTimeMillis());
            switch (AIRspEnum.fromCode(result.getResponseType())) {
                case RECOMMENDATION:
                    AIRecommendationVO recommendationVO = BeanUtil.copyProperties(result, AIRecommendationVO.class);
                    return Flux.just(new AiMessage("result", recommendationVO), new AiMessage("done", null));
                case SEARCHRESULT:
                    AISearchResultVO aiSearchResultVO = BeanUtil.copyProperties(result, AISearchResultVO.class);
                    return Flux.just(new AiMessage("result", aiSearchResultVO), new AiMessage("done", null));
                case IMAGE_SEARCH:
                    AIImageSearchVO aiImageSearchVO = BeanUtil.copyProperties(result, AIImageSearchVO.class);
                    return Flux.just(new AiMessage("result", aiImageSearchVO), new AiMessage("done", null));
                default:
                    return Flux.just(new AiMessage("result", result), new AiMessage("done", null));
            }
        }
        
        String cleanText = extractPureText(raw);
        if (!cleanText.isEmpty()) {
            return Flux.just(new AiMessage("content", cleanText), new AiMessage("done", null));
        }
        return Flux.just(new AiMessage("done", null));
    }

    @NotNull
    private List<Source> extraceSources(String sessionId, AIChatResponse result) {
        List<String> aiSources = result.getSourcesStr() != null ? result.getSourcesStr() : new ArrayList<>();

        List<Source> sources = new ArrayList<>();
        for (String source : aiSources) {
            int startIdx = source.indexOf("【来源：");
            int endIdx = source.indexOf("】");
            if (startIdx >= 0 && endIdx > startIdx) {
                String title = source.substring(startIdx + 4, endIdx);
                String content = source.substring(endIdx + 1);
                sources.add(Source.builder().title(title).sourceType(SourceEnum.DATABASE.getSourceType()).content(content).build());
            } else {
                sources.add(Source.builder().title("未知来源").sourceType(SourceEnum.DATABASE.getSourceType()).content(source).build());
            }
        }
        return sources;
    }

   /* private RAGSource extractRAGSource(String ragSource) {
        if (ragSource == null || ragSource.isEmpty()) {
            return null;
        }
        
        try {
            String content = ragSource;
            int sourceIdx = content.indexOf("【来源：");
            if (sourceIdx >= 0) {
                content = content.substring(sourceIdx + 5);
                int endIdx = content.indexOf("】");
                if (endIdx > 0) {
                    content = content.substring(endIdx + 1);
                }
            }
            
            String productInfo = "";
            String marketingDescription = "";
            List<RAGSource.Faq> faqList = new ArrayList<>();
            List<RAGSource.userReviews> reviewList = new ArrayList<>();
            
            int productInfoIdx = content.indexOf("【商品信息】");
            int marketingIdx = content.indexOf("【营销描述】");
            int faqIdx = content.indexOf("【官方问答】");
            int reviewIdx = content.indexOf("【用户评价】");
            
            if (productInfoIdx >= 0) {
                int end = findNextSectionStart(content, productInfoIdx);
                productInfo = content.substring(productInfoIdx + 6, end > 0 ? end : content.length()).trim();
            }
            
            if (marketingIdx >= 0) {
                int end = findNextSectionStart(content, marketingIdx);
                marketingDescription = content.substring(marketingIdx + 6, end > 0 ? end : content.length()).trim();
            }
            
            if (faqIdx >= 0) {
                int end = findNextSectionStart(content, faqIdx);
                String faqSection = content.substring(faqIdx + 6, end > 0 ? end : content.length());
                faqList = parseFaqSection(faqSection);
            }
            
            if (reviewIdx >= 0) {
                int end = findNextSectionStart(content, reviewIdx);
                String reviewSection = content.substring(reviewIdx + 6, end > 0 ? end : content.length());
                reviewList = parseReviewSection(reviewSection);
            }
            
            return RAGSource.builder()
                    .productInfo(productInfo)
                    .marketingDescription(marketingDescription)
                    .officialFAQ(faqList)
                    .userReviews(reviewList)
                    .build();
                    
        } catch (Exception e) {
            log.warn("解析RAGSource失败: {}", e.getMessage());
            return null;
        }
    }*/
    
    /*private int findNextSectionStart(String content, int currentIdx) {
        String[] sections = {"【商品信息】", "【营销描述】", "【官方问答】", "【用户评价】"};
        int nextIdx = -1;
        for (String section : sections) {
            int idx = content.indexOf(section, currentIdx + 6);
            if (idx > 0 && (nextIdx < 0 || idx < nextIdx)) {
                nextIdx = idx;
            }
        }
        return nextIdx;
    }
    
    private List<RAGSource.Faq> parseFaqSection(String section) {
        List<RAGSource.Faq> faqList = new ArrayList<>();
        if (section == null || section.isEmpty()) {
            return faqList;
        }
        
        String[] lines = section.split("\n");
        String currentQuestion = "";
        String currentAnswer = "";
        boolean readingQuestion = false;
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("问：")) {
                if (!currentQuestion.isEmpty() && !currentAnswer.isEmpty()) {
                    faqList.add(RAGSource.Faq.builder()
                            .question(currentQuestion.replace("问：", ""))
                            .answer(currentAnswer.replace("答：", ""))
                            .build());
                }
                currentQuestion = line;
                currentAnswer = "";
                readingQuestion = true;
            } else if (line.startsWith("答：")) {
                currentAnswer = line;
                readingQuestion = false;
            } else if (!line.isEmpty() && !readingQuestion && !currentAnswer.isEmpty()) {
                currentAnswer += " " + line;
            }
        }
        
        if (!currentQuestion.isEmpty() && !currentAnswer.isEmpty()) {
            faqList.add(RAGSource.Faq.builder()
                    .question(currentQuestion.replace("问：", ""))
                    .answer(currentAnswer.replace("答：", ""))
                    .build());
        }
        
        return faqList;
    }
    
    private List<RAGSource.userReviews> parseReviewSection(String section) {
        List<RAGSource.userReviews> reviewList = new ArrayList<>();
        if (section == null || section.isEmpty()) {
            return reviewList;
        }
        
        String[] lines = section.split("\n");
        String nickname = "";
        String rating = "";
        String comment = "";
        
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("- 用户：")) {
                if (!nickname.isEmpty() || !comment.isEmpty()) {
                    reviewList.add(RAGSource.userReviews.builder()
                            .nickname(nickname.replace("用户：", ""))
                            .rating(parseRatingToInt(rating.replace("评分：", "").replace("星", "")))
                            .content(comment.replace("评价：", ""))
                            .build());
                }
                nickname = line;
                rating = "";
                comment = "";
            } else if (line.startsWith("评分：")) {
                rating = line;
            } else if (line.startsWith("评价：")) {
                comment = line;
            } else if (!line.isEmpty() && !comment.isEmpty()) {
                comment += " " + line;
            }
        }
        
        if (!nickname.isEmpty() || !comment.isEmpty()) {
            reviewList.add(RAGSource.userReviews.builder()
                    .nickname(nickname.replace("用户：", ""))
                    .rating(parseRatingToInt(rating.replace("评分：", "").replace("星", "")))
                    .content(comment.replace("评价：", ""))
                    .build());
        }
        
        return reviewList;
    }*/

    private Integer parseRatingToInt(String ratingStr) {
        if (ratingStr == null || ratingStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(ratingStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int findSafeEnd(String accumulated, String... tags) {
        int safeEnd = accumulated.length();
        for (String tag : tags) {
            for (int len = Math.min(tag.length() - 1, accumulated.length()); len >= 1; len--) {
                if (accumulated.endsWith(tag.substring(0, len))) {
                    safeEnd = Math.min(safeEnd, accumulated.length() - len);
                    break;
                }
            }
        }
        return safeEnd;
    }

    private static class ParseState {
        boolean textSectionStarted = false;
        boolean resultSectionStarted = false;
        boolean resultSent = false;
    }

    private AIChatResponse extractStructuredResult(String raw) {
        int start = raw.indexOf(RESULT_START_TAG);
        int end = raw.indexOf(RESULT_END_TAG);
        if (start < 0) return null;

        String json = start >= 0 ? raw.substring(start + RESULT_START_TAG.length(),
                end > start ? end : raw.length()) : "";
        json = cleanJsonContent(json);

        try {
            return objectMapper.readValue(json, AIChatResponse.class);
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String cleanJsonContent(String json) {
        if (json == null || json.isEmpty()) return json;
        String cleaned = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "")
                .replaceAll("```", "").replaceAll("\\s*【/RESULT】\\s*", "").trim();
        int s = cleaned.indexOf("{");
        int e = cleaned.lastIndexOf("}");
        return s >= 0 && e > s ? cleaned.substring(s, e + 1) : cleaned.trim();
    }

    private String extractPureText(String full) {
        if (full == null || full.isEmpty()) return "";
        String text = full;
        int textStart = text.indexOf(TEXT_TAG);
        if (textStart >= 0) {
            int resultStart = text.indexOf(RESULT_START_TAG);
            text = resultStart > textStart ?
                    text.substring(textStart + TEXT_TAG.length(), resultStart) :
                    text.substring(textStart + TEXT_TAG.length());
        }
        return text.replaceAll("```json\\s*", "").replaceAll("```", "")
                .replaceAll(RESULT_START_TAG, "").replaceAll(RESULT_END_TAG, "").trim();
    }

    private List<Media> buildMediaList(List<MultipartFile> files) {
        List<Media> list = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    try {
                        if (f.getContentType() != null) {
                            list.add(new Media(MimeType.valueOf(f.getContentType()), f.getResource()));
                        }
                    } catch (Exception e) {
                        log.warn("文件格式解析失败: {}", e.getMessage());
                    }
                }
            }
        }
        return list;
    }

    private AIChatResponse validateAndCorrectProductData(AIChatResponse result) {
        String responseType = result.getResponseType();
        if (responseType == null) return result;

        Map<Long, Product> productCache = new HashMap<>();

        switch (AIRspEnum.fromCode(responseType)) {
            case RECOMMENDATION:
                if (result.getRecommendations() != null) {
                    batchLoadProducts(result.getRecommendations().stream()
                            .map(AIChatResponse.RecommendedProduct::getProductId)
                            .toList(),
                            result.getRecommendations().stream()
                                    .map(AIChatResponse.RecommendedProduct::getProductName)
                                    .toList(),
                            productCache);
                    for (AIChatResponse.RecommendedProduct rec : result.getRecommendations()) {
                        Product dbProduct = findCachedProduct(rec.getProductId(), rec.getProductName(), productCache);
                        if (dbProduct != null) {
                            rec.setProductId(dbProduct.getId());
                            rec.setProductName(dbProduct.getTitle());
                            rec.setPrice(dbProduct.getBasePrice() != null ? dbProduct.getBasePrice().doubleValue() : null);
                            rec.setBrand(dbProduct.getBrand());
                            rec.setCategory(dbProduct.getCategory());
                            rec.setMainImageUrl(dbProduct.getMainImageUrl());
                        } else {
                            log.warn("【数据校验】推荐商品不存在于数据库: productId={}, productName={}",
                                    rec.getProductId(), rec.getProductName());
                        }
                    }
                }
                break;
            case SEARCHRESULT:
                if (result.getProducts() != null) {
                    batchLoadProducts(result.getProducts().stream()
                            .map(AIChatResponse.SearchProduct::getProductId)
                            .toList(),
                            result.getProducts().stream()
                                    .map(AIChatResponse.SearchProduct::getProductName)
                                    .toList(),
                            productCache);
                    for (AIChatResponse.SearchProduct sp : result.getProducts()) {
                        Product dbProduct = findCachedProduct(sp.getProductId(), sp.getProductName(), productCache);
                        if (dbProduct != null) {
                            sp.setProductId(dbProduct.getId());
                            sp.setProductCode(dbProduct.getProductCode());
                            sp.setProductName(dbProduct.getTitle());
                            sp.setBrand(dbProduct.getBrand());
                            sp.setCategory(dbProduct.getCategory());
                            sp.setPrice(dbProduct.getBasePrice() != null ? dbProduct.getBasePrice().doubleValue() : null);
                            sp.setStatus(dbProduct.getStatus() != null ? (dbProduct.getStatus() == 1 ? "上架" : "下架") : null);
                            sp.setMainImageUrl(dbProduct.getMainImageUrl());
                        }
                    }
                }
                break;
            case IMAGE_SEARCH:
                if (result.getImageSearchProducts() != null) {
                    batchLoadProducts(result.getImageSearchProducts().stream()
                            .map(AIChatResponse.ImageSearchProduct::getProductId)
                            .toList(),
                            result.getImageSearchProducts().stream()
                                    .map(AIChatResponse.ImageSearchProduct::getProductName)
                                    .toList(),
                            productCache);
                    for (AIChatResponse.ImageSearchProduct isp : result.getImageSearchProducts()) {
                        Product dbProduct = findCachedProduct(isp.getProductId(), isp.getProductName(), productCache);
                        if (dbProduct != null) {
                            isp.setProductId(dbProduct.getId());
                            isp.setProductCode(dbProduct.getProductCode());
                            isp.setProductName(dbProduct.getTitle());
                            isp.setBrand(dbProduct.getBrand());
                            isp.setCategory(dbProduct.getCategory());
                            isp.setPrice(dbProduct.getBasePrice() != null ? dbProduct.getBasePrice().doubleValue() : null);
                            isp.setMainImageUrl(dbProduct.getMainImageUrl());
                        }
                    }
                }
                break;
            default:
                break;
        }
        return result;
    }

    private void batchLoadProducts(List<Long> productIds, List<String> productNames, Map<Long, Product> cache) {
        List<Long> validIds = productIds.stream().filter(id -> id != null).distinct().toList();
        if (!validIds.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(validIds);
            for (Product p : products) {
                cache.put(p.getId(), p);
            }
        }
        List<String> validNames = productNames.stream()
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .filter(name -> cache.values().stream().noneMatch(p -> name.equals(p.getTitle())))
                .toList();
        if (!validNames.isEmpty()) {
            QueryWrapper<Product> nameWrapper = new QueryWrapper<>();
            nameWrapper.and(w -> {
                for (int i = 0; i < validNames.size(); i++) {
                    if (i == 0) {
                        w.eq("title", validNames.get(i));
                    } else {
                        w.or().eq("title", validNames.get(i));
                    }
                }
            });
            nameWrapper.last("LIMIT " + validNames.size());
            List<Product> nameResults = productMapper.selectList(nameWrapper);
            for (Product p : nameResults) {
                cache.put(p.getId(), p);
            }
        }
    }

    private Product findCachedProduct(Long productId, String productName, Map<Long, Product> cache) {
        if (productId != null && cache.containsKey(productId)) {
            return cache.get(productId);
        }
        if (productName != null) {
            return cache.values().stream()
                    .filter(p -> productName.equals(p.getTitle()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}