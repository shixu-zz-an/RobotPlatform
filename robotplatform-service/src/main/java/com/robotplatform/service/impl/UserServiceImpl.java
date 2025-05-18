package com.robotplatform.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.robotplatform.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private MockDataService mockDataService;

    @Override
    public JSONObject getUserInfo(String sessionId) {
        log.info("获取用户信息, sessionId: {}", sessionId);
        return mockDataService.getMockUserInfo();
    }

    @Override
    public JSONObject getVehicleInfo(String sessionId) {
        log.info("获取车辆信息, sessionId: {}", sessionId);
        return mockDataService.getMockVehicleInfo();
    }

    @Override
    public JSONObject getDriverLicenseInfo(String sessionId) {
        log.info("获取驾驶证信息, sessionId: {}", sessionId);
        return mockDataService.getMockDriverLicenseInfo();
    }

    @Override
    public JSONObject getViolationInfo(String sessionId) {
        log.info("获取违法记录信息, sessionId: {}", sessionId);
        return mockDataService.getMockViolationInfo();
    }

    @Override
    public JSONObject getAllUserInfo(String sessionId) {
        log.info("获取用户所有相关信息, sessionId: {}", sessionId);
        JSONObject allInfo = new JSONObject();
        allInfo.put("userInfo", getUserInfo(sessionId));
        allInfo.put("vehicleInfo", getVehicleInfo(sessionId));
        allInfo.put("driverLicenseInfo", getDriverLicenseInfo(sessionId));
        allInfo.put("violationInfo", getViolationInfo(sessionId));
        return allInfo;
    }

    @Override
    public String getAvailableWindow(String businessType) {
        return "3";
    }

    @Override
    public boolean isHistoryViolationReminder(String sessionId) {
        return false;
    }
} 