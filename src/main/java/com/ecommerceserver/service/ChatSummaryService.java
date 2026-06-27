package com.ecommerceserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerceserver.model.entity.ChatSummary;

public interface ChatSummaryService extends IService<ChatSummary> {
    void generateSummaryAsync(String sessionId);
}
