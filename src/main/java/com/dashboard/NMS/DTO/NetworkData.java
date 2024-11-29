package com.dashboard.NMS.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
@AllArgsConstructor
@Data
public class NetworkData {
    Integer active_devices;
    Integer inactive_devices;
    Integer no_status_devices;
}
