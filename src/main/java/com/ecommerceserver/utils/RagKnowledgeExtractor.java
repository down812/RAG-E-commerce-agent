package com.ecommerceserver.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@Slf4j
@Component
public class RagKnowledgeExtractor {

    public Document extractFromJsonFile(File jsonFile, Long datasetId, String category) {
        try {
            String content = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
            JSONObject productJson = JSONObject.parseObject(content);
            
            if (productJson == null) {
                log.warn("JSON文件为空或格式错误: {}", jsonFile.getAbsolutePath());
                return null;
            }
            
            String productId = productJson.getString("product_id");
            String title = productJson.getString("title");
            
            JSONObject ragKnowledge = productJson.getJSONObject("rag_knowledge");
            if (ragKnowledge == null) {
                log.warn("文件不包含rag_knowledge字段: {}", jsonFile.getAbsolutePath());
                return null;
            }
            
            String documentText = buildDocumentText(ragKnowledge, productId, title, category);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("datasetId", datasetId.toString());
            metadata.put("productId", productId);
            metadata.put("title", title);
            metadata.put("category", category);
            metadata.put("source", jsonFile.getName());
            metadata.put("content_type", "rag_knowledge");
            
            Document document = new Document(documentText, metadata);
            log.info("成功从文件 {} 提取RAG知识，productId: {}, title: {}", 
                    jsonFile.getName(), productId, title);
            
            return document;
            
        } catch (IOException e) {
            log.error("读取JSON文件失败: {}", jsonFile.getAbsolutePath(), e);
            return null;
        }
    }
    
    private String buildDocumentText(JSONObject ragKnowledge, String productId, 
                                    String title, String category) {
        StringBuilder text = new StringBuilder();
        
        text.append("【商品信息】\n");
        text.append("商品ID：").append(productId != null ? productId : "未知").append("\n");
        text.append("商品名称：").append(title != null ? title : "未知").append("\n");
        text.append("商品分类：").append(category != null ? category : "未知").append("\n\n");
        
        String marketingDesc = ragKnowledge.getString("marketing_description");
        if (marketingDesc != null && !marketingDesc.isEmpty()) {
            text.append("【营销描述】\n");
            text.append(marketingDesc).append("\n\n");
        }
        
        JSONArray officialFaq = ragKnowledge.getJSONArray("official_faq");
        if (officialFaq != null && !officialFaq.isEmpty()) {
            text.append("【官方问答】\n");
            for (int i = 0; i < officialFaq.size(); i++) {
                JSONObject faq = officialFaq.getJSONObject(i);
                String question = faq.getString("question");
                String answer = faq.getString("answer");
                if (question != null && answer != null) {
                    text.append("问：").append(question).append("\n");
                    text.append("答：").append(answer).append("\n\n");
                }
            }
        }
        
        JSONArray userReviews = ragKnowledge.getJSONArray("user_reviews");
        if (userReviews != null && !userReviews.isEmpty()) {
            text.append("【用户评价】\n");
            for (int i = 0; i < userReviews.size(); i++) {
                JSONObject review = userReviews.getJSONObject(i);
                String nickname = review.getString("nickname");
                Integer rating = review.getInteger("rating");
                String comment = review.getString("comment");
                String date = review.getString("date");
                
                text.append("- 用户：").append(nickname != null ? nickname : "匿名").append("\n");
                text.append("  评分：").append(rating != null ? rating + "星" : "未评分").append("\n");
                if (date != null) {
                    text.append("  时间：").append(date).append("\n");
                }
                text.append("  评价：").append(comment != null ? comment : "无").append("\n\n");
            }
        }
        
        return text.toString();
    }
}