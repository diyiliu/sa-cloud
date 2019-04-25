package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Description: MaintainLog
 * Author: DIYILIU
 * Update: 2018-06-15 10:20
 */

@Data
@Entity
@Table(name = "equipment_maintain_log")
public class MaintainLog {

    @Id
    @GeneratedValue
    private Long id;

    private Long equipId;

    @Column(name = "totalWorkHours")
    private Double workHour;

    @Column(name = "buyMonths")
    private Double passMonth;

    private Date maintainTime;

    @Column(name = "maintainItemId")
    private Long itemId;
}
