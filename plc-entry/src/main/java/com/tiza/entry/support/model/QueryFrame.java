package com.tiza.entry.support.model;

import lombok.Data;

import java.util.List;

/**
 * Description: QueryFrame
 * Author: DIYILIU
 * Update: 2018-01-30 09:10
 */

@Data
public class QueryFrame {

    /** 从站地址 */
    private int site;

    /** 功能码 */
    private int code;

    /** 起始地址 */
    private int start;

    /** 字(word)数 */
    private int count;

    /** 功能集中的点 */
    private List<PointUnit> pointUnits;

    /** 类型(1:bit;2:byte;3:word;4:dword;5:digital)*/
    private int type;

    /** 查询频率*/
    private long frequency;

    /**
     * 下发标识
     *
     * @return
     */
    public String getKey(){

        return site + ":" + code + ":" + start + ":" + count;
    }
}
