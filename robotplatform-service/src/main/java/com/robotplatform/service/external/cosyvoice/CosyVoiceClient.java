package com.robotplatform.service.external.cosyvoice;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * CosyVoice客户端
 * 基于Python版客户端实现的Java版本
 */
@Slf4j
@Component
public class CosyVoiceClient {

    @Autowired
    private CosyVoiceConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Consumer<AbstractWebSocketMessage>> streamCallbacks = new ConcurrentHashMap<>();

    // 用户sessionId到ttsSessionId的映射
    private final Map<String, String> userSessionToTtsSession = new ConcurrentHashMap<>();
    // ttsSessionId到用户sessionId的映射
    private final Map<String, String> ttsSessionToUserSession = new ConcurrentHashMap<>();
    // 会话最后活动时间
    private final Map<String, Long> sessionLastActiveTime = new ConcurrentHashMap<>();

    // 心跳间隔（毫秒）
    private static final long HEARTBEAT_INTERVAL = 30000;
    // 会话超时时间（毫秒）
    private static final long SESSION_TIMEOUT = 600000; // 10分钟

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public CosyVoiceClient() {
        // 启动心跳和会话清理任务
        scheduler.scheduleAtFixedRate(this::checkSessions, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查会话状态并发送心跳
     */
    private void checkSessions() {
        long currentTime = System.currentTimeMillis();

        // 检查所有会话
        for (Map.Entry<String, Long> entry : sessionLastActiveTime.entrySet()) {
            String ttsSessionId = entry.getKey();
            long lastActiveTime = entry.getValue();

            // 如果会话超时，关闭会话
            if (currentTime - lastActiveTime > SESSION_TIMEOUT) {
                log.info("会话超时，关闭会话: {}", ttsSessionId);
                cleanupSession(ttsSessionId);
                continue;
            }

            // 发送心跳
            WebSocketSession session = sessions.get(ttsSessionId);
            if (session != null && session.isOpen()) {
                try {
                    Map<String, Object> heartbeat = Map.of("type", "heartbeat");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(heartbeat)));
                } catch (IOException e) {
                    log.error("发送心跳失败", e);
                    cleanupSession(ttsSessionId);
                }
            }
        }
    }

    /**
     * 获取或创建TTS会话
     */
    public String getOrCreateTtsSession(String userSessionId) {
        String ttsSessionId = userSessionToTtsSession.get(userSessionId);
        if (ttsSessionId == null) {
            ttsSessionId = generateSessionId();
            userSessionToTtsSession.put(userSessionId, ttsSessionId);
            ttsSessionToUserSession.put(ttsSessionId, userSessionId);
            sessionLastActiveTime.put(ttsSessionId, System.currentTimeMillis());
        }
        return ttsSessionId;
    }

    /**
     * 关闭用户会话
     */
    public void closeUserSession(String userSessionId) {
        String ttsSessionId = userSessionToTtsSession.remove(userSessionId);
        if (ttsSessionId != null) {
            cleanupSession(ttsSessionId);
        }
    }

    /**
     * 更新会话活动时间
     */
    private void updateSessionActivity(String ttsSessionId) {
        sessionLastActiveTime.put(ttsSessionId, System.currentTimeMillis());
    }

    /**
     * 流式合成语音（SFT模式）
     */
    public void synthesizeSpeechStream(String userSessionId, String text, String speakerId, float speed, Consumer<AbstractWebSocketMessage> callback) {
        synthesizeSpeechStream(userSessionId, "sft", text, speakerId, null, null, null, speed, callback);
    }

    /**
     * 流式合成语音（Zero-Shot模式）
     */
    public void synthesizeSpeechStreamZeroShot(String userSessionId, String text, String promptText, byte[] promptAudio, float speed,Consumer<AbstractWebSocketMessage> callback) {
        synthesizeSpeechStream(userSessionId, "zero_shot", text, null, promptText, promptAudio, null, speed, callback);
    }

    /**
     * 流式合成语音（Cross-Lingual模式）
     */
    public void synthesizeSpeechStreamCrossLingual(String userSessionId, String text, byte[] promptAudio, float speed, Consumer<AbstractWebSocketMessage> callback) {
        synthesizeSpeechStream(userSessionId, "cross_lingual", text, null, null, promptAudio, null, speed, callback);
    }

    /**
     * 流式合成语音（Instruct2模式）
     */
    public void synthesizeSpeechStreamInstruct2(String userSessionId, String text, String instructText, byte[] promptAudio, float speed, Consumer<AbstractWebSocketMessage> callback) {
        synthesizeSpeechStream(userSessionId, "instruct2", text, null, null, promptAudio, instructText, speed, callback);
    }

    /**
     * 流式合成语音（通用方法）
     */
    private void synthesizeSpeechStream(String userSessionId, String mode, String text, String speakerId,
                                        String promptText, byte[] promptAudio, String instructText, float speed, Consumer<AbstractWebSocketMessage> callback) {


        String sessionId = getOrCreateTtsSession(userSessionId);
        streamCallbacks.putIfAbsent(sessionId, callback);
        updateSessionActivity(sessionId);
        try {
            // 创建WebSocket会话
            WebSocketSession session = sessions.get(sessionId);
            if (session == null) {
                session = createSession(sessionId);
                sessions.put(sessionId, session);
            }
            // 准备请求数据
            Map<String, Object> payload = createPayload(mode, text, speakerId, promptText, instructText, speed);

            // 发送请求数据
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));

