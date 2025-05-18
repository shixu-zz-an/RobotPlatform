package com.robotplatform.service.dify.api.chatmessge;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * 使用OkHttp实现的Dify chat-messages接口客户端
 */
@Component
public class ChatMessageApi {

    private static  MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Value("${robotplatform.dify.api.base-url}")
    private  String baseUrl;

    @Value("${robotplatform.dify.api.default-key}")
    private  String apiKey;

    private final OkHttpClient client=new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();





    /**
     * 向Dify API发送聊天消息请求
     */
    public ChatMessageResponse sendChatMessage(ChatMessageRequest request) throws IOException {
        String url = baseUrl + "/chat-messages";

        // 将请求对象转换为JSON
        String requestJson = com.alibaba.fastjson2.JSON.toJSONString(request);

        // 构建请求
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, JSON))
                .build();

        // 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("意外的响应代码: " + response.code() + ", 消息: " +
                        (response.body() != null ? response.body().string() : "无响应体"));
            }

            // 解析响应
            if (response.body() != null) {
                return com.alibaba.fastjson2.JSON.parseObject(response.body().string(), ChatMessageResponse.class);
            } else {
                throw new IOException("空响应体");
            }
        }
    }

    /**
     * 开始新对话的辅助方法
     */
    public ChatMessageResponse startConversation(String query, String userId) throws IOException {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setQuery(query);
        request.setInputs(new HashMap<>());
        request.setResponseMode("streaming");
        if (userId != null && !userId.isEmpty()) {
            request.setUser(userId);
        }
        return sendChatMessage(request);
    }

    /**
     * 继续现有对话的辅助方法
     */
    public ChatMessageResponse continueConversation(String query, String conversationId, String userId) throws IOException {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setQuery(query);
        request.setResponseMode("streaming");
        request.setInputs(new HashMap<>());
        request.setConversationId(conversationId);
        if (userId != null && !userId.isEmpty()) {
            request.setUser(userId);
        }
        return sendChatMessage(request);
    }

    /**
     * 创建具有自定义配置的新OkHttpClient
     */
    public static OkHttpClient createClient(int connectTimeoutSeconds, int readTimeoutSeconds, int writeTimeoutSeconds) {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    public static void main(String[] args) {

    }
}
