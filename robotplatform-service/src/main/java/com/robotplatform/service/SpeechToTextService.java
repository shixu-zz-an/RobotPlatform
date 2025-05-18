package com.robotplatform.service;

import com.robotplatform.service.external.funasr.TranscriptionListener;

/**
 * 语音识别服务接口
 */
public interface SpeechToTextService {

    /**
     * 创建语音识别会话
     * 
     * @param listener 转录结果监听器
     * @return 会话ID
     */
    String createSession(TranscriptionListener listener);
    
    /**
     * 发送音频数据进行识别
     * 
     * @param sessionId 会话ID
     * @param audioData 音频数据
     */
    void sendAudioData(String sessionId, byte[] audioData);
    
    /**
     * 关闭语音识别会话
     * 
     * @param sessionId 会话ID
     */
    void closeSession(String sessionId);
    
    /**
     * 重置语音识别会话
     * 
     * @param sessionId 会话ID
     */
    void resetSession(String sessionId);
} 