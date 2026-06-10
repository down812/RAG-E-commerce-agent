package com.ecommerceserver.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerceserver.Enum.ProductStatusEnum;
import com.ecommerceserver.mapper.ProductMapper;
import com.ecommerceserver.mapper.ProductSkuAttributeMapper;
import com.ecommerceserver.mapper.ProductSkuMapper;
import com.ecommerceserver.model.dto.ProductDTO;
import com.ecommerceserver.model.dto.ProductSkuDTO;
import com.ecommerceserver.model.dto.ProductSkuAttributeDTO;
import com.ecommerceserver.model.entity.Product;
import com.ecommerceserver.model.entity.ProductSku;
import com.ecommerceserver.model.entity.ProductSkuAttribute;
import com.ecommerceserver.model.vo.ProductSkuAttributeVO;
import com.ecommerceserver.model.vo.ProductSkuVO;
import com.ecommerceserver.model.vo.ProductVO;
import com.ecommerceserver.service.OSSService;
import com.ecommerceserver.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductSkuMapper productSkuMapper;
    private final ProductSkuAttributeMapper productSkuAttributeMapper;
    private final OSSService ossService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProduct(ProductDTO productDTO, List<ProductSkuDTO> skuDTOList) {
        try {
            Product product = BeanUtil.copyProperties(productDTO, Product.class);
            if (StringUtils.isEmpty(product.getProductCode())) {
                product.setProductCode(generateProductCode());
            }
            product.setStatus(product.getStatus() == null ? ProductStatusEnum.UP.getCode() : ProductStatusEnum.DOWN.getCode());
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());

            boolean result = this.save(product);
            if (!result) {
                log.error("商品创建失败：{}", product.getTitle());
                return false;
            }

            if (CollectionUtil.isNotEmpty(skuDTOList)) {
                for (ProductSkuDTO skuDTO : skuDTOList) {
                    ProductSku sku = BeanUtil.copyProperties(skuDTO, ProductSku.class);
                    sku.setProductId(product.getId());
                    sku.setSkuCode(StringUtils.isEmpty(sku.getSkuCode()) ? generateSkuCode(product.getProductCode()) : sku.getSkuCode());
                    sku.setStatus(sku.getStatus() == null ? ProductStatusEnum.UP.getCode() : sku.getStatus());
                    sku.setCreatedAt(LocalDateTime.now());
                    sku.setUpdatedAt(LocalDateTime.now());

                    productSkuMapper.insert(sku);

                    if (CollectionUtil.isNotEmpty(skuDTO.getAttributeList())) {
                        for (ProductSkuAttributeDTO attrDTO : skuDTO.getAttributeList()) {
                            ProductSkuAttribute attr = BeanUtil.copyProperties(attrDTO, ProductSkuAttribute.class);
                            attr.setSkuId(sku.getId());
                            attr.setCreatedAt(LocalDateTime.now());
                            attr.setUpdatedAt(LocalDateTime.now());
                            productSkuAttributeMapper.insert(attr);
                        }
                    }
                }
            }

            log.info("商品创建成功：ID={}, 编码={}, 标题={}",
                    product.getId(), product.getProductCode(), product.getTitle());
            return true;

        } catch (Exception e) {
            log.error("商品创建异常：{}", productDTO.getTitle(), e);
            throw new RuntimeException("商品创建失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProduct(ProductDTO productDTO, List<ProductSkuDTO> skuDTOList) {
        try {
            Product product = BeanUtil.copyProperties(productDTO, Product.class);
            Product existProduct = this.getById(product.getId());
            if (existProduct == null) {
                log.error("商品不存在：ID={}", product.getId());
                return false;
            }

            boolean result = this.updateById(product);
            if (!result) {
                log.error("商品更新失败：ID={}", product.getId());
                return false;
            }

            if (skuDTOList != null && !skuDTOList.isEmpty()) {
                QueryWrapper<ProductSku> skuQuery = new QueryWrapper<>();
                skuQuery.eq("product_id", product.getId());
                List<ProductSku> existSkus = productSkuMapper.selectList(skuQuery);

                for (ProductSku existSku : existSkus) {
                    QueryWrapper<ProductSkuAttribute> attrQuery = new QueryWrapper<>();
                    attrQuery.eq("sku_id", existSku.getId());
                    productSkuAttributeMapper.delete(attrQuery);
                }
                productSkuMapper.delete(skuQuery);

                for (ProductSkuDTO skuDTO : skuDTOList) {
                    ProductSku sku = BeanUtil.copyProperties(skuDTO, ProductSku.class);
                    sku.setProductId(product.getId());
                    if (sku.getSkuCode() == null || sku.getSkuCode().isEmpty()) {
                        sku.setSkuCode(generateSkuCode(product.getProductCode()));
                    }
                    if (sku.getStatus() == null) {
                        sku.setStatus(1);
                    }
                    productSkuMapper.insert(sku);

                    if (skuDTO.getAttributeList() != null && !skuDTO.getAttributeList().isEmpty()) {
                        for (ProductSkuAttributeDTO attrDTO : skuDTO.getAttributeList()) {
                            ProductSkuAttribute attr = BeanUtil.copyProperties(attrDTO, ProductSkuAttribute.class);
                            attr.setSkuId(sku.getId());
                            productSkuAttributeMapper.insert(attr);
                        }
                    }
                }
            }

            log.info("商品更新成功：ID={}, 编码={}", product.getId(), product.getProductCode());
            return true;

        } catch (Exception e) {
            log.error("商品更新异常：ID={}", productDTO.getId(), e);
            throw new RuntimeException("商品更新失败：" + e.getMessage());
        }
    }
    
    @Override
    public ProductVO getProductByCode(String productCode) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_code", productCode);
        Product product = this.getOne(queryWrapper);
        if (product == null) {
            return null;
        }
        return convertToProductVO(product);
    }
    
    @Override
    public String uploadProductImage(MultipartFile file, String productCode) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }
            
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String objectName = String.format("products/%s/%s/%s%s",
                    productCode, date, uuid, fileExtension);
            
            String ossUrl = ossService.uploadFile(file, objectName);
            
            log.info("商品图片上传成功：productCode={}, url={}", productCode, ossUrl);
            return ossUrl;
            
        } catch (Exception e) {
            log.error("商品图片上传失败：productCode={}", productCode, e);
            throw new RuntimeException("图片上传失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteProduct(Long productId) {
        try {
            Product product = this.getById(productId);
            if (product == null) {
                log.error("商品不存在：ID={}", productId);
                return false;
            }
            
            QueryWrapper<ProductSku> skuQuery = new QueryWrapper<>();
            skuQuery.eq("product_id", productId);
            List<ProductSku> skuList = productSkuMapper.selectList(skuQuery);
            
            for (ProductSku sku : skuList) {
                QueryWrapper<ProductSkuAttribute> attrQuery = new QueryWrapper<>();
                attrQuery.eq("sku_id", sku.getId());
                productSkuAttributeMapper.delete(attrQuery);
            }
            productSkuMapper.delete(skuQuery);
            
            boolean result = this.removeById(productId);
            
            if (result) {
                log.info("商品删除成功：ID={}, 编码={}", productId, product.getProductCode());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("商品删除异常：ID={}", productId, e);
            throw new RuntimeException("商品删除失败：" + e.getMessage());
        }
    }


    @Override
    public ProductVO getProductById(Long id) {
        Product product = this.getById(id);
        if (product == null) {
            return null;
        }
        return convertToProductVO(product);
    }

    @Override
    public Page<ProductVO> getProductVOPage(Integer pageNum, Integer pageSize, Integer status, String category, String brand) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq("category", category);
        }
        if (brand != null && !brand.isEmpty()) {
            queryWrapper.like("brand", brand);
        }

        queryWrapper.orderByDesc("created_at");
        Page<Product> productPage = this.page(page, queryWrapper);
        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<ProductVO> searchProductsPage(String keyword, Integer pageNum, Integer pageSize) {
        Page<Product> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        queryWrapper.and(wrapper -> wrapper
                .like("title", keyword)
                .or()
                .like("product_code", keyword)
                .or()
                .like("brand", keyword)
                .or()
                .like("category", keyword)
        );

        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("created_at");

        Page<Product> productPage = this.page(page, queryWrapper);

        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        voPage.setRecords(productPage.getRecords().stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<ProductVO> getProductVOList(Integer status, String category, String brand, String title) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq("category", category);
        }
        if (brand != null && !brand.isEmpty()) {
            queryWrapper.like("brand", brand);
        }

        queryWrapper.like(StringUtils.isNotEmpty(title), "title", title);

        queryWrapper.orderByDesc("created_at");
        List<Product> productList = this.list(queryWrapper);

        return productList.stream()
                .map(this::convertToProductVO)
                .collect(Collectors.toList());
    }

    private ProductVO convertToProductVO(Product product) {
        ProductVO vo = BeanUtil.copyProperties(product, ProductVO.class);
        List<ProductSku> skuList = getSkuListByProductId(product.getId());
        if (skuList != null && !skuList.isEmpty()) {
            List<ProductSkuVO> skuVOList = skuList.stream().map(sku -> {
                ProductSkuVO skuVO = BeanUtil.copyProperties(sku, ProductSkuVO.class);
                if (sku.getAttributeList() != null && !sku.getAttributeList().isEmpty()) {
                    List<ProductSkuAttributeVO> attrVOList = sku.getAttributeList().stream()
                            .map(attr -> BeanUtil.copyProperties(attr, ProductSkuAttributeVO.class))
                            .collect(Collectors.toList());
                    skuVO.setAttributeList(attrVOList);
                }
                return skuVO;
            }).collect(Collectors.toList());
            vo.setSkuList(skuVOList);
        }
        return vo;
    }

    private List<ProductSku> getSkuListByProductId(Long productId) {
        QueryWrapper<ProductSku> skuQuery = new QueryWrapper<>();
        skuQuery.eq("product_id", productId);
        List<ProductSku> skuList = productSkuMapper.selectList(skuQuery);
        
        for (ProductSku sku : skuList) {
            QueryWrapper<ProductSkuAttribute> attrQuery = new QueryWrapper<>();
            attrQuery.eq("sku_id", sku.getId());
            List<ProductSkuAttribute> attrList = productSkuAttributeMapper.selectList(attrQuery);
            sku.setAttributeList(attrList);
        }
        
        return skuList;
    }
    
    private String generateProductCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4);
        return "P" + timestamp + random;
    }
    
    private String generateSkuCode(String productCode) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        return "S" + productCode + "_" + timestamp;
    }
}