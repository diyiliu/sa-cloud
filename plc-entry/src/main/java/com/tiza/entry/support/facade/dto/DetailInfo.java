package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Description: DetailInfo
 * Author: DIYILIU
 * Update: 2018-04-26 13:43
 */

@Data
@Entity
@IdClass(DetailInfoKey.class)
@Table(name = "equipment_info_detail")
public class DetailInfo {

    @Id
    private Long equipId;

    @Id
    private String tag;

    private String value;

    @Column(name = "PlcVersionPointId")
    private Long pointId;

    private Date lastTime;

    @Transient
    private Long faultId;

    @Transient
    private Integer faultType;
}


@Data
class DetailInfoKey implements Serializable {

    private Long equipId;

    private String tag;
}

