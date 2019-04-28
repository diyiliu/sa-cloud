package com.tiza.entry.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * 设备在线检测
 * Description: KeepAliveTask
 * Author: DIYILIU
 * Update: 2019-04-28 10:07
 */

@Slf4j
@Service
public class KeepAliveTask implements ITask {
    private final static int INTERVAL_TIME = 3; // 分钟

    @Resource
    private ICache onlineCacheProvider;

    @Resource
    private SenderTask senderTask;

    @Scheduled(fixedDelay = 10 * 1000)
    public void execute() {
        Set<Object> keys = onlineCacheProvider.getKeys();
        for (Iterator iter = keys.iterator(); iter.hasNext(); ) {

            String device = (String) iter.next();
            long time = (long) onlineCacheProvider.get(device);

            long now = System.currentTimeMillis();
            if (now - time > INTERVAL_TIME * 60 * 1000) {
                log.info("终端离线[{}], 检测时间[{}]", device, DateUtil.dateToString(new Date(now)));
                onlineCacheProvider.remove(device);
                senderTask.offline(device);
            }
        }
    }
}
