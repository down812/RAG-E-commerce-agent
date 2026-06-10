package com.ecommerceserver.controller.user;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerceserver.model.dto.ProductDTO;
import com.ecommerceserver.model.entity.Product;
import com.ecommerceserver.model.dto.ProductSkuDTO;
import com.ecommerceserver.model.vo.ProductVO;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product")
@Tag(name = "商品管理")
public class ProductController {
    
    private final ProductService productService;
    
    @PostMapping("/create")
    @Operation(summary = "创建商品", description = "创建商品并关联SKU")
    public Result<Boolean> createProduct(@RequestBody ProductDTO productDTO) {
        try {
            boolean success = productService.createProduct(productDTO, productDTO.getSkuList());
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("商品创建失败");
            }
        } catch (Exception e) {
            log.error("创建商品异常", e);
            return Result.error("商品创建失败：" + e.getMessage());
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新商品", description = "更新商品信息并重新关联SKU")
    public Result<Boolean> updateProduct(@RequestBody ProductDTO productDTO) {
        try {
            boolean success = productService.updateProduct(productDTO, productDTO.getSkuList());
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("商品更新失败");
            }
        } catch (Exception e) {
            log.error("更新商品异常", e);
            return Result.error("商品更新失败：" + e.getMessage());
        }
    }
    
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除商品", description = "删除商品及其关联的SKU和图片")
    public Result<Boolean> deleteProduct(
            @PathVariable @Parameter(description = "商品ID") Long id) {
        try {
            boolean success = productService.deleteProduct(id);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("商品删除失败");
            }
        } catch (Exception e) {
            log.error("删除商品异常：id={}", id, e);
            return Result.error("商品删除失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详细信息，包含SKU列表")
    public Result<ProductVO> getProductById(
            @PathVariable @Parameter(description = "商品ID") Long id) {
        try {
            ProductVO product = productService.getProductById(id);
            if (product != null) {
                return Result.success(product);
            } else {
                return Result.error("商品不存在");
            }
        } catch (Exception e) {
            log.error("获取商品详情异常：id={}", id, e);
            return Result.error("获取商品详情失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/detail/code/{productCode}")
    @Operation(summary = "根据编码获取商品", description = "根据商品编码获取商品详细信息")
    public Result<ProductVO> getProductByCode(
            @PathVariable @Parameter(description = "商品编码") String productCode) {
        try {
            ProductVO product = productService.getProductByCode(productCode);
            if (product != null) {
                return Result.success(product);
            } else {
                return Result.error("商品不存在");
            }
        } catch (Exception e) {
            log.error("获取商品详情异常：productCode={}", productCode, e);
            return Result.error("获取商品详情失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/list/page")
    @Operation(summary = "分页获取商品列表", description = "分页获取商品列表")
    public Result<Page<ProductVO>> getProductPage(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer pageNum,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "商品状态") Integer status,
            @RequestParam(required = false) @Parameter(description = "分类") String category,
            @RequestParam(required = false) @Parameter(description = "品牌") String brand) {
        try {
            Page<ProductVO> voPage = productService.getProductVOPage(pageNum, pageSize, status, category, brand);
            return Result.success(voPage);
        } catch (Exception e) {
            log.error("获取商品列表异常", e);
            return Result.error("获取商品列表失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    @Operation(summary = "商品列表", description = "获取商品列表")
    public Result<List<ProductVO>> getProductList(
            @RequestParam(required = false) @Parameter(description = "商品状态") Integer status,
            @RequestParam(required = false) @Parameter(description = "分类") String category,
            @RequestParam(required = false) @Parameter(description = "品牌") String brand,
            @RequestParam(required = false) @Parameter(description = "商品名称关键词") String title) {
        try {
            List<ProductVO> voList = productService.getProductVOList(status, category, brand, title);
            return Result.success(voList);
        } catch (Exception e) {
            log.error("获取商品列表异常", e);
            return Result.error("获取商品列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/upload/image")
    @Operation(summary = "上传商品图片", description = "上传商品主图到OSS，返回OSS访问URL")
    public Result<String> uploadProductImage(
            @RequestParam("file") @Parameter(description = "图片文件") MultipartFile file,
            @RequestParam @Parameter(description = "商品编码") String productCode) {
        try {
            String ossUrl = productService.uploadProductImage(file, productCode);
            return Result.success(ossUrl);
        } catch (Exception e) {
            log.error("上传商品图片异常：productCode={}", productCode, e);
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }
    
    @PutMapping("/update/image")
    @Operation(summary = "更新商品主图", description = "上传新图片并更新商品的主图URL")
    public Result<Boolean> updateProductImage(
            @RequestParam("file") @Parameter(description = "图片文件") MultipartFile file,
            @RequestParam @Parameter(description = "商品ID") Long productId) {
        try {
            Product product = productService.getById(productId);
            if (product == null) {
                return Result.error("商品不存在");
            }
            
            String productCode = product.getProductCode();
            String ossUrl = productService.uploadProductImage(file, productCode);
            
            product.setMainImageUrl(ossUrl);
            boolean success = productService.updateById(product);
            
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("更新商品图片失败");
            }
        } catch (Exception e) {
            log.error("更新商品图片异常：productId={}", productId, e);
            return Result.error("图片更新失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索商品", description = "根据关键词搜索商品")
    public Result<Page<ProductVO>> searchProducts(
            @RequestParam @Parameter(description = "搜索关键词") String keyword,
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer pageNum,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") Integer pageSize) {
        try {
            Page<ProductVO> voPage = productService.searchProductsPage(keyword, pageNum, pageSize);
            return Result.success(voPage);
        } catch (Exception e) {
            log.error("搜索商品异常：keyword={}", keyword, e);
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}