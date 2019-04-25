package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Description: AlarmDetail
 * Author: DIYILIU
 * Update: 2018-07-09 09:30
 */

@Data
@Entity
@Table(name = "alarm_policy_detail")
public class AlarmDetail {

    @Id
    private Long id;

    @Column(name = "PolicyId")
    private Long pId;

    @Column(name = "PlcVersionPointId")
    private Long pointId;

    /** 持续时间, 单位: 秒*/
    @Column(name = "Time")
    private Long duration;

    private String expression;
}
