package com.tiza.entry.support.facade;

import com.tiza.entry.support.facade.dto.DetailInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Description: DeviceInfoJpa
 * Author: DIYILIU
 * Update: 2018-04-16 09:51
 */
public interface DetailInfoJpa extends JpaRepository<DetailInfo, Long> {

    List<DetailInfo> findByEquipIdAndTagIn(long equipId, String[] tags);
}
