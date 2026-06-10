package com.ecommerceserver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerceserver.mapper.LogMapper;
import com.ecommerceserver.model.entity.Log;
import com.ecommerceserver.service.LogService;
import org.springframework.stereotype.Service;

@Service
public class LogServiceImpl extends ServiceImpl<LogMapper, Log> implements LogService {
}