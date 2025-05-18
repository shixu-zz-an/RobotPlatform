package com.robotplatform.service.external.funasr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "robotplatform.funasr")
public class FunasrConfig {

    private String serverUrl;

    private Integer heartbeatInterval;
    // 默认配置参数
    String DEFAULT_MODE = "2pass";
    String[] DEFAULT_CHUNK_SIZE = {"5", "10", "5"};
    int DEFAULT_CHUNK_INTERVAL = 10;
    boolean DEFAULT_ITN = true;
    Map<String, Integer> DEFAULT_HOTWORDS = Map.of("阿里巴巴", 20, "hello world", 40);

}
