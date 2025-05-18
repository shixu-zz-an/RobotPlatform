package com.robotplatform.service.external.cosyvoice;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CosyVoice配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "robotplatform.cosyvoice")
public class CosyVoiceConfig {

    /**
     * WebSocket服务器配置
     */
    private Server server;
    
    /**
     * 语音配置
     */
    private Voice voice;

    @Data
    public static class Server {
        /**
         * WebSocket服务器主机地址
         */
        private String host;

        /**
         * WebSocket服务器端口
         */
        private int port;
    }

    @Data
    public static class Voice {
        /**
         * 语音ID
         */
        private String id;
    }
    
    /**
     * 模型目录
     */
    private String modelDir = "iic/CosyVoice2-0.5B";
    

    

    
    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;
    
    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;
    
    /**
     * 写入超时时间（毫秒）
     */
    private int writeTimeout = 30000;
    
    /**
     * 重连间隔时间（毫秒）
     */
    private int reconnectInterval = 1000;
    
    /**
     * 最大重连次数
     */
    private int maxReconnectAttempts = 3;
}