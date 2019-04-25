package com.tiza.entry.support.facade.dto;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Description: SendLog
 * Author: DIYILIU
 * Update: 2018-04-27 16:33
 */

@Data
@Entity
@Table(name = "instructions_log")
public class SendLog {

    @Id
    private Long id;

    private Integer result;

    private String sendData;

    private String replyData;
}
