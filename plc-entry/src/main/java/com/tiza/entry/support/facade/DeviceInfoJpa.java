package com.tiza.entry.support.facade;

import com.tiza.entry.support.facade.dto.DeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: DeviceInfoJpa
 * Author: DIYILIU
 * Update: 2018-04-16 09:51
 */
public interface DeviceInfoJpa extends JpaRepository<DeviceInfo, Long> {

    DeviceInfo findById(long id);
}
