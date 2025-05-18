package com.robotplatform.service;

import com.alibaba.fastjson2.JSONObject;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 获取用户基本信息
     */
    JSONObject getUserInfo(String sessionId);
    
    /**
     * 获取车辆信息
     */
    JSONObject getVehicleInfo(String sessionId);
    
    /**
     * 获取驾驶证信息
     */
    JSONObject getDriverLicenseInfo(String sessionId);
    
    /**
     * 获取违法记录信息
     */
    JSONObject getViolationInfo(String sessionId);
    
    /**
     * 获取用户所有相关信息
     */
    JSONObject getAllUserInfo(String sessionId);
    
    /**
     * 获取可用窗口号
     */
    String getAvailableWindow(String businessType);
    
    /**
     * 判断是否为历史违法提醒场景
     */
    boolean isHistoryViolationReminder(String sessionId);
} 