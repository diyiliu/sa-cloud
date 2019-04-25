package com.tiza.entry.support.facade.dto;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Description: AlarmInfo
 * Author: DIYILIU
 * Update: 2018-07-09 09:30
 */

@Data
@Entity
@Table(name = "alarm_policy")
public class AlarmInfo {

    @Id
    private Long id;

    private String name;

    /**
     * (1:启用;2:关闭)
     */
    private Integer status;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "PolicyId")
    private List<AlarmDetail> alarmDetails;

    private Long faultId;

    @Transient
    private List<Long> pointIds;

    public List<Long> getPointIds() {
        if (CollectionUtils.isEmpty(pointIds)) {
            pointIds = alarmDetails.stream().map(AlarmDetail::getPointId).collect(Collectors.toList());
            return pointIds;
        }

        return pointIds;
    }
}
