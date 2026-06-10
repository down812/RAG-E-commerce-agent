package com.ecommerceserver.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ecommerceserver.exception.GlobalException;
import com.ecommerceserver.mapper.ProductMapper;
import com.ecommerceserver.mapper.ProductSkuAttributeMapper;
import com.ecommerceserver.mapper.ProductSkuMapper;
import com.ecommerceserver.model.entity.Product;
import com.ecommerceserver.model.entity.ProductSku;
import com.ecommerceserver.model.entity.ProductSkuAttribute;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.OSSService;
import com.ecommerceserver.service.ProductDataImportService;
import com.ecommerceserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductDataImportServiceImpl implements ProductDataImportService {
    
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSkuAttributeMapper productSkuAttributeMapper;
    private final OSSService ossService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importAllProducts(String dataDirectory) {
        ImportResult result = new ImportResult();
        
        File dataDir = new File(dataDirectory);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            log.error("数据目录不存在：{}", dataDirectory);
            result.addFail("数据目录不存在：" + dataDirectory);
            return result;
        }
        
        File[] categoryDirs = dataDir.listFiles(File::isDirectory);
        if (categoryDirs == null || categoryDirs.length == 0) {
            log.warn("未找到任何分类目录：{}", dataDirectory);
            result.addFail("未找到任何分类目录");
            return result;
        }
        
        for (File categoryDir : categoryDirs) {
            String categoryName = categoryDir.getName();
            File dataFileDir = new File(categoryDir, "data");
            
            if (!dataFileDir.exists() || !dataFileDir.isDirectory()) {
                log.warn("分类目录 {} 中未找到data文件夹", categoryName);
                continue;
            }
            
            File[] jsonFiles = dataFileDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (jsonFiles == null || jsonFiles.length == 0) {
                log.warn("分类目录 {} 中未找到JSON文件", categoryName);
                continue;
            }
            
            for (File jsonFile : jsonFiles) {
                try {
                    boolean success = importSingleProduct(jsonFile, categoryName);
                    if (success) {
                        result.addSuccess();
                        log.info("商品导入成功：{}", jsonFile.getName());
                    } else {
                        result.addFail("导入失败：" + jsonFile.getName());
                    }
                } catch (Exception e) {
                    log.error("导入文件失败：{}", jsonFile.getName(), e);
                    result.addFail("导入异常：" + jsonFile.getName() + " - " + e.getMessage());
                }
            }
        }
        
        log.info("批量导入完成：成功={}, 失败={}, 总计={}", 
                result.getSuccessCount(), result.getFailCount(), result.getTotalCount());
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importSingleProduct(File jsonFile, String category) {
        try {
            String content = new String(Files.readAllBytes(jsonFile.toPath()));
            JSONObject productJson = JSON.parseObject(content);
            
            String productCode = productJson.getString("product_id");
            if (productCode == null || productCode.isEmpty()) {
                log.error("JSON文件中未找到product_id：{}", jsonFile.getName());
                return false;
            }
            
            QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("product_code", productCode);
            Product existProduct = productMapper.selectOne(queryWrapper);
            
            if (existProduct != null) {
                log.info("商品已存在，跳过：{} - {}", productCode, productJson.getString("title"));
                return true;
            }
            
            Product product = new Product();
            product.setProductCode(productCode);
            product.setTitle(productJson.getString("title"));
            product.setBrand(productJson.getString("brand"));
            product.setCategory(productJson.getString("category"));
            product.setSubCategory(productJson.getString("sub_category"));
            
            Double basePrice = productJson.getDouble("base_price");
            if (basePrice != null) {
                product.setBasePrice(BigDecimal.valueOf(basePrice));
            }

            // ✅ 从JSON文件的image_path字段读取商品图片路径
            String imagePath = productJson.getString("image_path");
            if (imagePath != null && !imagePath.isEmpty()) {
                // 构建完整的本地图片路径
                // JSON文件位置：{dataDir}/{category}/data/{product}.json
                // 图片相对路径：{category}/images/{product}_live.jpg
                File dataDir = jsonFile.getParentFile(); // data目录
                File rootDir = dataDir.getParentFile().getParentFile(); // 根数据目录
                String fullImagePath = rootDir.getAbsolutePath() + "/" + imagePath;
                
                product.setLocalImagePath(fullImagePath);
                
                // ✅ 根据图片路径找到对应图片并上传到OSS
                File imageFile = new File(fullImagePath);
                if (imageFile.exists()) {
                    String ossUrl = uploadImageToOSS(imageFile, category, productCode);
                    product.setMainImageUrl(ossUrl);
                    
                    if (ossUrl == null || ossUrl.isEmpty()) {
                        log.warn("商品图片上传失败：productCode={}, imagePath={}", productCode, fullImagePath);
                    }
                } else {
                    log.warn("商品图片文件不存在：productCode={}, imagePath={}", productCode, fullImagePath);
                    product.setMainImageUrl(null);
                }
            } else {
                product.setLocalImagePath(null);
                product.setMainImageUrl(null);
                log.warn("商品未配置图片路径：productCode={}", productCode);
            }
            
            product.setStatus(1);
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            productMapper.insert(product);
            
            JSONArray skus = productJson.getJSONArray("skus");
            if (skus != null && !skus.isEmpty()) {
                for (int i = 0; i < skus.size(); i++) {
                    JSONObject skuJson = skus.getJSONObject(i);
                    
                    ProductSku sku = new ProductSku();
                    sku.setSkuCode(skuJson.getString("sku_id"));
                    sku.setProductId(product.getId());
                    
                    Double price = skuJson.getDouble("price");
                    if (price != null) {
                        sku.setPrice(BigDecimal.valueOf(price));
                    }

                    sku.setStatus(1);
                    sku.setCreatedAt(LocalDateTime.now());
                    sku.setUpdatedAt(LocalDateTime.now());
                    productSkuMapper.insert(sku);
                    
                    JSONObject properties = skuJson.getJSONObject("properties");
                    if (properties != null && !properties.isEmpty()) {
                        for (Map.Entry<String, Object> entry : properties.entrySet()) {
                            ProductSkuAttribute attr = new ProductSkuAttribute();
                            attr.setSkuId(sku.getId());
                            attr.setAttrName(entry.getKey());
                            attr.setAttrValue(String.valueOf(entry.getValue()));
                            attr.setCreatedAt(LocalDateTime.now());
                            productSkuAttributeMapper.insert(attr);
                        }
                    }
                }
            }
            
            log.info("商品导入成功：ID={}, 编码={}, 标题={}, 图片={}", 
                    product.getId(), productCode, product.getTitle(), product.getMainImageUrl());
            return true;
            
        } catch (Exception e) {
            log.error("导入单个商品失败：{}", jsonFile.getName(), e);
            throw new RuntimeException("导入商品失败：" + e.getMessage());
        }
    }
    
    private String uploadImageToOSS(File imageFile, String category, String productCode) {
        if (imageFile == null || !imageFile.exists()) {
            log.warn("商品图片文件不存在或为空：productCode={}", productCode);
            return null;
        }
        
        try {
            String date = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            String uuid = java.util.UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8);
            
            String extension = "";
            if (imageFile.getName().contains(".")) {
                extension = imageFile.getName().substring(imageFile.getName().lastIndexOf("."));
            }
            
            String ossPath = String.format("products/%s/%s/%s%s",
                    category, productCode, uuid, extension);
            
            String ossUrl = ossService.uploadLocalFile(imageFile, ossPath);
            
            log.info("图片上传成功：本地路径={} -> OSS路径={}", imageFile.getAbsolutePath(), ossUrl);
            return ossUrl;
            
        } catch (Exception e) {
            log.error("图片上传失败：localPath={}, productCode={}", imageFile.getAbsolutePath(), productCode, e);
            return null;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult migrateImagesToOSS(String imageDirectory, String dataDirectory) {
        ImportResult result = new ImportResult();
        
        File imageDir = new File(imageDirectory);
        if (!imageDir.exists() || !imageDir.isDirectory()) {
            log.error("图片目录不存在：{}", imageDirectory);
            result.addFail("图片目录不存在：" + imageDirectory);
            return result;
        }
        
        File dataDir = new File(dataDirectory);
        File[] categoryDirs = imageDir.listFiles(File::isDirectory);
        
        if (categoryDirs == null || categoryDirs.length == 0) {
            log.error("未找到任何分类目录：{}", imageDirectory);
            result.addFail("未找到任何分类目录");
            return result;
        }
        
        for (File categoryDir : categoryDirs) {
            String categoryName = categoryDir.getName();
            
            File[] images = categoryDir.listFiles((dir, name) -> {
                String lowerName = name.toLowerCase();
                return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || 
                       lowerName.endsWith(".png") || lowerName.endsWith(".gif");
            });
            
            if (images == null || images.length == 0) {
                log.warn("分类目录 {} 中未找到图片文件", categoryName);
                continue;
            }
            
            for (File image : images) {
                try {
                    String productCode = image.getName().replace("_live.jpg", "")
                                                       .replace("_live.jpeg", "")
                                                       .replace("_live.png", "")
                                                       .replace("_live.gif", "");
                    
                    String productCodeWithoutPrefix = productCode;
                    if (productCode.contains("_")) {
                        String[] parts = productCode.split("_");
                        if (parts.length >= 2) {
                            productCodeWithoutPrefix = parts[1];
                        }
                    }
                    
                    QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
                    queryWrapper.likeRight("product_code", productCode);
                    Product product = productMapper.selectOne(queryWrapper);
                    
                    if (product == null) {
                        log.warn("未找到对应商品，跳过图片：{} -> {}", image.getName(), productCode);
                        continue;
                    }
                    
                    String date = java.time.LocalDate.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                    String uuid = java.util.UUID.randomUUID().toString()
                            .replace("-", "").substring(0, 8);
                    String extension = "";
                    if (image.getName().contains(".")) {
                        extension = image.getName().substring(image.getName().lastIndexOf("."));
                    }
                    
                    String ossPath = String.format("products/%s/%s/%s%s",
                            categoryName, product.getProductCode(), uuid, extension);
                    
                    String ossUrl = ossService.uploadLocalFile(image, ossPath);
                    
                    product.setMainImageUrl(ossUrl);
                    productMapper.updateById(product);
                    
                    result.addSuccess();
                    log.info("图片迁移成功：{} -> {}", image.getName(), ossUrl);
                    
                } catch (Exception e) {
                    log.error("图片迁移失败：{}", image.getName(), e);
                    result.addFail("图片迁移失败：" + image.getName() + " - " + e.getMessage());
                }
            }
        }
        
        log.info("图片迁移完成：成功={}, 失败={}, 总计={}",
                result.getSuccessCount(), result.getFailCount(), result.getTotalCount());
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importCategory(String categoryName, String dataDirectory) {
        ImportResult result = new ImportResult();
        
        File categoryDir = new File(dataDirectory, categoryName);
        if (!categoryDir.exists() || !categoryDir.isDirectory()) {
            log.error("分类目录不存在：{}", categoryDir.getAbsolutePath());
            result.addFail("分类目录不存在：" + categoryName);
            return result;
        }
        
        File dataFileDir = new File(categoryDir, "data");
        if (!dataFileDir.exists() || !dataFileDir.isDirectory()) {
            log.error("分类目录中未找到data文件夹：{}", categoryName);
            result.addFail("分类目录中未找到data文件夹：" + categoryName);
            return result;
        }
        
        File[] jsonFiles = dataFileDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            log.warn("分类目录 {} 中未找到JSON文件", categoryName);
            result.addFail("分类目录中未找到JSON文件：" + categoryName);
            return result;
        }
        
        for (File jsonFile : jsonFiles) {
            try {
                boolean success = importSingleProduct(jsonFile, categoryName);
                if (success) {
                    result.addSuccess();
                } else {
                    result.addFail("导入失败：" + jsonFile.getName());
                }
            } catch (Exception e) {
                log.error("导入文件失败：{}", jsonFile.getName(), e);
                result.addFail("导入异常：" + jsonFile.getName() + " - " + e.getMessage());
            }
        }
        
        return result;
    }
}