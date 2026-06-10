package com.ecommerceserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerceserver.model.dto.AddToCartDTO;
import com.ecommerceserver.model.dto.CartItemDTO;
import com.ecommerceserver.model.entity.Cart;
import com.ecommerceserver.model.entity.CartItem;
import com.ecommerceserver.model.entity.Product;
import com.ecommerceserver.model.entity.ProductSku;
import com.ecommerceserver.model.entity.ProductSkuAttribute;
import com.ecommerceserver.mapper.CartMapper;
import com.ecommerceserver.mapper.CartItemMapper;
import com.ecommerceserver.mapper.ProductMapper;
import com.ecommerceserver.mapper.ProductSkuMapper;
import com.ecommerceserver.mapper.ProductSkuAttributeMapper;
import com.ecommerceserver.model.vo.CartItemVO;
import com.ecommerceserver.model.vo.CartVO;
import com.ecommerceserver.model.vo.ProductSkuAttributeVO;
import com.ecommerceserver.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductSkuAttributeMapper productSkuAttributeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO addToCart(Long userId, AddToCartDTO addToCartDTO) {
        // 1. 获取或创建购物车
        Cart cart = getOrCreateCart(userId);

        // 2. 检查商品和SKU是否存在、检查SKU是否属于该商品
        Product product = productMapper.selectById(addToCartDTO.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        ProductSku sku = productSkuMapper.selectOne(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, addToCartDTO.getProductId())
                .eq(ProductSku::getId, addToCartDTO.getSkuId()));

        if (sku == null) {
            throw new RuntimeException("SKU不存在或不属于该商品");
        }

        // 4. 检查购物车中是否已存在该商品SKU
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getCartId, cart.getId())
                .eq(CartItem::getProductId, addToCartDTO.getProductId())
                .eq(CartItem::getSkuId, addToCartDTO.getSkuId());
        CartItem existingItem = cartItemMapper.selectOne(queryWrapper);

        if (existingItem != null) {
            // 更新数量
            existingItem.setQuantity(existingItem.getQuantity() + addToCartDTO.getQuantity());
            existingItem.setSubtotal(existingItem.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            cartItemMapper.updateById(existingItem);
        } else {
            // 新增购物车项
            CartItem newItem = CartItem.builder()
                    .cartId(cart.getId())
                    .productId(addToCartDTO.getProductId())
                    .skuId(addToCartDTO.getSkuId())
                    .quantity(addToCartDTO.getQuantity())
                    .price(sku.getPrice())
                    .subtotal(sku.getPrice().multiply(BigDecimal.valueOf(addToCartDTO.getQuantity())))
                    .build();
            cartItemMapper.insert(newItem);
        }

        // 5. 更新购物车总金额和商品总数
        updateCartSummary(cart);

        // 6. 返回购物车VO
        return getCartVO(cart, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO updateCartItem(Long userId, CartItemDTO cartItemDTO) {
        // 1. 获取购物车项
        CartItem cartItem = cartItemMapper.selectById(cartItemDTO.getCartItemId());
        if (cartItem == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 2. 验证购物车项属于该用户
        Cart cart = cartMapper.selectById(cartItem.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该购物车项");
        }

        // 3. 检查库存
        ProductSku sku = productSkuMapper.selectById(cartItem.getSkuId());
        if (sku == null) {
            throw new RuntimeException("SKU不存在");
        }

        // 4. 更新数量和金额
        cartItem.setQuantity(cartItemDTO.getQuantity());
        cartItem.setSubtotal(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItemDTO.getQuantity())));
        cartItemMapper.updateById(cartItem);

        // 5. 更新购物车汇总
        updateCartSummary(cart);

        // 6. 返回购物车VO
        return getCartVO(cart, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CartVO removeCartItem(Long userId, Long cartItemId) {
        // 1. 获取购物车项
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 2. 验证购物车项属于该用户
        Cart cart = cartMapper.selectById(cartItem.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该购物车项");
        }

        // 3. 删除购物车项
        cartItemMapper.deleteById(cartItemId);

        // 4. 更新购物车汇总
        updateCartSummary(cart);

        // 5. 返回购物车VO
        return getCartVO(cart, null);
    }

    @Override
    public CartVO getCartByUserId(Long userId, String keyword) {
        // 1. 获取购物车
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        Cart cart = cartMapper.selectOne(queryWrapper);

        if (cart == null) {
            // 返回空购物车
            return CartVO.builder()
                    .userId(userId)
                    .cartItems(new ArrayList<>())
                    .totalItems(0)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }

        // 2. 返回购物车VO（带模糊查询）
        return getCartVO(cart, keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        // 1. 获取购物车
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        Cart cart = cartMapper.selectOne(queryWrapper);

        if (cart != null) {
            // 2. 删除所有购物车项
            LambdaQueryWrapper<CartItem> itemQueryWrapper = new LambdaQueryWrapper<>();
            itemQueryWrapper.eq(CartItem::getCartId, cart.getId());
            cartItemMapper.delete(itemQueryWrapper);

            // 3. 更新购物车汇总
            updateCartSummary(cart);
        }
    }

    @Override
    public Page<CartItemVO> getCartItemsByPage(Long userId, Integer pageNum, Integer pageSize, String keyword) {
        // 1. 获取购物车
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        Cart cart = cartMapper.selectOne(queryWrapper);

        if (cart == null) {
            // 返回空分页结果
            Page<CartItemVO> emptyPage = new Page<>(pageNum, pageSize);
            emptyPage.setRecords(new ArrayList<>());
            emptyPage.setTotal(0);
            return emptyPage;
        }

        // 2. 如果有模糊查询关键词，需要先查询所有购物车项再过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            return getCartItemsByPageWithKeyword(cart, pageNum, pageSize, keyword.trim());
        }

        // 3. 分页查询购物车项
        Page<CartItem> cartItemPage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CartItem> itemQueryWrapper = new LambdaQueryWrapper<>();
        itemQueryWrapper.eq(CartItem::getCartId, cart.getId())
                .orderByDesc(CartItem::getCreatedAt);
        Page<CartItem> pageResult = cartItemMapper.selectPage(cartItemPage, itemQueryWrapper);

        // 4. 转换为VO
        List<CartItemVO> cartItemVOs = pageResult.getRecords().stream()
                .map(this::convertToCartItemVO)
                .collect(Collectors.toList());

        // 5. 构建分页结果
        Page<CartItemVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setRecords(cartItemVOs);
        voPage.setTotal(pageResult.getTotal());
        voPage.setPages(pageResult.getPages());

        return voPage;
    }

    @Override
    public CartItemVO getCartItemDetail(Long userId, Long cartItemId) {
        // 1. 获取购物车项
        CartItem cartItem = cartItemMapper.selectById(cartItemId);
        if (cartItem == null) {
            throw new RuntimeException("购物车项不存在");
        }

        // 2. 验证购物车项属于该用户
        Cart cart = cartMapper.selectById(cartItem.getCartId());
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权查看该购物车项");
        }

        // 3. 转换为VO并返回
        return convertToCartItemVO(cartItem);
    }

    /**
     * 将CartItem转换为CartItemVO
     */
    private CartItemVO convertToCartItemVO(CartItem item) {
        Product product = productMapper.selectById(item.getProductId());
        ProductSku sku = productSkuMapper.selectById(item.getSkuId());

        String skuSpec = "";
        List<ProductSkuAttributeVO> skuAttributeVOs = new ArrayList<>();

        if (sku != null) {
            // 查询SKU属性列表
            LambdaQueryWrapper<ProductSkuAttribute> attrQueryWrapper = new LambdaQueryWrapper<>();
            attrQueryWrapper.eq(ProductSkuAttribute::getSkuId, item.getSkuId());
            List<ProductSkuAttribute> skuAttributes = productSkuAttributeMapper.selectList(attrQueryWrapper);

            // 转换为VO
            skuAttributeVOs = skuAttributes.stream()
                    .map(attr -> ProductSkuAttributeVO.builder()
                            .id(attr.getId())
                            .skuId(attr.getSkuId())
                            .attrName(attr.getAttrName())
                            .attrValue(attr.getAttrValue())
                            .build())
                    .collect(Collectors.toList());

            // 构建规格描述字符串（如：颜色:黑色, 容量:128GB）
            if (!skuAttributeVOs.isEmpty()) {
                skuSpec = skuAttributeVOs.stream()
                        .map(attr -> attr.getAttrName() + ":" + attr.getAttrValue())
                        .collect(Collectors.joining(", "));
            } else {
                skuSpec = "SKU:" + sku.getSkuCode();
            }
        }

        return CartItemVO.builder()
                .cartItemId(item.getId())
                .productId(item.getProductId())
                .productName(product != null ? product.getTitle() : "")
                .productImage(product != null ? product.getMainImageUrl() : "")
                .skuId(item.getSkuId())
                .skuSpec(skuSpec)
                .skuAttributes(skuAttributeVOs)
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    /**
     * 获取或创建购物车
     */
    private Cart getOrCreateCart(Long userId) {
        LambdaQueryWrapper<Cart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cart::getUserId, userId);
        Cart cart = cartMapper.selectOne(queryWrapper);

        if (cart == null) {
            cart = Cart.builder()
                    .userId(userId)
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .build();
            cartMapper.insert(cart);
        }

        return cart;
    }

    /**
     * 更新购物车汇总信息
     */
    private void updateCartSummary(Cart cart) {
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getCartId, cart.getId());
        List<CartItem> cartItems = cartItemMapper.selectList(queryWrapper);

        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        BigDecimal totalAmount = cartItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setTotalItems(totalItems);
        cart.setTotalAmount(totalAmount);
        cartMapper.updateById(cart);
    }

    /**
     * 构建购物车VO（支持模糊查询）
     */
    private CartVO getCartVO(Cart cart, String keyword) {
        // 1. 获取购物车项
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getCartId, cart.getId());
        List<CartItem> cartItems = cartItemMapper.selectList(queryWrapper);

        // 2. 转换为VO
        List<CartItemVO> cartItemVOs = cartItems.stream()
                .map(this::convertToCartItemVO)
                .collect(Collectors.toList());

        // 3. 如果有模糊查询关键词，进行过滤
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.trim().toLowerCase();
            cartItemVOs = cartItemVOs.stream()
                    .filter(itemVO -> {
                        // 检查商品名称是否包含关键词
                        if (itemVO.getProductName() != null && 
                            itemVO.getProductName().toLowerCase().contains(lowerKeyword)) {
                            return true;
                        }
                        // 检查SKU规格描述是否包含关键词
                        if (itemVO.getSkuSpec() != null && 
                            itemVO.getSkuSpec().toLowerCase().contains(lowerKeyword)) {
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        // 4. 重新计算总数和总金额
        int totalItems = cartItemVOs.stream().mapToInt(CartItemVO::getQuantity).sum();
        BigDecimal totalAmount = cartItemVOs.stream()
                .map(CartItemVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. 构建CartVO
        return CartVO.builder()
                .cartId(cart.getId())
                .userId(cart.getUserId())
                .cartItems(cartItemVOs)
                .totalItems(totalItems)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * 分页获取购物车项（带模糊查询）
     */
    private Page<CartItemVO> getCartItemsByPageWithKeyword(Cart cart, Integer pageNum, Integer pageSize, String keyword) {
        // 1. 查询所有购物车项
        LambdaQueryWrapper<CartItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CartItem::getCartId, cart.getId())
                .orderByDesc(CartItem::getCreatedAt);
        List<CartItem> allCartItems = cartItemMapper.selectList(queryWrapper);

        // 2. 转换为VO
        List<CartItemVO> allCartItemVOs = allCartItems.stream()
                .map(this::convertToCartItemVO)
                .collect(Collectors.toList());

        // 3. 根据关键词过滤
        String lowerKeyword = keyword.toLowerCase();
        List<CartItemVO> filteredItems = allCartItemVOs.stream()
                .filter(itemVO -> {
                    // 检查商品名称是否包含关键词
                    if (itemVO.getProductName() != null && 
                        itemVO.getProductName().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    // 检查SKU规格描述是否包含关键词
                    if (itemVO.getSkuSpec() != null && 
                        itemVO.getSkuSpec().toLowerCase().contains(lowerKeyword)) {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        // 4. 手动分页
        long total = filteredItems.size();
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredItems.size());
        
        List<CartItemVO> pagedItems = new ArrayList<>();
        if (fromIndex < filteredItems.size()) {
            pagedItems = filteredItems.subList(fromIndex, toIndex);
        }

        // 5. 构建分页结果
        Page<CartItemVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setRecords(pagedItems);
        voPage.setTotal(total);
        voPage.setPages((total + pageSize - 1) / pageSize);

        return voPage;
    }
}