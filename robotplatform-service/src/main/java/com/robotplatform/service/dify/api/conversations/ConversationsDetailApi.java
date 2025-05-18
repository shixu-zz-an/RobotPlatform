package com.robotplatform.service.dify.api.conversations;

import com.alibaba.fastjson2.JSON;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 使用OkHttp实现的Dify会话历史消息获取接口客户端
 */
@Component
public class ConversationsDetailApi {

    @Value("${robotplatform.dify.api.base-url}")
    private String baseUrl;

    @Value("${robotplatform.dify.api.default-key}")
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * 获取会话历史消息
     * 
     * @param conversationId 会话ID
     * @param userId 用户标识，由开发者定义规则，需保证用户标识在应用内唯一
     * @param firstId 当前页第一条聊天记录的ID，默认null
     * @param limit 一次请求返回多少条聊天记录，默认20条
     * @return 会话历史消息响应
     * @throws IOException 如果请求过程中发生IO错误
     */
    public ConversationMessagesResponse getConversationMessages(String conversationId, String userId, 
                                                              String firstId, Integer limit) throws IOException {
        // 构建URL，添加查询参数
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/messages").newBuilder()
                .addQueryParameter("conversation_id", conversationId)
                .addQueryParameter("user", userId);
        
        // 添加可选参数
        if (firstId != null && !firstId.isEmpty()) {
            urlBuilder.addQueryParameter("first_id", firstId);
        }
        
        if (limit != null) {
            urlBuilder.addQueryParameter("limit", String.valueOf(limit));
        }
        
        String url = urlBuilder.build().toString();
        
        // 构建请求
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();
        
        // 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("意外的响应代码: " + response.code() + ", 消息: " +
                        (response.body() != null ? response.body().string() : "无响应体"));
            }
            
            // 解析响应
            if (response.body() != null) {
                return JSON.parseObject(response.body().string(), ConversationMessagesResponse.class);
            } else {
                throw new IOException("空响应体");
            }
        }
    }
    
    /**
     * 获取会话历史消息（使用默认参数）
     * 
     * @param conversationId 会话ID
     * @param userId 用户标识
     * @return 会话历史消息响应
     * @throws IOException 如果请求过程中发生IO错误
     */
    public ConversationMessagesResponse getConversationMessages(String conversationId, String userId) throws IOException {
        return getConversationMessages(conversationId, userId, null, null);
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
}
