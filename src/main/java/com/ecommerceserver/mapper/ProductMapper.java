package com.ecommerceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerceserver.model.entity.DataSet;
import com.ecommerceserver.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
