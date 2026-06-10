package com.ecommerceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.mapper.DataSetMapper;
import com.ecommerceserver.mapper.DatasetFilesMapper;
import com.ecommerceserver.model.entity.DataSet;
import com.ecommerceserver.model.entity.DatasetFiles;
import com.ecommerceserver.service.DataSetService;
import com.ecommerceserver.service.RagKnowledgeImportService;
import com.ecommerceserver.service.VectorKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class RagKnowledgeImportServiceImpl implements RagKnowledgeImportService {

    @Autowired
    private VectorKnowledgeService vectorKnowledgeService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataSetMapper dataSetMapper;

    @Autowired
    private DatasetFilesMapper datasetFilesMapper;

    @Value("${tech-pilot-server.file.pathPrefix:/usr/local/project/smart-campus-service-assistant/data}")
    private String filePathPrefix;

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("^\\d+_(.+)$");

    @Override
    public Map<String, Object> importRagKnowledgeFromDirectory(String dataDir) {
        Map<String, Object> result = new HashMap<>();
        int totalSuccess = 0;
        int totalFailed = 0;
        Map<String, Integer> categoryStats = new HashMap<>();
        Map<String, Long> categoryDatasetIds = new HashMap<>();

        try {
            File rootDir = new File(dataDir);
            if (!rootDir.exists() || !rootDir.isDirectory()) {
                log.error("数据目录不存在或不是目录: {}", dataDir);
                result.put("success", false);
                result.put("message", "数据目录不存在或不是目录");
                return result;
            }

            File[] categoryDirs = rootDir.listFiles(File::isDirectory);
            if (categoryDirs == null || categoryDirs.length == 0) {
                log.warn("数据目录中没有子目录: {}", dataDir);
                result.put("success", true);
                result.put("message", "没有找到类别目录");
                result.put("totalSuccess", 0);
                result.put("totalFailed", 0);
                return result;
            }

            Arrays.sort(categoryDirs, Comparator.comparing(File::getName));

            for (File categoryDir : categoryDirs) {
                String categoryName = extractCategoryName(categoryDir.getName());
                if (categoryName == null) {
                    log.warn("跳过无效的目录名称: {}", categoryDir.getName());
                    continue;
                }

                log.info("开始处理类别: {}", categoryName);

                Long datasetId = getOrCreateDatasetForCategory(categoryName);
                if (datasetId == null) {
                    log.error("为类别 {} 创建或获取数据集失败", categoryName);
                    continue;
                }
                categoryDatasetIds.put(categoryName, datasetId);

                File dataSubDir = new File(categoryDir, "data");
                if (!dataSubDir.exists() || !dataSubDir.isDirectory()) {
                    log.warn("类别 {} 的data子目录不存在", categoryName);
                    continue;
                }

                File[] jsonFiles = dataSubDir.listFiles((dir, name) -> name.endsWith(".json"));
                if (jsonFiles == null || jsonFiles.length == 0) {
                    log.warn("类别 {} 的data目录中没有JSON文件", categoryName);
                    continue;
                }

                Arrays.sort(jsonFiles, Comparator.comparing(File::getName));

                int successCount = 0;
                int failedCount = 0;

                for (File jsonFile : jsonFiles) {
                    try {
                        boolean success = importSingleJsonFile(datasetId, categoryName, jsonFile);
                        if (success) {
                            successCount++;
                        } else {
                            failedCount++;
                        }
                    } catch (Exception e) {
                        log.error("处理文件 {} 时发生异常", jsonFile.getAbsolutePath(), e);
                        failedCount++;
                    }
                }

                categoryStats.put(categoryName, successCount);
                totalSuccess += successCount;
                totalFailed += failedCount;

                log.info("类别 {} 处理完成，成功: {}，失败: {}", categoryName, successCount, failedCount);
            }

            result.put("success", true);
            result.put("message", "导入完成");
            result.put("totalSuccess", totalSuccess);
            result.put("totalFailed", totalFailed);
            result.put("categoryStats", categoryStats);
            result.put("categoryDatasetIds", categoryDatasetIds);

            log.info("RAG知识导入完成，成功: {}，失败: {}", totalSuccess, totalFailed);

        } catch (Exception e) {
            log.error("批量导入RAG知识时发生异常", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            result.put("totalSuccess", totalSuccess);
            result.put("totalFailed", totalFailed);
        }

        return result;
    }

    @Override
    public boolean importSingleRagKnowledge(String category, String jsonFilePath) {
        try {
            File jsonFile = new File(jsonFilePath);
            if (!jsonFile.exists() || !jsonFile.isFile()) {
                log.error("JSON文件不存在: {}", jsonFilePath);
                return false;
            }

            Long datasetId = getOrCreateDatasetForCategory(category);
            if (datasetId == null) {
                log.error("获取或创建数据集失败");
                return false;
            }

            return importSingleJsonFile(datasetId, category, jsonFile);

        } catch (Exception e) {
            log.error("导入单个RAG知识时发生异常", e);
            return false;
        }
    }

    private String extractCategoryName(String dirName) {
        Matcher matcher = CATEGORY_PATTERN.matcher(dirName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private Long getOrCreateDatasetForCategory(String categoryName) {
        try {
            LambdaQueryWrapper<DataSet> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DataSet::getName, categoryName);
            DataSet existingDataset = dataSetMapper.selectOne(wrapper);

            if (existingDataset != null) {
                log.info("找到已存在的数据集: {}, ID: {}", categoryName, existingDataset.getId());
                return existingDataset.getId();
            }

            DataSet newDataset = DataSet.builder()
                    .name(categoryName)
                    .description("自动创建的商品数据集 - " + categoryName)
                    .disabled(1)
                    .appCount(0)
                    .docCount(0)
                    .userId(LoginContext.getUserId())
                    .createdAt(new Date())
                    .updatedAt(new Date())
                    .build();

            boolean saved = dataSetService.saveEntity(newDataset);
            if (saved) {
                DataSet savedDataset = dataSetMapper.selectOne(wrapper);
                if (savedDataset != null) {
                    log.info("成功创建数据集: {}, ID: {}", categoryName, savedDataset.getId());
                    return savedDataset.getId();
                }
            }

            log.error("创建数据集失败: {}", categoryName);
            return null;

        } catch (Exception e) {
            log.error("获取或创建数据集时发生异常: {}", categoryName, e);
            return null;
        }
    }

    private boolean importSingleJsonFile(Long datasetId, String category, File jsonFile) {
        try {
            Resource resource = new FileSystemResource(jsonFile);
            boolean saveSuccess = vectorKnowledgeService.saveRagKnowledge(datasetId, resource, category);

            if (saveSuccess) {
                DatasetFiles datasetFile = DatasetFiles.builder()
                        .name(jsonFile.getName())
                        .filePath(jsonFile.getAbsolutePath())
                        .fileType("json")
                        .fileSize(jsonFile.length())
                        .datasetId(datasetId)
                        .disabled(1)
                        .createdAt(new Date())
                        .hitCount(0)
                        .userId(LoginContext.getUserId())
                        .build();

                datasetFilesMapper.insert(datasetFile);
                log.info("成功导入文件 {} 到数据集 {}", jsonFile.getName(), datasetId);
                return true;
            } else {
                log.error("保存向量知识失败: {}", jsonFile.getName());
                return false;
            }

        } catch (Exception e) {
            log.error("导入单个JSON文件时发生异常: {}", jsonFile.getAbsolutePath(), e);
            return false;
        }
    }
}