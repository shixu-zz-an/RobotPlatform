package com.robotplatform.web.controller;

import com.robotplatform.client.model.RobotInfo;
import com.robotplatform.service.RobotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 机器人控制器
 */
@RestController
@RequestMapping("/api/robots")
public class RobotController {

    @Autowired
    private RobotService robotService;

    /**
     * 获取指定ID的机器人信息
     */
    @GetMapping("/{robotId}")
    public RobotInfo getRobot(@PathVariable String robotId) {
        return robotService.getRobotInfo(robotId);
    }

    /**
     * 获取所有机器人列表
     */
    @GetMapping
    public List<RobotInfo> getAllRobots() {
        return robotService.getAllRobots();
    }

    /**
     * 创建新机器人
     */
    @PostMapping
    public boolean createRobot(@RequestBody RobotInfo robotInfo) {
        return robotService.createRobot(robotInfo);
    }

    /**
     * 更新机器人信息
     */
    @PutMapping("/{robotId}")
    public boolean updateRobot(@PathVariable String robotId, @RequestBody RobotInfo robotInfo) {
        robotInfo.setRobotId(robotId);
        return robotService.updateRobot(robotInfo);
    }
} 