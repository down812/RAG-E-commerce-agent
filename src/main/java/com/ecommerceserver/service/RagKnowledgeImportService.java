package com.ecommerceserver.service;

import java.util.Map;

public interface RagKnowledgeImportService {

    /**
     * 从指定目录批量导入RAG知识到向量库
     * @param dataDir 数据目录路径
     * @return 导入结果统计
     */
    Map<String, Object> importRagKnowledgeFromDirectory(String dataDir);

    /**
     * 导入单个JSON文件的RAG知识
     * @param category 类别
     * @param jsonFilePath JSON文件路径
     * @return 是否成功
     */
    boolean importSingleRagKnowledge(String category, String jsonFilePath);
}