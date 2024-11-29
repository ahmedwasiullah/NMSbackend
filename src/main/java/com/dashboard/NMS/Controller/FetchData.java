package com.dashboard.NMS.Controller;

import com.dashboard.NMS.DTO.DeviceData;
import com.dashboard.NMS.DTO.DeviceHistory;
import com.dashboard.NMS.DTO.Logs;
import com.dashboard.NMS.DTO.NetworkData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dashboard.NMS.Repository.DeviceStatusRepository;
import com.dashboard.NMS.Repository.DatabaseMetadataRepository;

import java.util.List;

@RequestMapping("/fetch")
@RestController
public class FetchData {
    @Autowired
    DeviceStatusRepository deviceStatusRepository; // Follow Java naming conventions
    @Autowired
    DatabaseMetadataRepository databaseMetadataRepository; // Follow Java naming conventions

    // Endpoint to fetch table names
    @GetMapping("/Tables")
    public List<String> getTables(){
        return databaseMetadataRepository.getAllTableNames();
    }

    // Endpoint to fetch device status summary by table name
    @GetMapping("/getTableData")
    public NetworkData getTableData(@RequestParam String tableName){
        return deviceStatusRepository.getDeviceStatusSummary(tableName);
    }

    // Endpoint to fetch logs for a specific table (no time range specified)
    @GetMapping("/getLogs")
    public List<Logs> getTodaysLog(@RequestParam String tableName){
        return deviceStatusRepository.getDeviceLogs(tableName);
    }

    // Endpoint to fetch device data by table and device name (no time range)
    @GetMapping("/getDeviceData")
    public DeviceHistory getDeviceData(@RequestParam String tableName, @RequestParam String name){
        return deviceStatusRepository.getDeviceData(tableName, name);
    }

    // Endpoint to fetch specific logs for a device
    @GetMapping("/getDeviceLogsByDevice")
    public List<Logs> getDeviceSpecificLog(@RequestParam String tableName, @RequestParam String name){
        return deviceStatusRepository.getSpecificDeviceLogs(tableName, name);
    }

    // Endpoint to fetch specific logs for a device with a time range (days)
    @GetMapping("/getDeviceLogsByDays")
    public List<Logs> getDeviceSpecificLog(@RequestParam String tableName, @RequestParam String name, @RequestParam int days){
        return deviceStatusRepository.getSpecificDeviceLogs(tableName, name, days);
    }

    // Endpoint to fetch device data with a time range (days)
    @GetMapping("/getDeviceDataByDays")
    public DeviceHistory getDeviceData(@RequestParam String tableName, @RequestParam String name, @RequestParam int days){
        return deviceStatusRepository.getDeviceData(tableName, name, days);
    }
}
