package com.robotplatform.service.external.funasr;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * FunASR客户端
 * 负责与FunASR服务器的WebSocket通信
 */
@Slf4j
public class FunasrClient {


    // 存储会话信息
    private FunasrConfig funasrConfig;
    private ConcurrentHashMap<String, TranscriptionListener> listeners = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();

    /**
     * 构造函数
     */
    public FunasrClient(FunasrConfig config) {
        this.funasrConfig = config;
    }

    /**
     * 创建连接
     *
     * @param listener 转录结果监听器
     */
    public String createConnection(TranscriptionListener listener) {
        String sessionId = UUID.randomUUID().toString();
        try {
            WebSocketSession webSocketSession = startWebSocketConnection(funasrConfig.getServerUrl(), listener);
            webSocketSessions.put(sessionId, webSocketSession);
            listeners.put(sessionId, listener);
            String initMsg = generateInitializationMessage(sessionId);
            webSocketSession.sendMessage(new TextMessage(initMsg));
            log.info("创建FunASR连接成功, 会话ID: {}, 初始配置: {}", sessionId, initMsg);
        } catch (Exception e) {
            log.error("创建FunASR连接失败, 会话ID: {}", sessionId, e);
            throw new RuntimeException("创建FunASR连接失败", e);
        }
        return sessionId;
    }

    /**
     * 发送音频数据
     *
     * @param funasrSessionId 会话ID
     * @param audioData       音频数据
     */
    public void sendAudioData(String funasrSessionId, byte[] audioData) {
        try {
            WebSocketSession webSocketSession = webSocketSessions.get(funasrSessionId);
            if (webSocketSession == null || !webSocketSession.isOpen()) {
                reconnect(funasrSessionId);
                webSocketSession = webSocketSessions.get(funasrSessionId);
            }
            webSocketSession.sendMessage(new BinaryMessage(audioData));
        } catch (IOException e) {
            log.error("发送音频数据失败, 会话ID: {}", funasrSessionId, e);
            throw new RuntimeException("发送音频数据失败", e);
        }
    }

    /**
     * 重新连接
     *
     * @param funasrSessionId 会话ID
     */
    private void reconnect(String funasrSessionId) {
        log.info("重新连接FunASR, 会话ID: {}", funasrSessionId);
        createConnection(listeners.get(funasrSessionId));
    }

    /**
     * 关闭连接
     *
     * @param funasrSessionId 会话ID
     */
    public void close(String funasrSessionId) {
        WebSocketSession webSocketSession = webSocketSessions.get(funasrSessionId);
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(generateEndMessage()));
                webSocketSession.close();
                log.info("关闭FunASR连接, 会话ID: {}", funasrSessionId);
            }
        } catch (IOException e) {
            log.error("关闭FunASR连接失败, 会话ID: {}", funasrSessionId, e);
        }
        webSocketSessions.remove(funasrSessionId);
        listeners.remove(funasrSessionId);
    }

    /**
     * 发送心跳包
     */
    public void sendHeartbeat() {
        webSocketSessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage("{\"type\":\"heartbeat\"}"));
                    //log.debug("发送心跳包, 会话ID: {}", sessionId);
                } catch (IOException e) {
                    log.error("发送心跳包失败, 会话ID: {}", sessionId, e);
                }
            }
        });
    }

    /**
     * 生成初始化消息
     *
     * @param sessionId 会话ID
     * @return 初始化消息JSON字符串
     */
    private String generateInitializationMessage(String sessionId) {
        JSONObject config = new JSONObject();

        JSONArray chunkSizes = new JSONArray();
        for (String size : funasrConfig.getDEFAULT_CHUNK_SIZE()) {
            chunkSizes.add(Integer.parseInt(size));
        }

        config.put("chunk_size", chunkSizes);
        config.put("wav_name", "websocket-" + sessionId);
        config.put("is_speaking", true);
        config.put("chunk_interval", funasrConfig.getDEFAULT_CHUNK_INTERVAL());
        config.put("itn", funasrConfig.DEFAULT_ITN);
        config.put("mode", funasrConfig.getDEFAULT_MODE());

        JSONObject hotWords = new JSONObject();
        hotWords.putAll(funasrConfig.getDEFAULT_HOTWORDS());
        config.put("hotwords", hotWords.toString());

        return config.toString();
    }

    /**
     * 生成结束消息
     *
     * @return 结束消息JSON字符串
     */
    private String generateEndMessage() {
        JSONObject endConfig = new JSONObject();
        endConfig.put("is_speaking", false);
        return endConfig.toString();
    }

    /**
     * 启动WebSocket连接
     *
     * @param serverUrl 服务器URL
     * @param listener  转录结果监听器
     * @return WebSocket会话
     */
    private WebSocketSession startWebSocketConnection(String serverUrl, TranscriptionListener listener) throws ExecutionException, InterruptedException {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.doHandshake(new FunasrWebSocketHandler(listener), serverUrl).get();
        return session;
    }

    /**
     * FunASR WebSocket处理器
     */
    private class FunasrWebSocketHandler extends AbstractWebSocketHandler {

        private final TranscriptionListener listener;

        public FunasrWebSocketHandler(TranscriptionListener listener) {
            this.listener = listener;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            String payload = message.getPayload();

            try {
                JSONObject json = JSONObject.parseObject(payload);
                String mode = json.getString("mode");
                String text = json.getString("text");
                
                // 处理所有转录模式
                if (text != null && !text.trim().isEmpty()) {
                    log.debug("收到FunASR消息, mode={}, text={}", mode, text);
                    
                    TranscriptionResult result = new TranscriptionResult();
                    result.setText(text);
                    result.setMode(mode);
                    result.setSpeakerId("speaker1");
                    result.setTimeStamp(System.currentTimeMillis());
                    
                    // 根据模式设置实时和最终标志
                    result.setRealtime("2pass-online".equals(mode) || "online".equals(mode));
                    result.setIsFinal("2pass-offline".equals(mode));
                    
                    listener.onTranscriptionResult(result);
                }
            } catch (Exception e) {
                log.error("解析FunASR转录结果失败", e);
            }
        }
    }
}