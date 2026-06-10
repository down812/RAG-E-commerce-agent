package com.ecommerceserver.webSocket;

import cn.hutool.json.JSONUtil;
import com.ecommerceserver.exception.GlobalException;
import com.ecommerceserver.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket服务
 * fromUserId为当前连接userId
 * 支持一对一聊天并记录历史数据
 */
@Component
@ServerEndpoint(value = "/websocket/{fromUserId}")
public class WebSocketServer {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 线程安全Set，存放每个客户端对应的MyWebSocket对象
     */
    private static CopyOnWriteArraySet<WebSocketServer> WEBSOCKET_SET = new CopyOnWriteArraySet<>();

    /**
     * 客户端的连接会话
     */
    private Session session;

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        WEBSOCKET_SET.add(this);
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        WEBSOCKET_SET.remove(this);
        log.info("关闭websocket连接");
    }

    /**
     * 连接发生错误调用方法
     *
     * @param error 错误
     */
    @OnError
    public void onError(Throwable error) {
        log.error("websocket连接发生错误:{}",error);
        sendMessage("error");
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @return
     */
    @OnMessage
    public void onMessage(String message) {
        if (message.equals("close")){
            this.onClose();
        }else if (message.equals("ping")){
            sendMessage("pong");
        }
    }

    /**
     * 服务器主动推送，发送信息
     */
    public void sendMessage(Object message) {
        try {
            String msg = JSONUtil.toJsonStr(message);
            this.session.getBasicRemote().sendText(JSONUtil.toJsonStr(msg));
        } catch (IOException e) {
            throw new GlobalException(Result.error("websocket推送消息失败"));
        }
    }

    public static CopyOnWriteArraySet<WebSocketServer> getWebSocketSet() {
        return WEBSOCKET_SET;
    }
}
