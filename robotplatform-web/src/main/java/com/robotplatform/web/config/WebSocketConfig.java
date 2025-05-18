package com.robotplatform.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import com.robotplatform.web.websocket.VoiceDialogWebSocketHandler;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceDialogWebSocketHandler(), "/ws/voice/{sessionId}")
                .setAllowedOrigins("*"); // 允许所有来源，生产环境应当限制
    }
    
    @Bean
    public VoiceDialogWebSocketHandler voiceDialogWebSocketHandler() {
        return new VoiceDialogWebSocketHandler();
    }
} 