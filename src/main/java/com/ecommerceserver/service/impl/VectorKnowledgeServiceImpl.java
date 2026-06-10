package com.ecommerceserver.service.impl;

import com.ecommerceserver.mapper.DatasetFilesMapper;
import com.ecommerceserver.model.entity.DatasetFiles;
import com.ecommerceserver.service.VectorKnowledgeService;
import com.ecommerceserver.utils.RagKnowledgeExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
public class VectorKnowledgeServiceImpl implements VectorKnowledgeService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DatasetFilesMapper datasetFilesMapper;

    @Autowired
    private RagKnowledgeExtractor ragKnowledgeExtractor;

    @Override
    public boolean save(Long datasetId, Resource resource) {
        try {
            String filename = resource.getFilename();
            if (filename == null) {
                log.error("文件名不能为空");
                return false;
            }

            String fileSuffix = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            List<Document> documents;

            if (fileSuffix.equals("pdf")) {
                documents = readPdfDocument(resource);
            } else if (fileSuffix.equals("json")) {
                documents = readJsonDocument(resource);
            } else if (fileSuffix.matches("docx?|xlsx?|pptx?|txt|md|html?")) {
                documents = readDocumentByTika(resource);
            } else {
                log.error("暂不支持的文件类型: {}", fileSuffix);
                return false;
            }

            if (documents == null || documents.isEmpty()) {
                log.error("文档读取失败或文档为空");
                return false;
            }

            List<Document> documentsWithMetadata = new ArrayList<>();
            for (Document doc : documents) {
                Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                metadata.put("datasetId", datasetId.toString());
                metadata.put("source", filename);

                Document newDoc = new Document(doc.getText(), metadata);
                documentsWithMetadata.add(newDoc);
            }

            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(documentsWithMetadata);

            vectorStore.add(splitDocuments);

            log.info("向量库保存成功，共 {} 个文档块", splitDocuments.size());
            return true;

        } catch (Exception e) {
            log.error("向量库保存异常", e);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean saveRagKnowledge(Long datasetId, Resource resource, String category) {
        try {
            String filename = resource.getFilename();
            if (filename == null) {
                log.error("文件名不能为空");
                return false;
            }

            if (!(resource instanceof FileSystemResource)) {
                log.error("资源不是文件类型");
                return false;
            }

            File file = ((FileSystemResource) resource).getFile();
            Document document = ragKnowledgeExtractor.extractFromJsonFile(file, datasetId, category);
            
            if (document == null) {
                log.error("从JSON文件提取RAG知识失败");
                return false;
            }

            vectorStore.add(List.of(document));

            log.info("RAG知识向量库保存成功，文件: {}", filename);
            return true;

        } catch (Exception e) {
            log.error("RAG知识向量库保存异常", e);
            e.printStackTrace();
            return false;
        }
    }

    private List<Document> readPdfDocument(Resource resource) {
        try {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                            .withPagesPerDocument(1)
                            .build()
            );
            return reader.read();
        } catch (Exception e) {
            log.error("PDF文档读取异常", e);
            e.printStackTrace();
            return null;
        }
    }

    private List<Document> readDocumentByTika(Resource resource) {
        try {
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.read();
        } catch (Exception e) {
            log.error("Tika文档读取异常", e);
            e.printStackTrace();
            return null;
        }
    }

    private List<Document> readJsonDocument(Resource resource) {
        try {
            File file = resource.getFile();
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

            List<Document> documents = new ArrayList<>();

            com.alibaba.fastjson.JSONArray jsonArray = com.alibaba.fastjson.JSON.parseArray(content);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    com.alibaba.fastjson.JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject != null) {
                        StringBuilder textBuilder = new StringBuilder();

                        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                            textBuilder.append(entry.getKey())
                                    .append(": ")
                                    .append(entry.getValue())
                                    .append("; ");
                        }

                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("json_index", i);
                        metadata.put("json_type", "array_item");

                        Document doc = new Document(textBuilder.toString().trim(), metadata);
                        documents.add(doc);
                    }
                }
            } else {
                com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSON.parseObject(content);
                if (jsonObject != null) {
                    StringBuilder textBuilder = new StringBuilder();

                    for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                        textBuilder.append(entry.getKey())
                                .append(": ")
                                .append(entry.getValue())
                                .append("; ");
                    }

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("json_type", "object");

                    Document doc = new Document(textBuilder.toString().trim(), metadata);
                    documents.add(doc);
                }
            }

            log.info("JSON文件解析成功，共生成 {} 个文档", documents.size());
            return documents;

        } catch (Exception e) {
            log.error("JSON文档读取异常", e);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean deleteByDatasetId(Long datasetId) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .topK(1000)
                    .filterExpression("datasetId == " + datasetId)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            if (!documents.isEmpty()) {
                vectorStore.delete(documents.stream().map(Document::getId).toList());
                log.info("删除数据集向量成功: {}", datasetId);
            }
            return true;
        } catch (Exception e) {
            log.error("删除向量库异常", e);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Resource getFile(String fileId) {
        try {
            Long id = Long.parseLong(fileId);
            DatasetFiles datasetFile = datasetFilesMapper.selectById(id);
            if (datasetFile == null) {
                log.error("文件不存在: {}", fileId);
                return null;
            }

            File file = new File(datasetFile.getFilePath());
            if (!file.exists()) {
                log.error("文件物理路径不存在: {}", datasetFile.getFilePath());
                return null;
            }

            return new FileSystemResource(file);
        } catch (Exception e) {
            log.error("获取文件异常", e);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean deleteByFileId(String fileId) {
        try {
            Long id = Long.parseLong(fileId);
            DatasetFiles datasetFile = datasetFilesMapper.selectById(id);
            if (datasetFile == null) {
                log.error("文件不存在: {}", fileId);
                return false;
            }

            deleteByDatasetId(datasetFile.getDatasetId());

            File file = new File(datasetFile.getFilePath());
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("删除物理文件失败: {}", datasetFile.getFilePath());
                }
            }

            datasetFilesMapper.deleteById(id);

            log.info("删除文件成功: {}", fileId);
            return true;
        } catch (Exception e) {
            log.error("删除文件异常", e);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int topK) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.5)
                    .build();

            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error("向量搜索异常", e);
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    public List<Document> similaritySearchByDatasetId(String query, Long datasetId, int topK) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .similarityThreshold(0.5)
                    .filterExpression("datasetId == " + datasetId)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("数据集 {} 搜索到 {} 条结果", datasetId, results.size());
            return results;
        } catch (Exception e) {
            log.error("按数据集ID向量搜索异常", e);
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    public boolean rebuildByDatasetId(Long datasetId) {
        try {
            deleteByDatasetId(datasetId);

            List<DatasetFiles> files = datasetFilesMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DatasetFiles>()
                            .eq(DatasetFiles::getDatasetId, datasetId)
                            .eq(DatasetFiles::getDisabled, 1)
            );

            if (files == null || files.isEmpty()) {
                log.info("数据集 {} 没有文件需要重建向量", datasetId);
                return true;
            }

            int successCount = 0;
            int failCount = 0;

            for (DatasetFiles file : files) {
                try {
                    File physicalFile = new File(file.getFilePath());
                    if (!physicalFile.exists()) {
                        log.warn("文件不存在: {}", file.getFilePath());
                        failCount++;
                        continue;
                    }

                    Resource resource = new FileSystemResource(physicalFile);
                    if (save(datasetId, resource)) {
                        successCount++;
                        log.info("文件 {} 向量重建成功", file.getName());
                    } else {
                        failCount++;
                        log.error("文件 {} 向量重建失败", file.getName());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("处理文件 {} 时发生异常", file.getName(), e);
                }
            }

            log.info("数据集 {} 向量重建完成，成功: {}，失败: {}", datasetId, successCount, failCount);
            return failCount == 0;

        } catch (Exception e) {
            log.error("重建数据集向量库异常", e);
            e.printStackTrace();
            return false;
        }
    }
}