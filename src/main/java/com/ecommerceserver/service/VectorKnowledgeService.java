package com.ecommerceserver.service;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

public interface VectorKnowledgeService {

    /**
     * 保存文件到向量知识库
     * @param datasetId 数据集ID
     * @param resource 文件资源
     * @return 保存结果
     */
    boolean save(Long datasetId, Resource resource);

    /**
     * 保存RAG知识到向量知识库（不分割文档）
     * @param datasetId 数据集ID
     * @param resource JSON文件资源
     * @param category 类别
     * @return 保存结果
     */
    boolean saveRagKnowledge(Long datasetId, Resource resource, String category);

    /**
     * 根据数据集ID删除向量知识
     * @param datasetId 数据集ID
     * @return 删除结果
     */
    boolean deleteByDatasetId(Long datasetId);

    /**
     * 获取文件资源
     * @param fileId 文件ID
     * @return 文件资源
     */
    Resource getFile(String fileId);

    /**
     * 根据文件ID删除向量
     * @param fileId 文件ID
     * @return 删除结果
     */
    boolean deleteByFileId(String fileId);

    /**
     * 搜索相似的文档（全局搜索）
     * @param query 查询文本
     * @param topK 返回数量
     * @return 文档列表
     */
    List<Document> similaritySearch(String query, int topK);

    /**
     * 根据数据集ID搜索相似的文档
     * @param query 查询文本
     * @param datasetId 数据集ID
     * @param topK 返回数量
     * @return 文档列表
     */
    List<Document> similaritySearchByDatasetId(String query, Long datasetId, int topK);

    /**
     * 根据数据集ID从本地文件重建向量数据
     * @param datasetId 数据集ID
     * @return 重建结果，成功返回true
     */
    boolean rebuildByDatasetId(Long datasetId);

}