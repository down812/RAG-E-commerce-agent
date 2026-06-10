package com.ecommerceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerceserver.model.entity.Cart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {
}