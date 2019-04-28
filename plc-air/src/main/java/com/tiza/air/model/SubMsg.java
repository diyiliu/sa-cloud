package com.tiza.air.model;

import lombok.Data;

/**
 * Description: SubMsg
 * Author: DIYILIU
 * Update: 2019-04-24 13:55
 */

@Data
public class SubMsg {

    /** 设备编号 **/
    private String device;

    /** 消息体 **/
    private String data;

    /** 消息标识 **/
    private String key;

    /** 消息发送时间 **/
    private Long time;

    /** 0:查询; 1:设置; **/
    private Integer type;
}
