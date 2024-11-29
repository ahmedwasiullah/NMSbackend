package com.dashboard.NMS.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@AllArgsConstructor
@Data
@NoArgsConstructor
public class Logs {
    String name;
    String ip;
    String Status;
    Date timeStamp;
}
