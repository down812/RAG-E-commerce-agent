package com.ecommerceserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerceserver.model.entity.ChatSummary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSummaryMapper extends BaseMapper<ChatSummary> {
}