package com.robotplatform.service;

import com.robotplatform.client.model.VoiceMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 语音对话服务接口
 */
public interface VoiceDialogService {

    /**
     * 处理语音消息
     * 
     * @param voiceMessage 语音消息
     * @param responseCallback 响应回调函数
     */
    void processVoiceMessage(VoiceMessage voiceMessage, Consumer<VoiceMessage> responseCallback);

    /**
     * 重置会话状态
     * 
     * @param sessionId 会话ID
     */
    void resetSession(String sessionId);
    
    /**
     * 获取会话历史记录
     * 
     * @param sessionId 会话ID
     * @return 对话历史记录
     */
    List<VoiceMessage> getSessionHistory(String sessionId);
    
    /**
     * 创建新的会话
     * 
     * @return 新的会话ID
     */
    String createSession();
} 