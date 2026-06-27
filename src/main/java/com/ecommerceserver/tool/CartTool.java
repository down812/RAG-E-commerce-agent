package com.ecommerceserver.tool;

import com.ecommerceserver.context.LoginContext;
import com.ecommerceserver.model.dto.AddToCartDTO;
import com.ecommerceserver.model.vo.CartVO;
import com.ecommerceserver.service.CartService;
import com.ecommerceserver.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CartTool {
    private final CartService cartService;

    @Tool(description = "添加商品到购物车，并返回新加的购物车ID")
    public String AddToCart(AddToCartDTO  addToCartDTO) {
        Long userId = LoginContext.getUserId();
        CartVO cartVO = cartService.addToCart(userId, addToCartDTO);
        return String.valueOf(cartVO.getCartId());
    }

}
