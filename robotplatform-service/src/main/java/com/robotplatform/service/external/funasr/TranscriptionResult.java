package com.robotplatform.service.external.funasr;

import lombok.Data;

/**
 * 转录结果类
 */
@Data
public class TranscriptionResult {
    
    /**
     * 转录文本内容
     */
    private String text;
    
    /**
     * 说话人ID
     */
    private String speakerId;
    
    /**
     * 时间戳
     */
    private Long timeStamp;
    
    /**
     * 转录模式
     */
    private String mode;
    
    /**
     * 是否为实时转录结果
     */
    private Boolean realtime = false;
    
    /**
     * 是否为最终结果
     */
    private Boolean isFinal = false;
} 