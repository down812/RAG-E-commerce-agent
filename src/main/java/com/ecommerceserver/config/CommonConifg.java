package com.ecommerceserver.config;

import com.ecommerceserver.advisor.ContextChatMemoryAdvisor;
import com.ecommerceserver.advisor.DatabaseChatMemory;
import com.ecommerceserver.constants.SystemConstant;
import com.ecommerceserver.tool.CartTool;
import com.ecommerceserver.tool.ProductTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CommonConifg {

    @Value("${spring.ai.vectorstore.elasticsearch.top-k:3}")
    private int topK;

    @Value("${spring.ai.vectorstore.elasticsearch.similarity-threshold:0.8}")
    private double similarityThreshold;

    @Value("${spring.ai.chat.options.max-tokens:2048}")
    private int maxTokens;

    @Value("${spring.ai.chat.options.temperature:0.3}")
    private double temperature;

    /**
     * 知识库检索 Advisor。
     * 抽成单例 Bean 并从默认 Advisor 链中移除，改由 ChatServiceImpl 按意图“按需挂载”：
     * 纯问候/寒暄等无需知识库的轮次直接跳过，省去一次跨厂商 embedding + ES 检索的阻塞耗时。
     */
    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return new QuestionAnswerAdvisor(
                vectorStore,
                SearchRequest.builder()
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );
    }

    @Bean
    public ChatClient chatClient(
            OpenAiChatModel model,
            DatabaseChatMemory chatMemory,
            ProductTool productTool,
            CartTool cartTool,
            ContextChatMemoryAdvisor contextChatMemoryAdvisor) {

        return ChatClient.builder(model)
                .defaultSystem(SystemConstant.getSystemPrompt())
                .defaultOptions(OpenAiChatOptions.builder()
                        .maxTokens(maxTokens)
                        .temperature(temperature)
                        .build())
                .defaultTools(productTool, cartTool)
                .defaultAdvisors(
                        contextChatMemoryAdvisor,
                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory)
                        // QuestionAnswerAdvisor 不再默认挂载，改由 ChatServiceImpl 按需添加
                )
                .build();
    }

    @Bean
    public ChatClient summaryChatClient(OpenAiChatModel model) {
        return ChatClient.builder(model)
                .defaultSystem(SystemConstant.SUMMARY_SYSTEM_PROMPT)
                .defaultOptions(OpenAiChatOptions.builder()
                        .maxTokens(maxTokens)
                        .temperature(temperature)
                        .build())
                .defaultAdvisors(
                        //new SimpleLoggerAdvisor()
                )
                .build();
    }
}