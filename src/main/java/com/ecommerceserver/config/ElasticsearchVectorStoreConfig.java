package com.ecommerceserver.config;

import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchVectorStoreConfig {

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:e_commerce_server}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.initialize-schema:true}")
    private boolean initializeSchema;

    @Value("${spring.ai.vectorstore.elasticsearch.dimensions:1024}")
    private int dimensions;

    public String getIndexName() {
        return indexName;
    }

    public boolean isInitializeSchema() {
        return initializeSchema;
    }

    public int getDimensions() {
        return dimensions;
    }
}