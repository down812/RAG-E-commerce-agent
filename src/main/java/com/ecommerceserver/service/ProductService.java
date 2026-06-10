package com.ecommerceserver.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerceserver.model.dto.ProductDTO;
import com.ecommerceserver.model.dto.ProductSkuDTO;
import com.ecommerceserver.model.entity.Product;
import com.ecommerceserver.model.vo.ProductVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService extends IService<Product> {

    boolean createProduct(ProductDTO productDTO, List<ProductSkuDTO> skuList);

    boolean updateProduct(ProductDTO productDTO, List<ProductSkuDTO> skuList);

    ProductVO getProductByCode(String productCode);

    String uploadProductImage(MultipartFile file, String productCode);

    boolean deleteProduct(Long productId);

    ProductVO getProductById(Long id);

    Page<ProductVO> getProductVOPage(Integer pageNum, Integer pageSize, Integer status, String category, String brand);

    Page<ProductVO> searchProductsPage(String keyword, Integer pageNum, Integer pageSize);

    List<ProductVO> getProductVOList(Integer status, String category, String brand, String title);
}