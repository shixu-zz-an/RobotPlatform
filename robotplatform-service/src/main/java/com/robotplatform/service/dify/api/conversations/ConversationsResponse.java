package com.robotplatform.service.dify.api.conversations;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 会话列表响应对象
 */
@Data
public class ConversationsResponse {
    /**
     * 每页记录数
     */
    private Integer limit;

    /**
     * 是否还有更多记录
     */
    @JsonProperty("has_more")
    private Boolean hasMore;

    /**
     * 会话列表数据
     */
    private List<Conversation> data;

    /**
     * 会话信息
     */
    @Data
    public static class Conversation {
        /**
         * 会话ID
         */
        private String id;

        /**
         * 会话名称
         */
        private String name;

        /**
         * 会话输入参数
         */
        private Map<String, String> inputs;

        /**
         * 会话状态
         */
        private String status;

        /**
         * 创建时间（Unix时间戳）
         */
        @JsonProperty("created_at")
        private Long createdAt;

        /**
         * 更新时间（Unix时间戳）
         */
        @JsonProperty("updated_at")
        private Long updatedAt;
    }
}
