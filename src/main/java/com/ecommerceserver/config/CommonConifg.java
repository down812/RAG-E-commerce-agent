package com.ecommerceserver.config;

import com.ecommerceserver.advisor.ContextChatMemoryAdvisor;
import com.ecommerceserver.advisor.DatabaseChatMemory;
import com.ecommerceserver.constants.SystemConstant;
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

    @Bean
    public ChatClient chatClient(
            OpenAiChatModel model,
            DatabaseChatMemory chatMemory,
            VectorStore vectorStore,
            ProductTool productTool,
            ContextChatMemoryAdvisor contextChatMemoryAdvisor) {

        return ChatClient.builder(model)
                .defaultSystem(SystemConstant.getSystemPrompt())
                .defaultOptions(OpenAiChatOptions.builder()
                        .maxTokens(maxTokens)
                        .temperature(temperature)
                        .build())
                .defaultTools(productTool)
                .defaultAdvisors(
                        contextChatMemoryAdvisor,
//                        new SimpleLoggerAdvisor(),
                        new MessageChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.builder()
                                        .topK(topK)
                                        .similarityThreshold(similarityThreshold)
                                        .build()
                        )
                )
                .build();
    }
}