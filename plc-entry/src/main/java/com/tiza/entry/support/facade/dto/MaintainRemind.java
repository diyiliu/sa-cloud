package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Description: MaintainRemind
 * Author: DIYILIU
 * Update: 2018-06-14 14:52
 */

@Data
@Entity
@Table(name = "equipment_maintain_remind")
public class MaintainRemind {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long equipId;

    private Long policyId;

    private Long policyDetailId;

    private Double workHours;

    private Double buyMonths;

    private Date createTime;

    private Date updateTime;

    private Integer times;

    private Integer status;

    private Double intervalMonth;

    //1:是;0:否
    @Column(name = "isPrior")
    private Integer isMajor = 0;
}
