package com.robotplatform.service.impl;

import com.robotplatform.client.model.RobotInfo;
import com.robotplatform.service.RobotService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 机器人服务实现类
 */
@Service
public class RobotServiceImpl implements RobotService {

    // 模拟数据库
    private static final Map<String, RobotInfo> ROBOT_DB = new HashMap<>();

    static {
        // 初始化一些示例数据
        ROBOT_DB.put("1", RobotInfo.builder()
                .robotId("1")
                .robotName("Robot-01")
                .robotType("服务型")
                .status("在线")
                .build());
        
        ROBOT_DB.put("2", RobotInfo.builder()
                .robotId("2")
                .robotName("Robot-02")
                .robotType("工业型")
                .status("离线")
                .build());
    }

    @Override
    public RobotInfo getRobotInfo(String robotId) {
        return ROBOT_DB.get(robotId);
    }

    @Override
    public List<RobotInfo> getAllRobots() {
        return new ArrayList<>(ROBOT_DB.values());
    }

    @Override
    public boolean createRobot(RobotInfo robotInfo) {
        if (robotInfo == null || robotInfo.getRobotId() == null) {
            return false;
        }
        ROBOT_DB.put(robotInfo.getRobotId(), robotInfo);
        return true;
    }

    @Override
    public boolean updateRobot(RobotInfo robotInfo) {
        if (robotInfo == null || robotInfo.getRobotId() == null) {
            return false;
        }
        if (!ROBOT_DB.containsKey(robotInfo.getRobotId())) {
            return false;
        }
        ROBOT_DB.put(robotInfo.getRobotId(), robotInfo);
        return true;
    }
} 