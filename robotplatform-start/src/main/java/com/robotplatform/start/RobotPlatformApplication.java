package com.robotplatform.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RobotPlatform应用程序启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.robotplatform"})
@EnableScheduling
public class RobotPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RobotPlatformApplication.class, args);
    }
} 