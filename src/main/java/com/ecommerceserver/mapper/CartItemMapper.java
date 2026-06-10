package com.ecommerceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerceserver.model.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}