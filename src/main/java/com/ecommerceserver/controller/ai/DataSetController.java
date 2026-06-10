package com.ecommerceserver.controller.ai;

import com.ecommerceserver.constants.DatasetConstant;
import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.model.dto.DataSetDTO;
import com.ecommerceserver.model.entity.DataSet;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.DataSetService;
import com.ecommerceserver.service.VectorKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Tag(name = "知识库管理")
@Slf4j
@RestController
@RequestMapping("/ai/dataset")
public class DataSetController {
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private VectorKnowledgeService vectorKnowledgeService;
    
    @Autowired
    private DataSetService dataSetService;
    
    @Operation(summary = "创建数据集")
    @PostMapping("/create")
    public Result createDataSet(@RequestBody DataSetDTO dataSetDTO) {
        return Result.success(dataSetService.createDataSet(dataSetDTO));
    }
    
    @Operation(summary = "上传文件到数据集")
    @PostMapping("/upload/{datasetId}")
    public Result uploadFile(
            @PathVariable Long datasetId,
            @RequestParam("file") MultipartFile file) {
        boolean success = dataSetService.uploadFile(datasetId, file);
        if (success) {
            return Result.success("文件上传成功");
        } else {
            return Result.error("文件上传失败");
        }
    }
    
    @Operation(summary = "获取用户的数据集列表")
    @GetMapping("/list")
    public Result getUserDataSets() {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.error(DatasetConstant.USER_ID_NOT_NULL);
        }

        List<DataSet> dataSets = dataSetService.getUserDataSets(userId);
        return Result.success(dataSets);
    }
    
    @Operation(summary = "删除数据集")
    @DeleteMapping("/{datasetId}")
    public Result deleteDataSet(@PathVariable Long datasetId) {
        boolean success = dataSetService.deleteDataSet(datasetId);
        if (success) {
            return Result.success(DatasetConstant.DATASET_DELETE_SUCCESS);
        } else {
            return Result.error(DatasetConstant.DATASET_DELETE_FAILED);
        }
    }
    
    @Operation(summary = "启用/禁用数据集")
    @PostMapping("/toggle/{datasetId}")
    public Result toggleDataSet(
            @PathVariable Long datasetId,
            @RequestParam Integer disabled) {
        boolean success = dataSetService.toggleDataSet(datasetId, disabled);
        if (success) {
            String message = disabled == 1 ? "数据集已启用" : "数据集已禁用";
            return Result.success(message);
        } else {
            return Result.error("操作失败");
        }
    }
    
    @Operation(summary = "下载文件")
    @GetMapping("/file/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) {
        Resource resource = vectorKnowledgeService.getFile(fileId);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        
        String fileName = URLEncoder.encode(
                Objects.requireNonNull(resource.getFilename()),
                StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
    
    @Operation(summary = "删除文件")
    @DeleteMapping("/file/{fileId}")
    public Result deleteFile(@PathVariable String fileId) {
        Long userId = LoginContext.getUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        boolean success = vectorKnowledgeService.deleteByFileId(fileId);
        if (success) {
            return Result.success("文件删除成功");
        } else {
            return Result.error("文件删除失败");
        }
    }
}