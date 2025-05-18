package com.robotplatform.service.impl;

import com.robotplatform.service.TextToSpeechService;
import com.robotplatform.service.external.cosyvoice.CosyVoiceClient;
import com.robotplatform.service.external.cosyvoice.CosyVoiceConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.web.socket.AbstractWebSocketMessage;

import java.util.function.Consumer;

/**
 * 基于CosyVoice的语音合成服务实现
 */
@Slf4j
@Service
public class CosyVoiceTextToSpeechServiceImpl implements TextToSpeechService {

    @Resource
    private CosyVoiceClient cosyVoiceClient;
    @Autowired
    private CosyVoiceConfig config;

    @Override
    public void synthesizeSpeech(String sessionId, String text, String speakerId, Consumer<AbstractWebSocketMessage> callback) {
        try {
            log.info("合成语音, 文本: {}, 说话人ID: {},sessionId:{}", text, speakerId,sessionId);
            cosyVoiceClient.synthesizeSpeechStream(sessionId,text, speakerId, 1.0f, callback );
        } catch (Exception e) {
            log.error("语音合成失败", e);
        }
    }

    @Override
    public void synthesizeSpeechWithInstruct2(String sessionId, String text, String instructText, byte[] promptAudio, Consumer<AbstractWebSocketMessage> callback) {
        try {
            log.info("使用指令模式合成语音, 文本: {}, 指令文本: {} sessionId:{}", text, instructText,sessionId);
            cosyVoiceClient.synthesizeSpeechStreamInstruct2(sessionId,text, instructText, promptAudio, 1.0f, callback);
        } catch (Exception e) {
            log.error("指令模式语音合成失败", e);
        }
    }

    @Override
    public void synthesizeSpeechZeroShot(String sessionId, String text, String promptText, byte[] promptAudio, Consumer<AbstractWebSocketMessage> callback) {
        try {
            log.info("使用Zero-Shot模式合成语音, 提示文本: {},sessionId:{}", promptText,sessionId);
            cosyVoiceClient.synthesizeSpeechStreamZeroShot(sessionId,text, promptText, promptAudio, 1.0f, callback);
        } catch (Exception e) {
            log.error("Zero-Shot模式语音合成失败", e);
        }
    }

    @Override
    public String getDefaultSpeakerId() {
        return config.getVoice().getId();
    }

    @Override
    public void closeSession(String sessionId) {
        cosyVoiceClient.closeUserSession(sessionId);
    }
}