package com.robotplatform.service.impl;

import com.robotplatform.service.LargeLanguageModelService;
import com.robotplatform.service.UserService;
import com.robotplatform.service.external.lm.LargeLanguageModelClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基于LangChain4j的大语言模型服务实现
 */
@Slf4j
@Service
public class OllamaLanguageModelServiceImpl implements LargeLanguageModelService {

    @Value("${robotplatform.ollama.server.url:http://localhost:11434}")
    private String ollamaServerUrl;

    @Value("${robotplatform.ollama.model:qwen2.5:14b}")
    private String modelName;

    @Value("${robotplatform.ollama.timeout.seconds:60}")
    private int timeoutSeconds;

    private LargeLanguageModelClient llmClient;
    private ExecutorService executorService;

    // 存储会话上下文
    private Map<String, StringBuilder> sessionContextMap = new ConcurrentHashMap<>();

    private LargeLanguageModelClient.ClientConfig config;

    @PostConstruct
    public void init() {
        try {
            // 创建LargeLanguageModelClient实例
            config = LargeLanguageModelClient.ClientConfig.builder()
                    .baseUrl(ollamaServerUrl)
                    .modelName(modelName)
                    .timeoutSeconds(timeoutSeconds)
                    .build();

            llmClient = new LargeLanguageModelClient(config);

            executorService = Executors.newFixedThreadPool(5);
            log.info("LLM服务初始化完成, 服务器地址: {}, 模型: {}", ollamaServerUrl, modelName);
        } catch (Exception e) {
            log.error("LLM服务初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("LangChain4j LLM服务已关闭");
    }

    @Override
    public String chat(String systemPrompt, String prompt) {
        try {
            // log.info("调用LLM客户端进行对话, 提示词长度: {}", prompt.length());

            // 调用LLM客户端获取回复
            String response = llmClient.chat(systemPrompt, prompt);

            // log.info("LLM响应完成, 响应长度: {}", response.length());
            return response;
        } catch (Exception e) {
            log.error("LLM对话失败", e);
            return "抱歉，我现在无法回答您的问题，请稍后再试。";
        }
    }

    @Override
    public CompletableFuture<String> chatAsync(String systemPrompt, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return chat(systemPrompt, prompt);
            } catch (Exception e) {
                log.error("异步LangChain4j对话失败", e);
                return "抱歉，我现在无法回答您的问题，请稍后再试。";
            }
        }, executorService);
    }

    @Override
    public String chatWithContext(String sessionId, String systemPrompt, String prompt) {
        try {

            // 获取或创建会话上下文
            StringBuilder context = sessionContextMap.computeIfAbsent(sessionId, k -> new StringBuilder());

            // 将用户数据和优化后的用户输入添加到上下文
            if (context.length() > 0) {
                context.append("\n\n用户: ").append(prompt);
            } else {
                context.append("\n\n用户: ").append(prompt);
            }
            // 限制上下文大小，防止提示词过长
            limitContextSize(context);



            // 调用LLM客户端获取回复
            String response = llmClient.chat(config.SYSTEM_PROMPT, context.toString());

            // 将回复添加到上下文
            context.append("\n\n助手: ").append(response);

            log.info("LLM响应完成, 会话ID: {}, 提示词: {}, 响应: {}", sessionId,context, response);
            return response;
        } catch (Exception e) {
            log.error("LLM对话失败, 会话ID: {}", sessionId, e);
            return "抱歉，我现在无法回答您的问题，请稍后再试。";
        }
    }


    @Override
    public CompletableFuture<String> chatWithContextAsync(String sessionId, String prompt) {
        return this.chatWithContextAsync(sessionId, config.SYSTEM_PROMPT, prompt);
    }

    @Override
    public CompletableFuture<String> chatWithContextAsync(String sessionId, String systemPrompt, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return chatWithContext(sessionId, systemPrompt, prompt);
            } catch (Exception e) {
                log.error("异步LLM对话失败, 会话ID: {}", sessionId, e);
                return "抱歉，我现在无法回答您的问题，请稍后再试。";
            }
        }, executorService);
    }

    @Override
    public void clearContext(String sessionId) {
        sessionContextMap.remove(sessionId);
        log.info("清除会话上下文, 会话ID: {}", sessionId);
    }

    /**
     * 限制上下文大小，防止提示词过长
     * 当上下文超过一定大小时，保留最新的部分
     */
    private void limitContextSize(StringBuilder context) {
        final int MAX_CONTEXT_LENGTH = 4000; // 根据实际模型调整

        if (context.length() > MAX_CONTEXT_LENGTH) {
            // 保留最后部分，从最近的用户或助手开始的位置
            int cutIndex = context.lastIndexOf("用户:", context.length() - MAX_CONTEXT_LENGTH);
            if (cutIndex == -1) {
                cutIndex = context.lastIndexOf("助手:", context.length() - MAX_CONTEXT_LENGTH);
            }

            if (cutIndex > 0) {
                context.delete(0, cutIndex);
            }
        }
    }

    @Override
    public String optimizeUserInput( String input){
        return optimizeUserInput(config.TEXT_OPTIMIZER_PROMPT,input);
    }
    /**
     * 优化用户输入
     */
    @Override
    public String optimizeUserInput(String systemPrompt, String input) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n请优化以下输入：\n");
            builder.append(input);
            // 调用LLM进行优化
            String optimizedText = llmClient.chat(systemPrompt, builder.toString());

            // 如果优化失败，返回原始输入
            if (optimizedText == null || optimizedText.trim().isEmpty()) {
                return input;
            }

            return optimizedText.trim();
        } catch (Exception e) {
            log.error("优化用户输入失败", e);
            return input;
        }
    }
}