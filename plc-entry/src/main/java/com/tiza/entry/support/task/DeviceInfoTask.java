package com.tiza.entry.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.entry.support.facade.DeviceInfoJpa;
import com.tiza.entry.support.facade.dto.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Description: DeviceInfoTask
 * Author: DIYILIU
 * Update: 2018-01-30 11:07
 */

@Slf4j
@Service
public class DeviceInfoTask implements ITask {

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private DeviceInfoJpa deviceInfoJpa;


    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 200)
    public void execute() {
        log.info("刷新设备列表 ...");
        List<DeviceInfo> list = deviceInfoJpa.findAll();
        refresh(list, deviceCacheProvider);
    }

    private void refresh(List<DeviceInfo> deviceList, ICache deviceCache) {
        if (deviceList == null || deviceList.size() < 1) {
            log.warn("无设备!");
            return;
        }

        Set oldKeys = deviceCache.getKeys();
        Set tempKeys = new HashSet(deviceList.size());

        for (DeviceInfo device : deviceList) {
            String dtuId = device.getDtuId();
            deviceCache.put(dtuId, device);
            tempKeys.add(dtuId);
        }

        Collection subKeys = CollectionUtils.subtract(oldKeys, tempKeys);
        for (Iterator iterator = subKeys.iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            deviceCache.remove(key);
        }
    }
}
