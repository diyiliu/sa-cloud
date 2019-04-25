package com.tiza.entry.support.model;

import lombok.Data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: AlarmGroup
 * Author: DIYILIU
 * Update: 2018-07-10 14:19
 */

@Data
public class AlarmGroup {

    // 数据库ID
    private Long id;

    private Date startTime;

    private Date endTime;

    private Map<Long, AlarmItem> itemMap = new HashMap();
}
