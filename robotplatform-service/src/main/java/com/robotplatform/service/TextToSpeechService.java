package com.robotplatform.service;

import org.springframework.web.socket.AbstractWebSocketMessage;

import java.util.function.Consumer;

/**
 * 语音合成服务接口
 */
public interface TextToSpeechService {

    /**
     * 合成语音
     * @param sessionId
     * @param text
     * @param speakerId
     * @param callback
     */
    void synthesizeSpeech(String sessionId,String text, String speakerId, Consumer<AbstractWebSocketMessage> callback);

    /**
     * 使用指令模式合成语音
     * @param sessionId
     * @param text         要合成的文本
     * @param instructText 指令文本（控制语音风格）
     * @return Base64编码的音频数据
     */

    void synthesizeSpeechWithInstruct2(String sessionId,String text, String instructText, byte[] promptAudio, Consumer<AbstractWebSocketMessage> callback);

    /**
     * 使用Zero-Shot模式合成语音
     * @param sessionId
     * @param text         要合成的文本
     * @param promptText  提示文本
     * @param promptAudio 提示音频（Base64编码）
     * @return Base64编码的音频数据
     */
    void synthesizeSpeechZeroShot(String sessionId,String text,String promptText, byte[] promptAudio, Consumer<AbstractWebSocketMessage> callback);

    /**
     * 使用指定的音色ID获取默认音色
     *
     * @return 默认的说话人ID
     */
    String getDefaultSpeakerId();

    /**
     * 关闭语音合成会话
     * @param sessionId
     */
    void closeSession(String sessionId);
} 