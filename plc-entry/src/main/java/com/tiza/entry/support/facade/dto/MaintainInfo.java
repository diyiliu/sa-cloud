package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.*;

/**
 * Description: MaintainInfo
 * Author: DIYILIU
 * Update: 2018-06-14 09:05
 */

@Data
@Entity
@Table(name = "maintain_policy_detail")
public class MaintainInfo {

    @Id
    @GeneratedValue
    private Long id;

    private Long policyId;

    private Long itemId;

    private Integer isPeriod;

    // 1:是;0:否
    @Column(name = "isPrior")
    private Integer isMajor;

    private Double workHoursBegin;

    private Double buyMonthsBegin;

    @Column(name = "delayHoursEnd")
    private Double workHoursEnd;

    @Column(name = "delayMonthsEnd")
    private Double buyMonthsEnd;
}
