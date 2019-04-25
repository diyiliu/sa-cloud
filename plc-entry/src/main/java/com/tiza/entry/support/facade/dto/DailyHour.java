package com.tiza.entry.support.facade.dto;

import com.diyiliu.plugin.util.CommonUtil;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Description: DailyHour
 * Author: DIYILIU
 * Update: 2018-05-08 14:44
 */

@Data
@Entity
@Table(name = "equipment_worktime")
public class DailyHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long equipId;

    @Column(name = "StatisticDate")
    private Date day;

    private Date createTime;

    @Column(name = "WorkHours")
    private Double hour;

    @Column(name = "TotalHours")
    private Double totalHour;


    public void setHour(Double hour) {
        this.hour = CommonUtil.keepDecimal(hour, 1);
    }

    public void setTotalHour(Double totalHour) {
        this.totalHour = CommonUtil.keepDecimal(totalHour, 1);
    }
}

