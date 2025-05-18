package com.robotplatform.service.dify.api.appinit;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Dify parameters API客户端
 * 用于获取应用的参数配置信息
 */
@Component
public class ParametersApi {
    private static  MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Value("${robotplatform.dify.api.base-url}")
    private  String baseUrl;

    @Value("$robotplatform.dify.api.default-key}")
    private  String apiKey;

    private final OkHttpClient client=new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();


    /**
     * 获取应用参数配置
     *
     * @return 参数响应对象
     * @throws IOException 如果在API调用期间发生I/O错误
     */
    public ParametersReponse getParameters() throws IOException {
        String url = baseUrl + "/parameters";
        
        // 构建请求
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();
        
        // 执行请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("获取参数失败: " + response.code() + ", 消息: " + 
                        (response.body() != null ? response.body().string() : "无响应体"));
            }
            
            // 解析响应
            if (response.body() != null) {
                return com.alibaba.fastjson2.JSON.parseObject(response.body().string(), ParametersReponse.class);
            } else {
                throw new IOException("空响应体");
            }
        }
    }

}
