package com.tiza.entry.support.facade;

import com.tiza.entry.support.facade.dto.FaultInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: FaultInfoJpa
 * Author: DIYILIU
 * Update: 2018-05-09 17:09
 */
public interface FaultInfoJpa extends JpaRepository<FaultInfo, Long> {

    FaultInfo findById(long id);

/*
    @Query("select f from FaultInfo f where endTime is null or endTime < startTime")
    List<FaultInfo> findByEndTimeIsNullOrEndTimeBeforeStartTime();
*/
}
