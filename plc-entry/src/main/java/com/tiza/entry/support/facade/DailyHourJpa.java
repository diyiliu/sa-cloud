package com.tiza.entry.support.facade;

import com.tiza.entry.support.facade.dto.DailyHour;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Description: DailyHourJpa
 * Author: DIYILIU
 * Update: 2018-05-08 14:59
 */
public interface DailyHourJpa extends JpaRepository<DailyHour, Long> {


    List<DailyHour> findByEquipId(long equipId, Sort sort);
}
