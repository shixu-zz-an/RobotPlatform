package com.robotplatform.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 机器人信息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 机器人ID
     */
    private String robotId;

    /**
     * 机器人名称
     */
    private String robotName;

    /**
     * 机器人类型
     */
    private String robotType;

    /**
     * 机器人状态
     */
    private String status;
} 