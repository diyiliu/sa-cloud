package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Description: FaultInfo
 * Author: DIYILIU
 * Update: 2018-05-09 17:04
 */

@Data
@Entity
@Table(name = "equipment_fault")
public class FaultInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long faultId;

    private Long equipId;

    @Column(name = "PlcVersionPointId")
    private Long pointId;

    private String tag;

    private String value;

    @Column(name = "FaultStartTime")
    private Date startTime;

    @Column(name = "FaultEndTime")
    private Date endTime;

    @Transient
    private Integer faultType;

    // 1:故障报警;2:自定义报警
    private Integer alarmType;

    private Long alarmPolicyId;
}
