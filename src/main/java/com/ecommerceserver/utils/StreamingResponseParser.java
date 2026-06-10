package com.ecommerceserver.utils;

import com.ecommerceserver.model.vo.AIChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreamingResponseParser {

    private final ObjectMapper objectMapper;

    private static final String TEXT_START = "【TEXT】";
    private static final String RESULT_START = "【RESULT】";
    private static final String RESULT_END = "【/RESULT】";
    private static final String JSON_CODE_BLOCK = "```json";
    private static final String CODE_BLOCK_END = "```";

    public static class ParseState {
        private StringBuilder textBuffer = new StringBuilder();
        private StringBuilder resultBuffer = new StringBuilder();
        private StringBuilder pendingText = new StringBuilder();
        private boolean inResultSection = false;
        private boolean textSectionStarted = false;
        private int resultDepth = 0;
        private List<String> contentChunks = new ArrayList<>();
        private List<AIChatResponse> resultChunks = new ArrayList<>();

        public List<String> getContentChunks() {
            return contentChunks;
        }

        public List<AIChatResponse> getResultChunks() {
            return resultChunks;
        }

        public StringBuilder getTextBuffer() {
            return textBuffer;
        }
    }

    public ParseState createState() {
        return new ParseState();
    }

    public void processChunk(String chunk, ParseState state, 
                            Consumer<String> textConsumer, 
                            Consumer<AIChatResponse> resultConsumer) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }

        String accumulated = state.pendingText.toString() + chunk;
        state.pendingText.setLength(0);

        while (!accumulated.isEmpty()) {
            if (!state.inResultSection) {
                int textTagIdx = accumulated.indexOf(TEXT_START);
                int resultTagIdx = accumulated.indexOf(RESULT_START);

                if (textTagIdx == -1 && resultTagIdx == -1) {
                    if (state.textSectionStarted) {
                        state.textBuffer.append(accumulated);
                        if (state.textBuffer.length() >= 5) {
                            String textChunk = state.textBuffer.toString();
                            state.textBuffer.setLength(0);
                            state.contentChunks.add(textChunk);
                            textConsumer.accept(textChunk);
                        }
                    }
                    state.pendingText.append(accumulated);
                    break;
                }

                if (resultTagIdx != -1 && (textTagIdx == -1 || resultTagIdx < textTagIdx)) {
                    if (state.textSectionStarted && state.textBuffer.length() > 0) {
                        String textChunk = state.textBuffer.toString();
                        state.contentChunks.add(textChunk);
                        textConsumer.accept(textChunk);
                        state.textBuffer.setLength(0);
                    }

                    state.inResultSection = true;
                    state.resultDepth = 0;
                    String beforeResult = accumulated.substring(0, resultTagIdx);
                    if (!beforeResult.isEmpty()) {
                        state.pendingText.append(beforeResult);
                    }
                    accumulated = accumulated.substring(resultTagIdx + RESULT_START.length());
                    continue;
                }

                if (textTagIdx != -1) {
                    state.textSectionStarted = true;
                    String beforeText = accumulated.substring(0, textTagIdx);
                    if (!beforeText.isEmpty()) {
                        state.pendingText.append(beforeText);
                    }
                    accumulated = accumulated.substring(textTagIdx + TEXT_START.length());
                    continue;
                }
            } else {
                accumulated = processResultSection(accumulated, state, resultConsumer);
                if (accumulated == null) {
                    break;
                }
            }
        }
    }

    private String processResultSection(String input, ParseState state, 
                                       Consumer<AIChatResponse> resultConsumer) {
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '{') {
                if (state.resultDepth == 0 && i > 0) {
                    String before = input.substring(0, i);
                    state.pendingText.append(before);
                    input = input.substring(i);
                    i = -1;
                }
                state.resultDepth++;
            } else if (c == '}') {
                state.resultDepth--;
            }

            if (state.resultDepth == 0) {
                state.resultBuffer.append(c);
                String current = state.resultBuffer.toString();
                int jsonStart = current.indexOf('{');
                int jsonEnd = current.lastIndexOf("}");

                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonCandidate = current.substring(jsonStart, jsonEnd + 1);
                    jsonCandidate = cleanJsonString(jsonCandidate);

                    try {
                        AIChatResponse response = objectMapper.readValue(jsonCandidate, AIChatResponse.class);
                        state.resultChunks.add(response);
                        resultConsumer.accept(response);
                        log.debug("成功解析result chunk");
                    } catch (Exception e) {
                        log.debug("JSON解析不完整或失败，继续积累: {}", e.getMessage());
                    }
                }

                state.resultBuffer.setLength(0);
                state.inResultSection = false;
                return input.substring(i + 1);
            } else {
                state.resultBuffer.append(c);
            }
        }

        state.resultBuffer.append(input);
        return null;
    }

    public void finalize(ParseState state, Consumer<String> textConsumer) {
        if (state.textBuffer.length() > 0) {
            String textChunk = state.textBuffer.toString();
            state.contentChunks.add(textChunk);
            textConsumer.accept(textChunk);
            state.textBuffer.setLength(0);
        }
    }

    public String cleanJsonString(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String cleaned = json;
        cleaned = cleaned.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*", "");
        cleaned = cleaned.replaceAll("```", "");

        int jsonStart = cleaned.indexOf("{");
        int jsonEnd = cleaned.lastIndexOf("}");

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        return cleaned.trim();
    }

    public AIChatResponse parseCompleteResult(String fullText) {
        int resultIdx = fullText.indexOf(RESULT_START);
        if (resultIdx < 0) {
            return null;
        }

        String json = fullText.substring(resultIdx + RESULT_START.length());
        json = cleanJsonString(json);

        try {
            return objectMapper.readValue(json, AIChatResponse.class);
        } catch (Exception e) {
            log.warn("完整JSON解析失败: {}", e.getMessage());
            return null;
        }
    }
}