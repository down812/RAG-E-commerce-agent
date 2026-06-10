package com.ecommerceserver.controller;

import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.ProductDataImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/data-import")
@Tag(name = "数据导入管理")
public class DataImportController {
    
    private final ProductDataImportService dataImportService;
    
    @PostMapping("/import/all")
    @Operation(summary = "批量导入所有商品", description = "导入data目录下所有分类的商品数据")
    public Result<ProductDataImportService.ImportResult> importAllProducts(
            @RequestParam @Parameter(description = "数据目录路径") String dataDirectory) {
        try {
            log.info("开始批量导入商品，目录：{}", dataDirectory);
            ProductDataImportService.ImportResult result = dataImportService.importAllProducts(dataDirectory);
            
            if (result.getFailCount() > 0) {
                log.warn("批量导入完成，存在失败项：{}", result.getErrors());
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量导入商品失败", e);
            return Result.error("批量导入失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/import/category")
    @Operation(summary = "导入单个分类商品", description = "导入指定分类下的所有商品数据")
    public Result<ProductDataImportService.ImportResult> importCategory(
            @RequestParam @Parameter(description = "分类名称") String categoryName,
            @RequestParam @Parameter(description = "数据目录路径") String dataDirectory) {
        try {
            log.info("开始导入分类商品，分类：{}，目录：{}", categoryName, dataDirectory);
            ProductDataImportService.ImportResult result = 
                    dataImportService.importCategory(categoryName, dataDirectory);
            return Result.success(result);
        } catch (Exception e) {
            log.error("导入分类商品失败：category={}", categoryName, e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/import/single")
    @Operation(summary = "上传单个JSON文件导入", description = "上传单个商品JSON文件进行导入")
    public Result<Boolean> importSingleProduct(
            @RequestParam("file") @Parameter(description = "JSON文件") MultipartFile file,
            @RequestParam @Parameter(description = "分类名称") String category) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".json")) {
                return Result.error("只能上传JSON文件");
            }
            
            File tempFile = File.createTempFile("product_", ".json");
            file.transferTo(tempFile);
            
            boolean success = dataImportService.importSingleProduct(tempFile, category);
            
            if (!tempFile.delete()) {
                log.warn("临时文件删除失败：{}", tempFile.getAbsolutePath());
            }
            
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("商品导入失败");
            }
        } catch (IOException e) {
            log.error("处理上传文件失败", e);
            return Result.error("文件处理失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("导入单个商品失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/migrate/images")
    @Operation(summary = "迁移本地图片到OSS", description = "将本地商品图片迁移到阿里云OSS，并更新数据库中的图片URL")
    public Result<ProductDataImportService.ImportResult> migrateImagesToOSS(
            @RequestParam @Parameter(description = "图片目录路径") String imageDirectory,
            @RequestParam @Parameter(description = "数据目录路径（用于获取分类信息）") String dataDirectory) {
        try {
            log.info("开始迁移图片到OSS，图片目录：{}", imageDirectory);
            ProductDataImportService.ImportResult result = 
                    dataImportService.migrateImagesToOSS(imageDirectory, dataDirectory);
            
            if (result.getFailCount() > 0) {
                log.warn("图片迁移完成，存在失败项：{}", result.getErrors());
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("迁移图片到OSS失败", e);
            return Result.error("图片迁移失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/import/beauty")
    @Operation(summary = "导入美妆护肤分类", description = "导入美妆护肤分类下的所有商品")
    public Result<ProductDataImportService.ImportResult> importBeautyProducts(
            @RequestParam @Parameter(description = "数据目录路径") String dataDirectory) {
        try {
            return Result.success(dataImportService.importCategory("1_美妆护肤", dataDirectory));
        } catch (Exception e) {
            log.error("导入美妆护肤分类失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/import/digital")
    @Operation(summary = "导入数码电子分类", description = "导入数码电子分类下的所有商品")
    public Result<ProductDataImportService.ImportResult> importDigitalProducts(
            @RequestParam @Parameter(description = "数据目录路径") String dataDirectory) {
        try {
            return Result.success(dataImportService.importCategory("2_数码电子", dataDirectory));
        } catch (Exception e) {
            log.error("导入数码电子分类失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/import/clothes")
    @Operation(summary = "导入服饰运动分类", description = "导入服饰运动分类下的所有商品")
    public Result<ProductDataImportService.ImportResult> importClothesProducts(
            @RequestParam @Parameter(description = "数据目录路径") String dataDirectory) {
        try {
            return Result.success(dataImportService.importCategory("3_服饰运动", dataDirectory));
        } catch (Exception e) {
            log.error("导入服饰运动分类失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/import/food")
    @Operation(summary = "导入食品生活分类", description = "导入食品生活分类下的所有商品")
    public Result<ProductDataImportService.ImportResult> importFoodProducts(
            @RequestParam @Parameter(description = "数据目录路径") String dataDirectory) {
        try {
            return Result.success(dataImportService.importCategory("4_食品生活", dataDirectory));
        } catch (Exception e) {
            log.error("导入食品生活分类失败", e);
            return Result.error("导入失败：" + e.getMessage());
        }
    }
}