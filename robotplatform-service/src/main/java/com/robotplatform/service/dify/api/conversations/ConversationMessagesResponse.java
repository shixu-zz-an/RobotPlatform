package com.robotplatform.service.dify.api.conversations;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * Dify API 会话历史消息响应模型
 */
public class ConversationMessagesResponse {
    
    @JSONField(name = "limit")
    private Integer limit;
    
    @JSONField(name = "has_more")
    private Boolean hasMore;
    
    @JSONField(name = "data")
    private List<MessageData> data;
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Boolean getHasMore() {
        return hasMore;
    }
    
    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    public List<MessageData> getData() {
        return data;
    }
    
    public void setData(List<MessageData> data) {
        this.data = data;
    }
    
    /**
     * 消息数据模型
     */
    public static class MessageData {
        @JSONField(name = "id")
        private String id;
        
        @JSONField(name = "conversation_id")
        private String conversationId;
        
        @JSONField(name = "inputs")
        private Object inputs;
        
        @JSONField(name = "query")
        private String query;
        
        @JSONField(name = "answer")
        private String answer;
        
        @JSONField(name = "message_files")
        private List<MessageFile> messageFiles;
        
        @JSONField(name = "feedback")
        private Feedback feedback;
        
        @JSONField(name = "retriever_resources")
        private List<RetrieverResource> retrieverResources;
        
        @JSONField(name = "created_at")
        private Long createdAt;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getConversationId() {
            return conversationId;
        }
        
        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }
        
        public Object getInputs() {
            return inputs;
        }
        
        public void setInputs(Object inputs) {
            this.inputs = inputs;
        }
        
        public String getQuery() {
            return query;
        }
        
        public void setQuery(String query) {
            this.query = query;
        }
        
        public String getAnswer() {
            return answer;
        }
        
        public void setAnswer(String answer) {
            this.answer = answer;
        }
        
        public List<MessageFile> getMessageFiles() {
            return messageFiles;
        }
        
        public void setMessageFiles(List<MessageFile> messageFiles) {
            this.messageFiles = messageFiles;
        }
        
        public Feedback getFeedback() {
            return feedback;
        }
        
        public void setFeedback(Feedback feedback) {
            this.feedback = feedback;
        }
        
        public List<RetrieverResource> getRetrieverResources() {
            return retrieverResources;
        }
        
        public void setRetrieverResources(List<RetrieverResource> retrieverResources) {
            this.retrieverResources = retrieverResources;
        }
        
        public Long getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(Long createdAt) {
            this.createdAt = createdAt;
        }
    }
    
    /**
     * 消息文件模型
     */
    public static class MessageFile {
        @JSONField(name = "id")
        private String id;
        
        @JSONField(name = "type")
        private String type;
        
        @JSONField(name = "url")
        private String url;
        
        @JSONField(name = "belongs_to")
        private String belongsTo;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getBelongsTo() {
            return belongsTo;
        }
        
        public void setBelongsTo(String belongsTo) {
            this.belongsTo = belongsTo;
        }
    }
    
    /**
     * 反馈信息模型
     */
    public static class Feedback {
        @JSONField(name = "rating")
        private String rating;
        
        public String getRating() {
            return rating;
        }
        
        public void setRating(String rating) {
            this.rating = rating;
        }
    }
    
    /**
     * 引用和归属分段模型
     */
    public static class RetrieverResource {
        @JSONField(name = "position")
        private Integer position;
        
        @JSONField(name = "dataset_id")
        private String datasetId;
        
        @JSONField(name = "dataset_name")
        private String datasetName;
        
        @JSONField(name = "document_id")
        private String documentId;
        
        @JSONField(name = "document_name")
        private String documentName;
        
        @JSONField(name = "segment_id")
        private String segmentId;
        
        @JSONField(name = "score")
        private Double score;
        
        @JSONField(name = "content")
        private String content;
        
        public Integer getPosition() {
            return position;
        }
        
        public void setPosition(Integer position) {
            this.position = position;
        }
        
        public String getDatasetId() {
            return datasetId;
        }
        
        public void setDatasetId(String datasetId) {
            this.datasetId = datasetId;
        }
        
        public String getDatasetName() {
            return datasetName;
        }
        
        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }
        
        public String getDocumentId() {
            return documentId;
        }
        
        public void setDocumentId(String documentId) {
            this.documentId = documentId;
        }
        
        public String getDocumentName() {
            return documentName;
        }
        
        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }
        
        public String getSegmentId() {
            return segmentId;
        }
        
        public void setSegmentId(String segmentId) {
            this.segmentId = segmentId;
        }
        
        public Double getScore() {
            return score;
        }
        
        public void setScore(Double score) {
            this.score = score;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}
