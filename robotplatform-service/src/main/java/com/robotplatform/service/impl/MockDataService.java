package com.robotplatform.service.impl;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class MockDataService {
    
    public JSONObject getMockUserInfo() {
        JSONObject userInfo = new JSONObject();
        userInfo.put("name", "张三");
        userInfo.put("idCard", "330102199001011234");
        userInfo.put("phone", "13800138000");
        userInfo.put("address", "浙江省杭州市西湖区");
        return userInfo;
    }

    public JSONObject getMockVehicleInfo() {
        JSONObject vehicleInfo = new JSONObject();
        vehicleInfo.put("vehicles", Arrays.asList(
            createVehicle("浙A12345", "小型汽车", "大众", "帕萨特", "2020-01-01", "2024-12-31"),
            createVehicle("浙A67890", "小型汽车", "丰田", "凯美瑞", "2021-03-15", "2025-03-15"),
            createVehicle("浙B54321", "小型汽车", "本田", "雅阁", "2019-06-20", "2024-06-20")
        ));
        return vehicleInfo;
    }

    private JSONObject createVehicle(String plateNumber, String vehicleType, String brand, 
                                   String model, String registrationDate, String inspectionExpiryDate) {
        JSONObject vehicle = new JSONObject();
        vehicle.put("plateNumber", plateNumber);
        vehicle.put("vehicleType", vehicleType);
        vehicle.put("brand", brand);
        vehicle.put("model", model);
        vehicle.put("registrationDate", registrationDate);
        vehicle.put("inspectionExpiryDate", inspectionExpiryDate);
        return vehicle;
    }

    public JSONObject getMockDriverLicenseInfo() {
        JSONObject licenseInfo = new JSONObject();
        licenseInfo.put("licenseNumber", "330102199001011234");
        licenseInfo.put("name", "张三");
        licenseInfo.put("licenseType", "C1");
        licenseInfo.put("issueDate", "2018-01-01");
        licenseInfo.put("expiryDate", "2024-01-01");
        licenseInfo.put("status", "正常");
        return licenseInfo;
    }

    public JSONObject getMockViolationInfo() {
        JSONObject violationInfo = new JSONObject();
        violationInfo.put("totalViolations", 3);
        violationInfo.put("unpaidViolations", 2);
        violationInfo.put("violationList", Arrays.asList(
            createViolation("浙A12345", "2024-01-15", "超速行驶", "杭州市西湖区", "200元", false),
            createViolation("浙A67890", "2024-02-01", "闯红灯", "杭州市上城区", "150元", true),
            createViolation("浙B54321", "2024-02-15", "违停", "杭州市滨江区", "100元", false)
        ));
        return violationInfo;
    }

    private JSONObject createViolation(String plateNumber, String date, String type, 
                                     String location, String fine, boolean paid) {
        JSONObject violation = new JSONObject();
        violation.put("plateNumber", plateNumber);
        violation.put("date", date);
        violation.put("type", type);
        violation.put("location", location);
        violation.put("fine", fine);
        violation.put("paid", paid);
        return violation;
    }
} 