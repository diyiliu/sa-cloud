package com.tiza.entry.support.facade;

import com.tiza.entry.support.facade.dto.AlarmInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: AlarmInfoJpa
 * Author: DIYILIU
 * Update: 2018-07-09 10:13
 */
public interface AlarmInfoJpa extends JpaRepository<AlarmInfo, Long> {


    AlarmInfo findById(long id);
}
