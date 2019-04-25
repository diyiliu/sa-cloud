package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;


/**
 * Description: DeviceInfo
 * Author: DIYILIU
 * Update: 2018-01-30 11:13
 */

@Data
@Entity
@Table(name = "equipment")
public class DeviceInfo {

    @Id
    private Long id;

    private String dtuId;

    private String equipmentCode;

    /** 出厂日期 */
    private Date factoryDate;

    /** 现场调试时间 */
    private Date debugTime;

    @Column(name = "PlcVersionId")
    private String softVersion;

    private Long maintainPolicyId;

    @Transient
    private Double workHours;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "equipment_alarm_policy_rel",
            joinColumns = @JoinColumn(name = "EquipId", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "PolicyId", referencedColumnName = "id"))
    private List<AlarmInfo> alarmInfoList;
}
