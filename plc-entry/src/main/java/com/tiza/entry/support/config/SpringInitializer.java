package com.tiza.entry.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.tiza.entry.support.facade.AlarmInfoJpa;
import com.tiza.entry.support.facade.dto.AlarmDetail;
import com.tiza.entry.support.facade.dto.AlarmInfo;
import com.tiza.entry.support.facade.dto.FaultInfo;
import com.tiza.entry.support.model.AlarmGroup;
import com.tiza.entry.support.model.AlarmItem;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: SpringInitializer
 * Author: DIYILIU
 * Update: 2019-04-28 09:43
 */

@Component
public class SpringInitializer implements InitializingBean {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private AlarmInfoJpa alarmInfoJpa;

    @Resource
    private ICache faultCacheProvider;

    @Resource
    private ICache alarmCacheProvider;

    @Override
    public void afterPropertiesSet() {

        synchFault();
    }

    /**
     * 同步故障缓存
     */
    private void synchFault() {
        String sql = "SELECT " +
                " f.*, c.`Level` " +
                "FROM " +
                " equipment_fault f " +
                "LEFT JOIN fault_code c ON c.id = f.FaultId " +
                "WHERE " +
                " f.FaultEndTime IS NULL " +
                "OR f.FaultEndTime < f.FaultStartTime";


        List<FaultInfo> faultInfoList = jdbcTemplate.query(sql, (ResultSet rs, int rowNum) -> {
            FaultInfo faultInfo = new FaultInfo();
            faultInfo.setId(rs.getLong("id"));
            faultInfo.setFaultType(rs.getInt("level"));
            faultInfo.setEquipId(rs.getLong("equipId"));
            faultInfo.setTag(rs.getString("tag"));
            faultInfo.setStartTime(rs.getTimestamp("faultStartTime"));
            faultInfo.setAlarmType(rs.getInt("alarmType"));
            faultInfo.setAlarmPolicyId(rs.getLong("alarmPolicyId"));

            return faultInfo;
        });

        // 故障
        List<FaultInfo> faults = faultInfoList.stream().filter(f -> f.getAlarmType() == 1).collect(Collectors.toList());
        Map<Long, List<FaultInfo>> listMap = faults.stream().collect(Collectors.groupingBy(FaultInfo::getEquipId));
        faultCacheProvider.put(listMap);

        // 自定义报警
        List<FaultInfo> alarms = faultInfoList.stream().filter(f -> f.getAlarmType() == 2).collect(Collectors.toList());
        initAlarm(alarms);
    }

    private void initAlarm(List<FaultInfo> alarms) {
        for (FaultInfo alarm : alarms) {
            long policyId = alarm.getAlarmPolicyId();

            AlarmInfo alarmInfo = alarmInfoJpa.findById(policyId);
            AlarmGroup alarmGroup = new AlarmGroup();
            alarmGroup.setId(alarm.getId());
            alarmGroup.setStartTime(alarm.getStartTime());

            Map<Long, AlarmItem> itemMap = new HashMap();
            List<AlarmDetail> alarmDetails = alarmInfo.getAlarmDetails();
            for (AlarmDetail detail : alarmDetails) {
                AlarmItem item = new AlarmItem();
                item.setId(detail.getId());
                itemMap.put(detail.getId(), item);
            }
            alarmGroup.setItemMap(itemMap);

            String key = alarm.getEquipId() + ":" + alarmInfo.getId();
            alarmCacheProvider.put(key, alarmGroup);
        }
    }
}
