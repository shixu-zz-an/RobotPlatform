package com.robotplatform.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 语音消息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceMessage implements Serializable {
    // 会话ID
    private String sessionId;
    
    // 消息类型: 'audio'(音频数据), 'text'(文本), 'transcription'(转录结果), 'error'(错误)
    private String type;
    
    // 音频数据(Base64编码)
    private String audioData;
    
    // 文本内容
    private String text;
    
    // 说话人ID
    private String speakerId;
    
    // 消息时间戳
    private Long timestamp;
    
    // 错误信息
    private String errorMessage;
    
    // 是否是最终结果
    private Boolean isFinal;
}