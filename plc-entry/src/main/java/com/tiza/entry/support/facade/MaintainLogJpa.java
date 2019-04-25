package com.tiza.entry.support.facade;

import com.tiza.entry.support.facade.dto.MaintainLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Description: MaintainLogJpa
 * Author: DIYILIU
 * Update: 2018-06-15 10:44
 */
public interface MaintainLogJpa extends JpaRepository<MaintainLog, Long> {

    List<MaintainLog> findByEquipId(long equipId, Sort sort);
}
