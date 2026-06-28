package com.ecommerceserver.tool;

import com.ecommerceserver.model.dto.AddToCartDTO;
import com.ecommerceserver.model.vo.CartVO;
import com.ecommerceserver.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CartTool {
    private final CartService cartService;

    @Tool(description = "【仅当用户明确要求加入购物车时调用】将指定商品规格加入当前用户的购物车。" +
            "调用前必须满足：1) 已通过getProductSkus确认了要加购的skuId；2) 用户已明确告知购买数量。" +
            "若用户未说明数量，不要调用本工具，应先反问用户购买几件。")
    public String addToCart(
            @ToolParam(description = "商品ID, 必须从工具调用中获取，不得擅自修改") Long productId,
            @ToolParam(description = "商品SKU ID（规格ID），需从getProductSkus返回的skus列表中选取") Long skuId,
            @ToolParam(description = "购买数量，必须由用户明确告知，不得擅自默认") Integer quantity) {

        // userId 从当前请求作用域的上下文取（工具运行在 reactor 线程，ThreadLocal 的 LoginContext 取不到）。
        // 复用 ContextChatMemoryAdvisor 在流处理期间维护的 contextDataCache。
        Long userId = AiToolUserContext.currentUserId();
        if (userId == null || userId <= 0) {
            return "无法加入购物车：未能识别当前登录用户，请确认已登录后重试。";
        }
        if (productId == null || skuId == null) {
            return "无法加入购物车：缺少商品或规格信息，请先确认要购买的商品规格。";
        }
        if (quantity == null || quantity < 1) {
            return "请告知您要购买的数量（至少1件），我再为您加入购物车。";
        }

        try {
            AddToCartDTO dto = AddToCartDTO.builder()
                    .productId(productId)
                    .skuId(skuId)
                    .quantity(quantity)
                    .build();
            CartVO cartVO = cartService.addToCart(userId, dto);
            log.info("【加入购物车】userId={}, productId={}, skuId={}, quantity={}, cartId={}",
                    userId, productId, skuId, quantity, cartVO.getCartId());
            return "已成功将商品（数量：" + quantity + "）加入购物车，当前购物车共有 "
                    + cartVO.getTotalItems() + " 件商品。";
        } catch (Exception e) {
            log.warn("【加入购物车失败】userId={}, productId={}, skuId={}, 原因={}",
                    userId, productId, skuId, e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "未知错误";
            if (msg.contains("SKU")) {
                return "加入购物车失败：该商品规格不存在，请通过getProductSkus重新确认可选规格。";
            }
            if (msg.contains("商品")) {
                return "加入购物车失败：该商品不存在或已下架。";
            }
            return "加入购物车失败：" + msg;
        }
    }
}
