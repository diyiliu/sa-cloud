package com.tiza.entry.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.air.cluster.HBaseUtil;
import com.tiza.entry.support.facade.DailyHourJpa;
import com.tiza.entry.support.facade.dto.DailyHour;
import com.tiza.entry.support.facade.dto.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 当日运行时长
 * <p>
 * Description: DailyRunTask
 * Author: DIYILIU
 * Update: 2019-04-28 14:54
 */

@Slf4j
@Service
public class DailyRunTask implements ITask {

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private DailyHourJpa dailyHourJpa;

    @Resource
    private HBaseUtil hbaseUtil;

    @Scheduled(cron = "${cron.daily-run}")
    public void execute() {
        log.info("工作时长统计 ... ");

        dealDaily(new Date());
    }

    public void dealDaily(Date date) {
        DateTime dt = new DateTime(date).withMillisOfDay(0);
        long endTime = dt.getMillis();
        dt = dt.minusDays(1);
        long startTime = dt.getMillis();

        final String tag = "TotalRunTime";
        Set set = deviceCacheProvider.getKeys();
        set.forEach(key -> {
            DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(key);
            Long id = deviceInfo.getId();

            double last = 0;
            List<DailyHour> lastHours = dailyHourJpa.findByEquipId(id, Sort.by(Sort.Direction.DESC, new String[]{"day", "totalHour"}));
            if (CollectionUtils.isNotEmpty(lastHours)) {
                last = lastHours.get(0).getTotalHour();
            }

            try {
                List<String> values = hbaseUtil.scan(id.intValue(), tag, startTime, endTime);
                // 当日最大
                double max;
                if (CollectionUtils.isEmpty(values)) {
                    max = last;
                } else {
                    max = Double.valueOf(values.get(values.size() - 1));
                }

                // 当日工作时间
                double hour = 0;
                if (last > 0 && max > last) {
                    hour = max - last;
                }

                DailyHour dailyHour = new DailyHour();
                dailyHour.setEquipId(id);
                dailyHour.setDay(new Date(startTime));
                dailyHour.setCreateTime(new Date());
                dailyHour.setHour(hour);
                dailyHour.setTotalHour(max);

                dailyHourJpa.save(dailyHour);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
