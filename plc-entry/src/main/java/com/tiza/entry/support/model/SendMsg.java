package com.tiza.entry.support.model;

import lombok.Data;

import java.util.List;

/**
 * Description: SendMsg
 * Author: DIYILIU
 * Update: 2018-02-09 14:54
 */

@Data
public class SendMsg {
    private String deviceId;

    private Integer cmd;

    private byte[] bytes;

    private boolean isFirst = false;

    /** 0: 查询; 1: 设置 */
    private Integer type;

    /** 处理结果 0: 下发; 1: 接收 */
    private Integer result = 0;

    /** 指令KEY */
    private String key;

    /** 下发时间 */
    private Long dateTime;

    /** 下发功能集节点 */
    private List<PointUnit> unitList;

    /** 参数设置Tag */
    private String[] tags;

    /** 数据库指令下发id */
    private Long rowId;
}
