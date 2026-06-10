package com.ecommerceserver.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerceserver.model.dto.AddToCartDTO;
import com.ecommerceserver.model.dto.CartItemDTO;
import com.ecommerceserver.model.vo.CartItemVO;
import com.ecommerceserver.model.vo.CartVO;

public interface CartService {
    /**
     * 添加商品到购物车
     */
    CartVO addToCart(Long userId, AddToCartDTO addToCartDTO);

    /**
     * 更新购物车项数量
     */
    CartVO updateCartItem(Long userId, CartItemDTO cartItemDTO);

    /**
     * 删除购物车项
     */
    CartVO removeCartItem(Long userId, Long cartItemId);

    /**
     * 获取用户购物车
     */
    CartVO getCartByUserId(Long userId, String keyword);

    /**
     * 清空购物车
     */
    void clearCart(Long userId);

    /**
     * 分页获取购物车项
     */
    Page<CartItemVO> getCartItemsByPage(Long userId, Integer pageNum, Integer pageSize, String keyword);

    /**
     * 根据ID获取购物车项详情
     */
    CartItemVO getCartItemDetail(Long userId, Long cartItemId);
}