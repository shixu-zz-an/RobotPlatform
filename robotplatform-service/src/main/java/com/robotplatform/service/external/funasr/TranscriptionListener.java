package com.robotplatform.service.external.funasr;

/**
 * 转录结果监听器接口
 */
public interface TranscriptionListener {
    
    /**
     * 处理转录结果
     * 
     * @param result 转录结果
     */
    void onTranscriptionResult(TranscriptionResult result);
} 