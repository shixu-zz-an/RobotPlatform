package com.robotplatform.service;

import java.util.concurrent.CompletableFuture;

/**
 * 大型语言模型服务接口
 */
public interface LargeLanguageModelService {

    /**
     * 与大语言模型对话
     *
     * @param prompt 用户输入
     * @return 模型回答
     */
    String chat(String systemPrompt,String prompt);

    /**
     * 异步与大语言模型对话
     *
     * @param prompt 用户输入
     * @return 模型回答的Future
     */
    CompletableFuture<String> chatAsync(String systemPrompt,String prompt);
    
    /**
     * 与大语言模型对话（带会话上下文）
     *
     * @param sessionId 会话ID
     * @param prompt 用户输入
     * @return 模型回答
     */
    String chatWithContext(String sessionId,String systemPrompt, String prompt);

    /**
     * 异步与大语言模型对话（带会话上下文）
     *
     * @param sessionId 会话ID
     * @param prompt 用户输入
     * @return 模型回答的Future
     */
    CompletableFuture<String> chatWithContextAsync(String sessionId, String prompt);

    /**
     * 异步与大语言模型对话（带会话上下文）

     * @param sessionId 会话ID
     * @param systemPrompt 指定系统模版
     * @param prompt 用户输入
     * @return 模型回答的Future
     */
    CompletableFuture<String> chatWithContextAsync(String sessionId,String systemPrompt, String prompt);
    
    /**
     * 清除会话上下文
     *
     * @param sessionId 会话ID
     */
    void clearContext(String sessionId);

    /**
     * 转写用户提示词，根据上下文及用户的输入
     * @param input
     * @return
     */
     String optimizeUserInput(String systemPrompt,String input);

    /**
     * 使用默认系统提示词转写用户提示词，根据上下文及用户的输入
     * @param input
     * @return
     */
    String optimizeUserInput(String input);
} 