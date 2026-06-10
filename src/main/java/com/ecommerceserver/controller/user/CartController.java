package com.ecommerceserver.controller.user;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.model.dto.AddToCartDTO;
import com.ecommerceserver.model.dto.CartItemDTO;
import com.ecommerceserver.model.vo.CartItemVO;
import com.ecommerceserver.model.vo.CartVO;
import com.ecommerceserver.result.Result;
import com.ecommerceserver.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Tag(name = "购物车管理")
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    @Operation(summary = "添加商品到购物车", description = "将商品添加到用户购物车")
    public Result<CartVO> addToCart(
            @Valid @RequestBody AddToCartDTO addToCartDTO) {
        Long userId = LoginContext.getUserId();
        try {
            CartVO cartVO = cartService.addToCart(userId, addToCartDTO);
            return Result.success(cartVO);
        } catch (Exception e) {
            log.error("添加商品到购物车异常：userId={}", userId, e);
            return Result.error("添加商品到购物车失败：" + e.getMessage());
        }
    }

    @PutMapping("/update")
    @Operation(summary = "更新购物车项", description = "更新购物车中商品的数量")
    public Result<CartVO> updateCartItem(
            @Valid @RequestBody CartItemDTO cartItemDTO) {
        Long userId = LoginContext.getUserId();
        try {
            CartVO cartVO = cartService.updateCartItem(userId, cartItemDTO);
            return Result.success(cartVO);
        } catch (Exception e) {
            log.error("更新购物车项异常：userId={}", userId, e);
            return Result.error("更新购物车项失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/remove/{cartItemId}")
    @Operation(summary = "删除购物车项", description = "从购物车中删除指定商品")
    public Result<CartVO> removeCartItem(
            @PathVariable @Parameter(description = "购物车项ID") Long cartItemId) {
        Long userId = LoginContext.getUserId();
        try {
            CartVO cartVO = cartService.removeCartItem(userId, cartItemId);
            return Result.success(cartVO);
        } catch (Exception e) {
            log.error("删除购物车项异常：userId={}, cartItemId={}", userId, cartItemId, e);
            return Result.error("删除购物车项失败：" + e.getMessage());
        }
    }

    @GetMapping("/get")
    @Operation(summary = "获取购物车", description = "获取用户购物车信息，支持按商品名称模糊查询")
    public Result<CartVO> getCart(
            @RequestParam(required = false) @Parameter(description = "商品名称关键词") String keyword) {
        Long userId = LoginContext.getUserId();
        try {
            CartVO cartVO = cartService.getCartByUserId(userId, keyword);
            return Result.success(cartVO);
        } catch (Exception e) {
            log.error("获取购物车异常：userId={}", userId, e);
            return Result.error("获取购物车失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空购物车", description = "清空用户购物车所有商品")
    public Result<Void> clearCart() {
        Long userId = LoginContext.getUserId();
        try {
            cartService.clearCart(userId);
            return Result.success();
        } catch (Exception e) {
            log.error("清空购物车异常：userId={}", userId, e);
            return Result.error("清空购物车失败：" + e.getMessage());
        }
    }

    @GetMapping("/items/page")
    @Operation(summary = "分页获取购物车项", description = "分页获取用户购物车中的商品项，支持按商品名称模糊查询")
    public Result<Page<CartItemVO>> getCartItemsByPage(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer pageNum,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "商品名称关键词") String keyword) {
        Long userId = LoginContext.getUserId();
        try {
            Page<CartItemVO> cartItemPage = cartService.getCartItemsByPage(userId, pageNum, pageSize, keyword);
            return Result.success(cartItemPage);
        } catch (Exception e) {
            log.error("分页获取购物车项异常：userId={}", userId, e);
            return Result.error("分页获取购物车项失败：" + e.getMessage());
        }
    }

    @GetMapping("/item/detail/{cartItemId}")
    @Operation(summary = "获取购物车项详情", description = "根据购物车项ID获取详细信息")
    public Result<CartItemVO> getCartItemDetail(
            @PathVariable @Parameter(description = "购物车项ID") Long cartItemId) {
        Long userId = LoginContext.getUserId();
        try {
            CartItemVO cartItemVO = cartService.getCartItemDetail(userId, cartItemId);
            return Result.success(cartItemVO);
        } catch (Exception e) {
            log.error("获取购物车项详情异常：userId={}, cartItemId={}", userId, cartItemId, e);
            return Result.error("获取购物车项详情失败：" + e.getMessage());
        }
    }


}