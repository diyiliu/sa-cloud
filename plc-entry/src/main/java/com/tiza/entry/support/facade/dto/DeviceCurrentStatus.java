package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Description: DeviceCurrentStatus
 * Author: DIYILIU
 * Update: 2018-06-14 16:26
 */

@Data
@Entity
@Table(name = "equipment_info")
public class DeviceCurrentStatus {

    @Id
    @Column(name = "equipmentId")
    private Long equipId;

    private Integer dtuStatus;

    private Double totalWorkTime;
}
