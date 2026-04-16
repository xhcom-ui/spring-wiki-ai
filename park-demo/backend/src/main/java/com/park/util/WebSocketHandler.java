package com.park.util;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket处理器
 */
public class WebSocketHandler extends TextWebSocketHandler {

    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    /**
     * 发送消息给所有客户端
     */
    public static void sendMessage(Object message) {
        String jsonMessage = JSON.toJSONString(message);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 发送消息给指定客户端
     */
    public static void sendMessage(WebSocketSession session, Object message) {
        if (session.isOpen()) {
            try {
                String jsonMessage = JSON.toJSONString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
