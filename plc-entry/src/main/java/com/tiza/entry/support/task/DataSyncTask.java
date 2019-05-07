package com.tiza.entry.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.DateUtil;
import com.tiza.air.cluster.RedisUtil;
import com.tiza.entry.support.facade.dto.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Description: DataSyncTask
 * Author: DIYILIU
 * Update: 2019-05-07 10:48
 */

@Slf4j
@Service
public class DataSyncTask implements ITask {

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private ICache deviceCacheProvider;

    @Value("${redis.equipment-info-key}")
    private String summaryKey;

    @Value("${redis.equipment-detail-key}")
    private String detailKey;

    @Scheduled(fixedDelay = 1 * 60 * 1000, initialDelay = 30 * 1000)
    public void execute() {
        log.info("定时同步 redis 到数据库 ... ");

        Set set = deviceCacheProvider.getKeys();
        set.forEach(e -> {
            DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(e);
            long id = deviceInfo.getId();

            // 持久化到数据库
            syncSummary(id);
            syncDetail(id);
        });
    }

    /**
     * 更新当前表数据
     *
     * @param id
     */
    public void syncSummary(long id) {
        Map dataMap = redisUtil.hgetAll(summaryKey + id);
        String timeKey = "lastTime";
        if (isValid(dataMap, timeKey)) {
            String timeStr = (String) dataMap.get(timeKey);
            dataMap.replace(timeKey, DateUtil.stringToDate(timeStr));

            List list = new ArrayList();
            StringBuilder sqlBuilder = new StringBuilder("UPDATE equipment_info SET ");
            for (Iterator iterator = dataMap.keySet().iterator(); iterator.hasNext(); ) {
                String key = (String) iterator.next();

                sqlBuilder.append(key).append("=?, ");
                Object val = dataMap.get(key);
                list.add(val);
            }

            // log.info("[更新] 设备[{}]当前信息...", id);
            String sql = sqlBuilder.substring(0, sqlBuilder.length() - 2) + " WHERE equipmentId=" + id;
            jdbcTemplate.update(sql, list.toArray());
        }
    }

    /**
     * 更新详情表数据
     *
     * @param id
     */
    public void syncDetail(long id) {
        Map dataMap = redisUtil.hgetAll(detailKey + id);
        String timeKey = "lastTime";
        if (isValid(dataMap, timeKey)) {
            dataMap.remove(timeKey);

            List list = new ArrayList();
            String sql = "REPLACE INTO equipment_info_detail(EquipId, Tag, Value, LastTime) VALUES(?, ?, ?, ?)";
            for (Iterator iterator = dataMap.keySet().iterator(); iterator.hasNext(); ) {
                String key = (String) iterator.next();
                String value = (String) dataMap.get(key);
                String[] strArr = value.split("\\|");

                value = strArr[0];
                Date date = new Date(Long.parseLong(strArr[1]));
                Object[] args = new Object[]{id, key, value, date};
                list.add(args);
            }

            // log.info("[更新] 设备[{}]详细信息...", id);
            jdbcTemplate.batchUpdate(sql, list);
        }
    }

    private boolean isValid(Map dataMap, String timeKey) {
        if (MapUtils.isNotEmpty(dataMap)) {
            String timeStr = (String) dataMap.get(timeKey);
            Date date = DateUtil.stringToDate(timeStr);
            if (System.currentTimeMillis() - date.getTime() < 5 * 60 * 1000) {

                return true;
            }
        }

        return false;
    }
}
