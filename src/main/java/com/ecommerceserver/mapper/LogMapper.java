package com.ecommerceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerceserver.model.entity.Log;
import org.apache.ibatis.annotations.Mapper;


/**
 * @author dawn
 * @date 2025-04-10 21:17
 */

@Mapper
public interface LogMapper extends BaseMapper<Log> {
}