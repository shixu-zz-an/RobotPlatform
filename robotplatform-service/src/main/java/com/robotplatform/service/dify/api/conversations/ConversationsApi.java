package com.robotplatform.service.dify.api.conversations;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 会话列表API客户端
 */
@Component
public class ConversationsApi {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

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
     * 获取会话列表
     *
     * @param request 会话列表请求参数
     * @return 会话列表响应对象
     * @throws IOException 如果在API调用期间发生I/O错误
     */
    public ConversationsResponse getConversations(ConversationsRequest request) throws IOException {
        // 构建URL和查询参数
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + "/conversations").newBuilder()
                .addQueryParameter("user", request.getUser())
                .addQueryParameter("limit", String.valueOf(request.getLimit()));
        
        // 如果有lastId，添加到查询参数中
        if (request.getLastId() != null && !request.getLastId().isEmpty()) {
            urlBuilder.addQueryParameter("last_id", request.getLastId());
        }
        
        // 构建请求
        Request httpRequest = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();
        
        // 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取会话列表失败: " + response.code() + ", 消息: " + 
                        (response.body() != null ? response.body().string() : "无响应体"));
            }
            
            // 解析响应
            if (response.body() != null) {
                return com.alibaba.fastjson2.JSON.parseObject(response.body().string(), ConversationsResponse.class);
            } else {
                throw new IOException("空响应体");
            }
        }
    }


}
