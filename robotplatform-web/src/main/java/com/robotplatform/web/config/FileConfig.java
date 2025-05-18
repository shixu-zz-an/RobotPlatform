package com.robotplatform.web.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件相关配置
 */
@Slf4j
@Configuration
public class FileConfig {

    /**
     * 确保日志目录存在
     */
    @PostConstruct
    public void ensureLogDirectoryExists() {
        try {
            Path logDir = Paths.get("./log");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
                log.info("日志目录已创建: {}", logDir.toAbsolutePath());
            } else {
                log.info("日志目录已存在: {}", logDir.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("创建日志目录失败", e);
        }
    }
} 