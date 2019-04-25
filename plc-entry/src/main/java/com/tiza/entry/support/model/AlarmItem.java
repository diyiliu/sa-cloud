package com.tiza.entry.support.model;

import lombok.Data;

/**
 * Description: AlarmItem
 * Author: DIYILIU
 * Update: 2018-07-10 10:46
 */

@Data
public class AlarmItem {

    private Long id;

    // 持续时间
    private Long duration;

    private Long startTime;

    // 0:卫报警, 1: 报警
    private Integer status = 0;
}
