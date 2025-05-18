package com.robotplatform.service.dify.api.upload;

import com.alibaba.fastjson2.JSON;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 使用OkHttp实现的Dify文件上传接口客户端
 */
@Component
public class FileUploadApi {

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
     * 向Dify API上传文件
     * 
     * @param file 要上传的文件
     * @param userId 用户标识
     * @return 上传响应，包含文件ID和相关信息
     * @throws IOException 如果上传过程中发生IO错误
     */
    public FileUploadResponse uploadFile(File file, String userId) throws IOException {
        String url = baseUrl + "/files/upload";

        // 构建multipart请求体
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/octet-stream")))
                .addFormDataPart("user", userId)
                .build();

        // 构建请求
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        // 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("意外的响应代码: " + response.code() + ", 消息: " +
                        (response.body() != null ? response.body().string() : "无响应体"));
            }

            // 解析响应
            if (response.body() != null) {
                return JSON.parseObject(response.body().string(),FileUploadResponse.class);
            } else {
                throw new IOException("空响应体");
            }
        }
    }

    /**
     * 向Dify API上传MultipartFile
     * 
     * @param multipartFile Spring MultipartFile
     * @param userId 用户标识
     * @return 上传响应，包含文件ID和相关信息
     * @throws IOException 如果上传过程中发生IO错误
     */
    public FileUploadResponse uploadFile(MultipartFile multipartFile, String userId) throws IOException {
        String url = baseUrl + "/files/upload";

        // 构建multipart请求体
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", multipartFile.getOriginalFilename(),
                        RequestBody.create(multipartFile.getBytes(), MediaType.parse(multipartFile.getContentType())))
                .addFormDataPart("user", userId)
                .build();

        // 构建请求
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        // 执行请求
        try (Response response = client.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("意外的响应代码: " + response.code() + ", 消息: " +
                        (response.body() != null ? response.body().string() : "无响应体"));
            }

            // 解析响应
            if (response.body() != null) {
                return JSON.parseObject(response.body().string(), FileUploadResponse.class);
            } else {
                throw new IOException("空响应体");
            }
        }
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
