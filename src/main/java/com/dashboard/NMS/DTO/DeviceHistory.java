package com.dashboard.NMS.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class DeviceHistory {
    Integer NoOfUp;
    Integer NoOfDown;
    Double totalUpTime;
    Double totalDownTime;
}
