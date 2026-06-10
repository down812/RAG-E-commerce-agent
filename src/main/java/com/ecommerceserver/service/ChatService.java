package com.ecommerceserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerceserver.model.entity.Log;
import com.ecommerceserver.model.vo.AiMessage;
import com.ecommerceserver.model.vo.SessionInfo;
import com.ecommerceserver.model.vo.SessionList;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {
    Flux<AiMessage> analyse(String sessionId, String messageId, String content, List<MultipartFile> files);

    void clearHistory(String sessionId);

    List<SessionInfo> getSession(String sessionId);

    List<SessionList> getSessions();

    void stopChat(String sessionId, String messageId);
}