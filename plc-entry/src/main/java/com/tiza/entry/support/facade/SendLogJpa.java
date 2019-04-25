package com.tiza.entry.support.facade;

import com.tiza.entry.support.facade.dto.SendLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: SendLogJpa
 * Author: DIYILIU
 * Update: 2018-04-16 09:51
 */
public interface SendLogJpa extends JpaRepository<SendLog, Long> {

    SendLog findById(long id);
}
