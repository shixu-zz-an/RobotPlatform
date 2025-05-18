package com.robotplatform.service;

import com.robotplatform.client.model.RobotInfo;

import java.util.List;

/**
 * 机器人服务接口
 */
public interface RobotService {

    /**
     * 获取机器人信息
     * 
     * @param robotId 机器人ID
     * @return 机器人信息
     */
    RobotInfo getRobotInfo(String robotId);

    /**
     * 获取所有机器人信息
     * 
     * @return 机器人信息列表
     */
    List<RobotInfo> getAllRobots();

    /**
     * 创建机器人
     * 
     * @param robotInfo 机器人信息
     * @return 创建结果
     */
    boolean createRobot(RobotInfo robotInfo);

    /**
     * 更新机器人信息
     * 
     * @param robotInfo 机器人信息
     * @return 更新结果
     */
    boolean updateRobot(RobotInfo robotInfo);
} 