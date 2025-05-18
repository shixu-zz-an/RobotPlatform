package com.robotplatform.service.dify.api.chatmessge;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Dify chat-messages API的响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String event;
    
    @JSONField(name = "task_id")
    private String taskId;
    
    private String id;
    
    @JSONField(name = "message_id")
    private String messageId;
    
    @JSONField(name = "conversation_id")
    private String conversationId;
    
    private String mode;
    private String answer;
    private Metadata metadata;
    
    @JSONField(name = "created_at")
    private long createdAt;

    /**
     * 包含使用统计和检索资源的元数据信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Usage usage;
        private List<RetrieverResource> retrieverResources;

        /**
         * 获取使用统计信息
         * @return 使用统计信息
         */
        public Usage getUsage() {
            return usage;
        }

        /**
         * 设置使用统计信息
         * @param usage 使用统计信息
         */
        public void setUsage(Usage usage) {
            this.usage = usage;
        }

        /**
         * 获取检索资源列表
         * @return 检索资源列表
         */
        public List<RetrieverResource> getRetrieverResources() {
            return retrieverResources;
        }

        /**
         * 设置检索资源列表
         * @param retrieverResources 检索资源列表
         */
        public void setRetrieverResources(List<RetrieverResource> retrieverResources) {
            this.retrieverResources = retrieverResources;
        }
    }

    /**
     * 令牌和定价的使用统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage implements Serializable {
        private static final long serialVersionUID = 1L;
        
        @JSONField(name = "prompt_tokens")
        private int promptTokens;
        
        @JSONField(name = "prompt_unit_price")
        private String promptUnitPrice;
        
        @JSONField(name = "prompt_price_unit")
        private String promptPriceUnit;
        
        @JSONField(name = "prompt_price")
        private String promptPrice;
        
        @JSONField(name = "completion_tokens")
        private int completionTokens;
        
        @JSONField(name = "completion_unit_price")
        private String completionUnitPrice;
        
        @JSONField(name = "completion_price_unit")
        private String completionPriceUnit;
        
        @JSONField(name = "completion_price")
        private String completionPrice;
        
        @JSONField(name = "total_tokens")
        private int totalTokens;
        
        @JSONField(name = "total_price")
        private String totalPrice;
        
        private String currency;
        private double latency;

        /**
         * 获取提示令牌数
         * @return 提示令牌数
         */
        public int getPromptTokens() {
            return promptTokens;
        }

        /**
         * 设置提示令牌数
         * @param promptTokens 提示令牌数
         */
        public void setPromptTokens(int promptTokens) {
            this.promptTokens = promptTokens;
        }

        /**
         * 获取提示单位价格
         * @return 提示单位价格
         */
        public String getPromptUnitPrice() {
            return promptUnitPrice;
        }

        /**
         * 设置提示单位价格
         * @param promptUnitPrice 提示单位价格
         */
        public void setPromptUnitPrice(String promptUnitPrice) {
            this.promptUnitPrice = promptUnitPrice;
        }

        /**
         * 获取提示价格单位
         * @return 提示价格单位
         */
        public String getPromptPriceUnit() {
            return promptPriceUnit;
        }

        /**
         * 设置提示价格单位
         * @param promptPriceUnit 提示价格单位
         */
        public void setPromptPriceUnit(String promptPriceUnit) {
            this.promptPriceUnit = promptPriceUnit;
        }

        /**
         * 获取提示价格
         * @return 提示价格
         */
        public String getPromptPrice() {
            return promptPrice;
        }

        /**
         * 设置提示价格
         * @param promptPrice 提示价格
         */
        public void setPromptPrice(String promptPrice) {
            this.promptPrice = promptPrice;
        }

        /**
         * 获取完成令牌数
         * @return 完成令牌数
         */
        public int getCompletionTokens() {
            return completionTokens;
        }

        /**
         * 设置完成令牌数
         * @param completionTokens 完成令牌数
         */
        public void setCompletionTokens(int completionTokens) {
            this.completionTokens = completionTokens;
        }

        /**
         * 获取完成单位价格
         * @return 完成单位价格
         */
        public String getCompletionUnitPrice() {
            return completionUnitPrice;
        }

        /**
         * 设置完成单位价格
         * @param completionUnitPrice 完成单位价格
         */
        public void setCompletionUnitPrice(String completionUnitPrice) {
            this.completionUnitPrice = completionUnitPrice;
        }

        /**
         * 获取完成价格单位
         * @return 完成价格单位
         */
        public String getCompletionPriceUnit() {
            return completionPriceUnit;
        }

        /**
         * 设置完成价格单位
         * @param completionPriceUnit 完成价格单位
         */
        public void setCompletionPriceUnit(String completionPriceUnit) {
            this.completionPriceUnit = completionPriceUnit;
        }

        /**
         * 获取完成价格
         * @return 完成价格
         */
        public String getCompletionPrice() {
            return completionPrice;
        }

        /**
         * 设置完成价格
         * @param completionPrice 完成价格
         */
        public void setCompletionPrice(String completionPrice) {
            this.completionPrice = completionPrice;
        }

        /**
         * 获取总令牌数
         * @return 总令牌数
         */
        public int getTotalTokens() {
            return totalTokens;
        }

        /**
         * 设置总令牌数
         * @param totalTokens 总令牌数
         */
        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }

        /**
         * 获取总价格
         * @return 总价格
         */
        public String getTotalPrice() {
            return totalPrice;
        }

        /**
         * 设置总价格
         * @param totalPrice 总价格
         */
        public void setTotalPrice(String totalPrice) {
            this.totalPrice = totalPrice;
        }

        /**
         * 获取货币类型
         * @return 货币类型
         */
        public String getCurrency() {
            return currency;
        }

        /**
         * 设置货币类型
         * @param currency 货币类型
         */
        public void setCurrency(String currency) {
            this.currency = currency;
        }

        /**
         * 获取响应延迟时间
         * @return 响应延迟时间
         */
        public double getLatency() {
            return latency;
        }

        /**
         * 设置响应延迟时间
         * @param latency 响应延迟时间
         */
        public void setLatency(double latency) {
            this.latency = latency;
        }
    }

    /**
     * 检索资源的信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieverResource implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int position;
        private String datasetId;
        private String datasetName;
        private String documentId;
        private String documentName;
        private String segmentId;
        private double score;
        private String content;

        /**
         * 获取位置
         * @return 位置
         */
        public int getPosition() {
            return position;
        }

        /**
         * 设置位置
         * @param position 位置
         */
        public void setPosition(int position) {
            this.position = position;
        }

        /**
         * 获取数据集ID
         * @return 数据集ID
         */
        public String getDatasetId() {
            return datasetId;
        }

        /**
         * 设置数据集ID
         * @param datasetId 数据集ID
         */
        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }

        /**
         * 获取数据集名称
         * @return 数据集名称
         */
        public String getDatasetName() {
            return datasetName;
        }

        /**
         * 设置数据集名称
         * @param datasetName 数据集名称
         */
        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }

        /**
         * 获取文档ID
         * @return 文档ID
         */
        public String getDocumentId() {
            return documentId;
        }

        /**
         * 设置文档ID
         * @param documentId 文档ID
         */
        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }

        /**
         * 获取文档名称
         * @return 文档名称
         */
        public String getDocumentName() {
            return documentName;
        }

        /**
         * 设置文档名称
         * @param documentName 文档名称
         */
        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }

        /**
         * 获取片段ID
         * @return 片段ID
         */
        public String getSegmentId() {
            return segmentId;
        }

        /**
         * 设置片段ID
         * @param segmentId 片段ID
         */
        public void setSegmentId(String segmentId) {
            this.segmentId = segmentId;
        }

        /**
         * 获取相关度分数
         * @return 相关度分数
         */
        public double getScore() {
            return score;
        }

        /**
         * 设置相关度分数
         * @param score 相关度分数
         */
        public void setScore(double score) {
            this.score = score;
        }

        /**
         * 获取内容
         * @return 内容
         */
        public String getContent() {
            return content;
        }

        /**
         * 设置内容
         * @param content 内容
         */
        public void setContent(String content) {
            this.content = content;
        }
    }
}
