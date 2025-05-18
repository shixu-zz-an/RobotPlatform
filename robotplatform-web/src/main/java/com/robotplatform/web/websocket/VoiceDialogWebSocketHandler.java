package com.robotplatform.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.robotplatform.client.model.VoiceMessage;
import com.robotplatform.service.VoiceDialogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 语音对话WebSocket处理器 - 基于Spring WebSocket
 */
@Slf4j
public class VoiceDialogWebSocketHandler extends TextWebSocketHandler {

    // 存储所有WebSocket连接
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储会话ID映射
    private final Map<String, String> sessionIdMap = new ConcurrentHashMap<>();

    @Autowired
    private VoiceDialogService voiceDialogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URI路径中获取会话ID
        String sessionId = extractSessionId(session);
        sessions.put(sessionId, session);
        sessionIdMap.put(session.getId(), sessionId);
        log.info("新的WebSocket连接建立, 会话ID: {}, 当前连接数: {}", sessionId, sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (session == null) {
            log.warn("收到空WebSocket会话的关闭事件");
            return;
        }

        String sessionId = sessionIdMap.remove(session.getId());
        if (sessionId != null) {
            sessions.remove(sessionId);

            try {
                // 清理会话资源
                voiceDialogService.resetSession(sessionId);
                log.info("WebSocket连接关闭, 会话ID: {}, 状态码: {}, 原因: {}, 当前连接数: {}",
                        sessionId,
                        status.getCode(),  // 获取关闭状态码（如1000表示正常关闭）
                        status.getReason(),// 获取关闭原因描述
                        sessions.size());
            } catch (Exception e) {
                log.error("WebSocket连接关闭时清理资源失败, 会话ID: {}, 关闭状态: {}/{}",
                        sessionId,
                        status.getCode(),
                        status.getReason(),
                        e);
            }
        } else {
            log.debug("WebSocket连接关闭, 无法找到对应会话ID, WebSocket ID: {}, 关闭状态: {}/{}",
                    session.getId(),
                    status.getCode(),
                    status.getReason());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = sessionIdMap.get(session.getId());
        if (sessionId == null) {
            log.error("无法找到会话ID, WebSocketSession ID: {}", session.getId());
            return;
        }

        try {
            String payload = message.getPayload();
            //log.info("收到客户端消息, 会话ID: {}, 消息长度: {}", sessionId, payload.length());

            // 解析客户端消息
            VoiceMessage voiceMessage = objectMapper.readValue(payload, VoiceMessage.class);

            // 设置会话ID
            voiceMessage.setSessionId(sessionId);

            // 创建响应回调
            Consumer<VoiceMessage> responseCallback = response -> {
                try {
                    // 发送响应给客户端
                    String responseJson = objectMapper.writeValueAsString(response);
                    WebSocketSession webSocketSession = sessions.get(sessionId);
                    if (webSocketSession != null && webSocketSession.isOpen()) {
                        webSocketSession.sendMessage(new TextMessage(responseJson));
                    }
                } catch (IOException e) {
                    log.error("发送响应失败", e);
                }
            };

            // 处理消息
            voiceDialogService.processVoiceMessage(voiceMessage, responseCallback);

        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = sessionIdMap.get(session.getId());
        log.error("WebSocket传输错误, 会话ID: " + sessionId, exception);
    }

    /**
     * 从WebSocketSession中提取会话ID
     */
    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(path).build();
        String[] pathSegments = uriComponents.getPathSegments().toArray(new String[0]);

        // 假设路径格式为 /ws/voice/{sessionId}
        if (pathSegments.length > 2) {
            return pathSegments[2]; // 返回路径中的sessionId
        }

        // 如果路径中没有会话ID，则使用WebSocketSession ID
        return session.getId();
    }
} 