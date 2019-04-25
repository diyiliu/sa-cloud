package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Description: PointInfo
 * Author: DIYILIU
 * Update: 2018-04-23 09:33
 */

@Data
@Entity
@Table(name = "plc_version_point")
public class PointInfo {

    @Id
    private Long id;

    /** plc版本号*/
    @Column(name = "PLCVersionId")
    private String versionId;

    /** 唯一标识*/
    private String tag;

    /** 名称*/
    private String name;

    /** 数据库关联字段*/
    private String field;

    /** 类型(1:bit;2:byte;3:word;4:dword;5:digital)*/
    private Integer pointType;

    /** 从站地址*/
    private Integer siteId;

    /** 起始地址*/
    private Integer address;

    /** bit在byte中的位置[0, 7]*/
    private Integer position;

    /** 查询频率*/
    private Long frequency;

    /** 保存数据类型*/
    private Integer saveType;

    /** 数据格式(1:float;2:int;3:hex)*/
    private Integer dataType;

    /** 硬件故障类型(0:不是故障;1:紧急报警;2:重要报警;3:一般警示,默认不是故障)*/
    private Integer faultType;

    /** 故障Id,关联故障表*/
    private Long faultId;

    /** 1:只读;2:只写;3:读写*/
    private Integer readWrite;

    /** 读功能集*/
    private Integer readFunction;

    /** 写功能集*/
    private Integer writeFunction;
}
