package com.tiza.entry.support.model;

import lombok.Data;

/**
 * Description: MsgMemory
 * Author: DIYILIU
 * Update: 2018-04-26 08:57
 */

@Data
public class MsgMemory {

    private String deviceId;

    /** 当前正在下发的消息*/
    private SendMsg current;
}
