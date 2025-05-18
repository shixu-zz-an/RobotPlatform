package com.robotplatform.service.dify;


import com.robotplatform.service.dify.api.appinit.ParametersApi;
import com.robotplatform.service.dify.api.appinit.ParametersReponse;
import com.robotplatform.service.dify.api.chatmessge.ChatMessageApi;
import com.robotplatform.service.dify.api.chatmessge.ChatMessageResponse;
import com.robotplatform.service.dify.api.conversations.*;

import com.robotplatform.service.dify.api.upload.FileUploadApi;
import com.robotplatform.service.dify.api.upload.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.Resource;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify API管理器
 * 负责处理与Dify API的所有交互，包括对话初始化、消息发送和历史记录获取
 */

@Component
public class DifyApiManager {
    private final Map<String, String> scenarioApiKeys = new HashMap<>();

    @Resource
    private ParametersApi parametersApi;

    @Resource
    private ChatMessageApi chatMessageApi;

    @Resource
    private ConversationsApi conversationsApi;
    
    @Resource
    private FileUploadApi fileUploadApi;
    
    @Resource
    private ConversationsDetailApi conversationsDetailApi;

    /**
     * 注册场景特定的API密钥
     *
     * @param scenario 场景标识
     * @param apiKey API密钥
     */
    public void registerScenarioApiKey(String scenario, String apiKey) {
        scenarioApiKeys.put(scenario, apiKey);
    }

    /**
     * 初始化对话参数
     *
     * @param scenario 场景标识
     * @return 参数配置响应
     * @throws IOException 如果API调用失败
     */
    public ParametersReponse initializeParameters(String scenario) throws IOException {
        return parametersApi.getParameters();
    }

    /**
     * 发送对话消息并返回诊断结果
     *
     * @param scenario 场景标识
     * @param query 用户查询
     * @param userId 用户ID
     * @param conversationId 会话ID（可选）
     * @return 诊断结果DTO
     * @throws IOException 如果API调用失败
     */
    public ChatMessageResponse sendMessage(String scenario, String query, String userId, String conversationId) throws IOException {
        ChatMessageResponse response;
        if (conversationId == null || conversationId.isEmpty()) {
            response = chatMessageApi.startConversation(query, userId);
        } else {
            response = chatMessageApi.continueConversation(query, conversationId, userId);
        }
        
        return response;
    }



    /**
     * 发送对话消息并返回诊断结果
     *
     * @param scenario 场景标识
     * @param query 用户查询
     * @param userId 用户ID
     * @param conversationId 会话ID（可选）
     * @return 诊断结果DTO
     * @throws IOException 如果API调用失败
     */
    public ChatMessageResponse sendMessageByChatMessageResponse(String scenario, String query, String userId, String conversationId) throws IOException {
        ChatMessageResponse response;
        if (conversationId == null || conversationId.isEmpty()) {
            response = chatMessageApi.startConversation(query, userId);
        } else {
            response = chatMessageApi.continueConversation(query, conversationId, userId);
        }

        // 将ChatMessageResponse转换为DiagnosisDTO
        return response;
    }

    /**
     * 获取对话历史记录
     *
     * @param scenario 场景标识
     * @param userId 用户ID
     * @param lastId 上一条记录ID（可选）
     * @param limit 每页记录数
     * @return 会话列表响应
     * @throws IOException 如果API调用失败
     */
    public ConversationsResponse getConversationHistory(String scenario, String userId, String lastId, int limit) throws IOException {
        ConversationsRequest request = new ConversationsRequest();
        request.setUser(userId);
        request.setLastId(lastId);
        request.setLimit(limit);
        
        return conversationsApi.getConversations(request);
    }
    
    /**
     * 上传文件到Dify API
     *
     * @param file 要上传的文件
     * @param userId 用户ID
     * @return 文件上传响应
     * @throws IOException 如果API调用失败
     */
    public FileUploadResponse uploadFile(MultipartFile file, String userId) throws IOException {
        return fileUploadApi.uploadFile(file, userId);
    }
    
    /**
     * 获取会话详细消息记录
     *
     * @param scenario 场景标识
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param firstId 当前页第一条聊天记录的ID（可选）
     * @param limit 每页记录数
     * @return 会话详细消息响应
     * @throws IOException 如果API调用失败
     */
    public ConversationMessagesResponse getConversationDetails(String scenario, String conversationId, String userId,
                                                               String firstId, Integer limit) throws IOException {
        return conversationsDetailApi.getConversationMessages(conversationId, userId, firstId, limit);
    }
    
    /**
     * 获取会话详细消息记录（使用默认参数）
     *
     * @param scenario 场景标识
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @return 会话详细消息响应
     * @throws IOException 如果API调用失败
     */
    public ConversationMessagesResponse getConversationDetails(String scenario, String conversationId, String userId) throws IOException {
        return conversationsDetailApi.getConversationMessages(conversationId, userId);
    }

}
