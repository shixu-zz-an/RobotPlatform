package com.robotplatform.service.impl;

import com.robotplatform.service.SpeechToTextService;
import com.robotplatform.service.external.funasr.FunasrClient;
import com.robotplatform.service.external.funasr.FunasrConfig;
import com.robotplatform.service.external.funasr.TranscriptionListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 基于FunASR的语音识别服务实现
 */
@Slf4j
@Service
public class FunASRSpeechToTextServiceImpl implements SpeechToTextService {


    @Autowired
    private FunasrConfig config;
    private FunasrClient funasrClient;
    private ScheduledExecutorService scheduledExecutorService;
    private ConcurrentHashMap<String, String> sessionMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            
            // 初始化FunASR客户端
            funasrClient = new FunasrClient(config);
            
            // 创建定时任务，定期发送心跳包
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorService.scheduleAtFixedRate(
                    this::sendHeartbeat,
                    config.getHeartbeatInterval(),
                    config.getHeartbeatInterval(),
                    TimeUnit.SECONDS
            );
            
            log.info("FunASR语音识别服务初始化完成, 服务器地址: {}", config.getServerUrl());
        } catch (Exception e) {
            log.error("FunASR语音识别服务初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
        
        // 关闭所有会话
        sessionMap.forEach((sessionId, funasrSessionId) -> {
            try {
                funasrClient.close(funasrSessionId);
            } catch (Exception e) {
                log.error("关闭FunASR会话失败, 会话ID: {}", sessionId, e);
            }
        });
        
        log.info("FunASR语音识别服务已关闭");
    }

    @Override
    public String createSession(TranscriptionListener listener) {
        String funasrSessionId = funasrClient.createConnection(listener);
        String sessionId = "asr-" + funasrSessionId;
        sessionMap.put(sessionId, funasrSessionId);
        log.info("创建FunASR会话, 会话ID: {}, FunASR会话ID: {}", sessionId, funasrSessionId);
        return sessionId;
    }

    @Override
    public void sendAudioData(String sessionId, byte[] audioData) {
        String funasrSessionId = sessionMap.get(sessionId);
        if (funasrSessionId == null) {
            log.error("无法找到FunASR会话, 会话ID: {}", sessionId);
            return;
        }
        
        try {
            funasrClient.sendAudioData(funasrSessionId, audioData);
        } catch (Exception e) {
            log.error("发送音频数据失败, 会话ID: {}", sessionId, e);
        }
    }

    @Override
    public void closeSession(String sessionId) {
        // 添加参数有效性检查
        if (sessionId == null) {
            log.warn("尝试关闭空会话ID的FunASR会话");
            return;
        }
        
        String funasrSessionId = sessionMap.remove(sessionId);
        if (funasrSessionId != null) {
            try {
                funasrClient.close(funasrSessionId);
                log.info("关闭FunASR会话, 会话ID: {}, FunASR会话ID: {}", sessionId, funasrSessionId);
            } catch (Exception e) {
                log.error("关闭FunASR会话失败, 会话ID: {}", sessionId, e);
            }
        } else {
            log.debug("找不到FunASR会话映射, 会话ID: {}", sessionId);
        }
    }

    @Override
    public void resetSession(String sessionId) {
        // 先关闭旧会话
        closeSession(sessionId);
    }
    
    /**
     * 发送心跳包
     */
    private void sendHeartbeat() {
        try {
            funasrClient.sendHeartbeat();
        } catch (Exception e) {
            log.error("发送FunASR心跳包失败", e);
        }
    }
}