            // 如果需要发送示例音频
            if (promptAudio != null) {
                session.sendMessage(new BinaryMessage(ByteBuffer.wrap(promptAudio)));
            }

        } catch (Exception e) {
            log.error("流式语音合成失败", e);
            cleanupSession(sessionId);
            throw new RuntimeException("流式语音合成失败", e);
        }
    }

    /**
     * 创建WebSocket会话
     */
    private WebSocketSession createSession(String sessionId) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        String url = String.format("ws://%s:%d", config.getServer().getHost(), config.getServer().getPort());

        // 配置WebSocket会话
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        WebSocketHandler handler = new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
                log.info("WebSocket连接已建立, 会话ID: {}", sessionId);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
                try {
                    if (message instanceof TextMessage) {
                        handleTextMessage(sessionId, (TextMessage) message);
                    } else if (message instanceof BinaryMessage) {
                        handleBinaryMessage(sessionId, (BinaryMessage) message);
                    }
                } catch (Exception e) {
                    log.error("处理WebSocket消息失败", e);
                    handleError(sessionId, e);
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) {
                log.error("WebSocket传输错误", exception);
                handleError(sessionId, exception);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                log.info("WebSocket连接已关闭, 会话ID: {}, 状态: {}", sessionId, status);
                cleanupSession(sessionId);
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        };

        // 设置WebSocket会话配置
        WebSocketSession session = client.doHandshake(handler, headers, new URI(url)).get(config.getConnectTimeout(), TimeUnit.MILLISECONDS);

        // 设置消息大小限制
        //session.setMessageSizeLimit(1024 * 1024); // 设置为1MB
        session.setBinaryMessageSizeLimit(1024 * 1024); // 设置为1MB
        session.setTextMessageSizeLimit(1024 * 1024); // 设置为1MB

        return session;
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(String sessionId, TextMessage message) throws IOException {
        String payload = message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);

        Consumer<AbstractWebSocketMessage>  callback = streamCallbacks.get(sessionId);
        if(callback==null){
            log.info("tts会话已关闭， ttsSessionId: {}", sessionId);
            return ;
        }

        String type = (String) data.get("type");
        if ("error".equals(type)) {
            String errorMessage = (String) data.get("message");
            handleError(sessionId, new RuntimeException(errorMessage));
        } else if ("header".equals(type)) {
            // 处理头部信息
            callback.accept(message);
            //log.info("收到头部信息: {}", data);
        } else if ("chunk_info".equals(type)) {
            // 处理块信息
            callback.accept(message);
            //log.debug("收到块信息: {}", data);
        } else if ("end".equals(type)) {
            // 处理结束标记
            callback.accept(message);
            //log.info("收到结束标记: {}", data);

        }
        updateSessionActivity(sessionId);
    }

    /**
     * 处理二进制消息
     */
    private void handleBinaryMessage(String sessionId, BinaryMessage message) {

        // 调用流式回调
        Consumer<AbstractWebSocketMessage>  callback = streamCallbacks.get(sessionId);
        if(callback==null){
            log.info("tts会话已关闭， ttsSessionId: {}", sessionId);
            return ;
        }
        callback.accept(message);



    }

    /**
     * 处理错误
     */
    private void handleError(String sessionId, Throwable error) {

        cleanupSession(sessionId);
    }

    /**
     * 清理会话
     */
    private void cleanupSession(String sessionId) {
        WebSocketSession session = sessions.remove(sessionId);
        if (session != null) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭WebSocket会话失败", e);
            }
        }

        streamCallbacks.remove(sessionId);
        sessionLastActiveTime.remove(sessionId);

        // 清理用户会话映射
        String userSessionId = ttsSessionToUserSession.remove(sessionId);
        if (userSessionId != null) {
            userSessionToTtsSession.remove(userSessionId);
        }
    }

    /**
     * 创建请求数据
     */
    private Map<String, Object> createPayload(String mode, String text, String speakerId,
                                              String promptText, String instructText, float speed) {
        Map<String, Object> payload = Map.of(
                "mode", mode,
                "tts_text", text,
                "speed", speed
        );

        if (speakerId != null) {
            payload = Map.of(
                    "mode", mode,
                    "tts_text", text,
                    "spk_id", speakerId,
                    "speed", speed
            );
        }

        if (promptText != null) {
            payload = Map.of(
                    "mode", mode,
                    "tts_text", text,
                    "prompt_text", promptText,
                    "speed", speed
            );
        }

        if (instructText != null) {
            payload = Map.of(
                    "mode", mode,
                    "tts_text", text,
                    "instruct_text", instructText,
                    "speed", speed
            );
        }

        return payload;
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "cosyvoice-" + System.currentTimeMillis();
    }
}