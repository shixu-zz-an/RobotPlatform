package com.robotplatform.service.dify.api.conversations;

import lombok.Data;

/**
 * 会话列表请求对象
 */
@Data
public class ConversationsRequest {
    /**
     * 用户标识
     */
    private String user;

    /**
     * 上一条记录的ID，用于分页
     */
    private String lastId;

    /**
     * 每页记录数，默认20
     */
    private Integer limit = 20;
}
