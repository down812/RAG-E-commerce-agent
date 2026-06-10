package com.ecommerceserver.controller;

import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.RagKnowledgeImportService;
import com.ecommerceserver.service.VectorKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rag-knowledge")
@Tag(name = "RAG知识库管理")
public class RagKnowledgeController {

    private final RagKnowledgeImportService ragKnowledgeImportService;

    private final VectorKnowledgeService vectorKnowledgeService;

    @Value("${tech-pilot-server.file.pathPrefix:/usr/local/project/smart-campus-service-assistant/data}")
    private String defaultDataPath;

    @PostMapping("/import")
    @Operation(summary = "批量导入RAG知识", description = "从指定目录批量导入所有JSON文件的rag_knowledge字段到向量知识库")
    public Result<Map<String, Object>> importRagKnowledge(
            @RequestParam(required = false) @Parameter(description = "数据目录路径，默认使用配置路径") String dataDirectory) {
        try {
            if (dataDirectory == null || dataDirectory.isEmpty()) {
                dataDirectory = defaultDataPath;
            }
            log.info("开始批量导入RAG知识，目录：{}", dataDirectory);
            Map<String, Object> result = ragKnowledgeImportService.importRagKnowledgeFromDirectory(dataDirectory);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量导入RAG知识失败", e);
            return Result.error("批量导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/import/single")
    @Operation(summary = "导入单个文件RAG知识", description = "导入单个JSON文件的rag_knowledge字段到指定类别的数据集")
    public Result<Boolean> importSingleRagKnowledge(
            @RequestParam @Parameter(description = "类别名称，如：美妆护肤") String category,
            @RequestParam @Parameter(description = "JSON文件路径") String jsonFilePath) {
        try {
            log.info("开始导入单个RAG知识，类别：{}，文件：{}", category, jsonFilePath);
            boolean success = ragKnowledgeImportService.importSingleRagKnowledge(category, jsonFilePath);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("导入失败");
            }
        } catch (Exception e) {
            log.error("导入单个RAG知识失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/search")
    @Operation(summary = "全局搜索RAG知识", description = "在所有数据集中搜索相似的RAG知识文档")
    public Result<List<Document>> searchRagKnowledge(
            @RequestParam @Parameter(description = "搜索查询文本") String query,
            @RequestParam(defaultValue = "5") @Parameter(description = "返回结果数量") int topK) {
        try {
            List<Document> results = vectorKnowledgeService.similaritySearch(query, topK);
            return Result.success(results);
        } catch (Exception e) {
            log.error("搜索RAG知识失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    @GetMapping("/search/{datasetId}")
    @Operation(summary = "按数据集ID搜索RAG知识", description = "在指定数据集中搜索相似的RAG知识文档")
    public Result<List<Document>> searchRagKnowledgeByDataset(
            @PathVariable @Parameter(description = "数据集ID") Long datasetId,
            @RequestParam @Parameter(description = "搜索查询文本") String query,
            @RequestParam(defaultValue = "5") @Parameter(description = "返回结果数量") int topK) {
        try {
            List<Document> results = vectorKnowledgeService.similaritySearchByDatasetId(query, datasetId, topK);
            return Result.success(results);
        } catch (Exception e) {
            log.error("按数据集搜索RAG知识失败", e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{datasetId}")
    @Operation(summary = "删除数据集的所有向量", description = "根据数据集ID删除向量库中的所有相关文档")
    public Result<Boolean> deleteByDatasetId(
            @PathVariable @Parameter(description = "数据集ID") Long datasetId) {
        try {
            boolean success = vectorKnowledgeService.deleteByDatasetId(datasetId);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除数据集向量失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }
}