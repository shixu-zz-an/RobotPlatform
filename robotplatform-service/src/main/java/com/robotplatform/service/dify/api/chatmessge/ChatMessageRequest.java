package com.robotplatform.service.dify.api.chatmessge;

import java.util.List;
import java.util.Map;

/**
 * Dify chat-messages API的请求对象
 */
public class ChatMessageRequest {
    private Map<String, Object> inputs;
    private String query;
    private String responseMode;
    private String conversationId;
    private String user;
    private List<FileInfo> files;

    /**
     * 文件上传信息
     */
    public static class FileInfo {
        private String type;
        private String transferMethod;
        private String url;

        /**
         * 获取文件类型
         * @return 文件类型
         */
        public String getType() {
            return type;
        }

        /**
         * 设置文件类型
         * @param type 文件类型
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * 获取文件传输方式
         * @return 文件传输方式
         */
        public String getTransferMethod() {
            return transferMethod;
        }

        /**
         * 设置文件传输方式
         * @param transferMethod 文件传输方式
         */
        public void setTransferMethod(String transferMethod) {
            this.transferMethod = transferMethod;
        }

        /**
         * 获取文件URL
         * @return 文件URL
         */
        public String getUrl() {
            return url;
        }

        /**
         * 设置文件URL
         * @param url 文件URL
         */
        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * 获取输入参数
     * @return 输入参数
     */
    public Map<String, Object> getInputs() {
        return inputs;
    }

    /**
     * 设置输入参数
     * @param inputs 输入参数
     */
    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    /**
     * 获取查询字符串
     * @return 查询字符串
     */
    public String getQuery() {
        return query;
    }

    /**
     * 设置查询字符串
     * @param query 查询字符串
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * 获取响应模式
     * @return 响应模式
     */
    public String getResponseMode() {
        return responseMode;
    }

    /**
     * 设置响应模式
     * @param responseMode 响应模式
     */
    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    /**
     * 获取会话ID
     * @return 会话ID
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * 设置会话ID
     * @param conversationId 会话ID
     */
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * 获取用户信息
     * @return 用户信息
     */
    public String getUser() {
        return user;
    }

    /**
     * 设置用户信息
     * @param user 用户信息
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * 获取文件列表
     * @return 文件列表
     */
    public List<FileInfo> getFiles() {
        return files;
    }

    /**
     * 设置文件列表
     * @param files 文件列表
     */
    public void setFiles(List<FileInfo> files) {
        this.files = files;
    }
}
