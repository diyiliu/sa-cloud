package com.tiza.entry.support.model;

import com.tiza.entry.support.facade.dto.PointInfo;
import lombok.Data;

/**
 * 点组成的单元: 字(两个字节，2*8 bit)
 * (数字量类型除外)
 * Description: PointUnit
 * Author: DIYILIU
 * Update: 2018-04-20 16:40
 */

@Data
public class PointUnit {
    /** 类型(1:bit;2:byte;3:word;4:dword;5:digital)*/
    private Integer type;

    private String[] tags;

    private PointInfo[] points;

    /** 1:只读;2:只写;3:读写*/
    private Integer readWrite;

    /** 读功能集*/
    private Integer readFunction;

    /** 写功能集*/
    private Integer writeFunction;

    /** 从站地址*/
    private Integer siteId;

    /** 起始地址*/
    private Integer address;

    /** 查询频率*/
    private Long frequency;

    /** 查询单元*/
    private QueryFrame queryFrame;
}
