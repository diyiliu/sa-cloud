package com.tiza.entry.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.entry.support.facade.DeviceCurrentStatusJpa;
import com.tiza.entry.support.facade.MaintainInfoJpa;
import com.tiza.entry.support.facade.MaintainLogJpa;
import com.tiza.entry.support.facade.MaintainRemindJpa;
import com.tiza.entry.support.facade.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: MaintainTask
 * Author: DIYILIU
 * Update: 2018-06-14 09:02
 */

@Slf4j
@Service
public class MaintainTask implements ITask {

    @Resource
    private MaintainInfoJpa maintainInfoJpa;

    @Resource
    private MaintainRemindJpa maintainRemindJpa;

    @Resource
    private MaintainLogJpa maintainLogJpa;

    @Resource
    private DeviceCurrentStatusJpa deviceCurrentStatusJpa;

    @Resource
    private ICache deviceCacheProvider;

    @Scheduled(cron = "0 10 0 * * ?")
    public void execute() {
        log.info("保养提醒分析 ... ");

        Map<String, Object> deviceInfoMap = deviceCacheProvider.get(deviceCacheProvider.getKeys());
        Set<Long> policyIds = deviceInfoMap.keySet().stream().map(e -> {
            DeviceInfo deviceInfo = (DeviceInfo) deviceInfoMap.get(e);
            return deviceInfo.getMaintainPolicyId();
        }).collect(Collectors.toSet());

        List<MaintainInfo> maintainInfoList = maintainInfoJpa.findByPolicyIdIn(policyIds, Sort.by(new String[]{"isPeriod", "isMajor", "workHoursBegin"}));
        Map<Long, List<MaintainInfo>> maintainInfoMap = maintainInfoList.stream().collect(Collectors.groupingBy(MaintainInfo::getPolicyId));

        Set<String> deviceSet = deviceInfoMap.keySet();
        for (Iterator<String> iterator = deviceSet.iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            DeviceInfo deviceInfo = (DeviceInfo) deviceInfoMap.get(key);

            if (deviceInfo.getDebugTime() == null) {
                continue;
            }

            DeviceCurrentStatus currentStatus = deviceCurrentStatusJpa.findByEquipId(deviceInfo.getId());
            deviceInfo.setWorkHours(currentStatus.getTotalWorkTime());

            // 保养记录
            List<MaintainLog> maintainLogs = maintainLogJpa.findByEquipId(deviceInfo.getId(), Sort.by(Sort.Direction.DESC, "maintainTime"));
            MaintainLog lastMtLog = null;
            if (CollectionUtils.isNotEmpty(maintainLogs)) {
                lastMtLog = maintainLogs.get(0);
            }

            Long policyId = deviceInfo.getMaintainPolicyId();
            if (!maintainInfoMap.containsKey(policyId)) {
                continue;
            }

            List<MaintainInfo> maintainInfos = maintainInfoMap.get(policyId);
            List<MaintainInfo> list = maintainInfos.stream().filter(mt -> mt.getIsMajor() == 0).collect(Collectors.toList());
            // 普通保养
            for (MaintainInfo maintainInfo : list) {

                dealMaintain(maintainInfo, lastMtLog, deviceInfo);
            }

            List<MaintainInfo> majorList = maintainInfos.stream().filter(mt -> mt.getIsMajor() == 1).collect(Collectors.toList());
            // 小保养、大保养
            dealMajor(majorList, lastMtLog, deviceInfo);
        }
    }

    private void dealMaintain(MaintainInfo maintainInfo, MaintainLog maintainLog, DeviceInfo deviceInfo) {
        double workHour = deviceInfo.getWorkHours() == null ? 0 : deviceInfo.getWorkHours();
        double month = calcMonth(deviceInfo.getDebugTime(), new Date());

        boolean isPeriod = maintainInfo.getIsPeriod() == 1 ? true : false;
        List<MaintainRemind> reminds = maintainRemindJpa.findByEquipIdAndPolicyDetailId(deviceInfo.getId(), maintainInfo.getId(), Sort.by(Sort.Direction.DESC, new String[]{"CreateTime", "workHours"}));

        MaintainRemind remind = new MaintainRemind();
        remind.setEquipId(deviceInfo.getId());
        remind.setPolicyId(maintainInfo.getPolicyId());
        remind.setPolicyDetailId(maintainInfo.getId());
        remind.setWorkHours(workHour);
        remind.setBuyMonths(month);

        remind.setTimes(reminds.size() + 1);
        remind.setCreateTime(new Date());
        remind.setStatus(1);

        double mtHour = maintainInfo.getWorkHoursBegin();
        // 非周期保养
        if (!isPeriod && CollectionUtils.isEmpty(reminds)) {

            // 保养提醒
            if (workHour > mtHour) {

                maintainRemindJpa.save(remind);
            }
        }
        // 周期保养
        else {
            if (CollectionUtils.isNotEmpty(reminds)) {
                MaintainRemind lastRemind = reminds.get(0);
                if (lastRemind.getStatus() == 1) {

                    return;
                }
            }

            double hourGap = workHour;
            if (maintainLog != null) {
                hourGap = workHour - maintainLog.getWorkHour();
            }

            // 保养提醒
            if (hourGap >= mtHour) {
                if (CollectionUtils.isNotEmpty(reminds)) {
                    MaintainRemind lastRemind = reminds.get(0);

                    if (workHour - lastRemind.getWorkHours() < mtHour) {

                        return;
                    }
                }

                maintainRemindJpa.save(remind);
            }
        }
    }

    private void dealMajor(List<MaintainInfo> majorList, MaintainLog maintainLog, DeviceInfo deviceInfo) {
        double workHour = deviceInfo.getWorkHours() == null ? 0 : deviceInfo.getWorkHours();
        double month = calcMonth(deviceInfo.getDebugTime(), new Date());

        if (CollectionUtils.isEmpty(majorList)) {
            return;
        }

        double monthGap = 0;
        boolean isFit = false;
        MaintainInfo fitMt = null;
        if (maintainLog != null) {
            double workHourGap = deviceInfo.getWorkHours() - maintainLog.getWorkHour();
            monthGap = calcMonth(maintainLog.getMaintainTime(), new Date());

            int index = 0;
            for (int i = 0; i < majorList.size(); i++) {
                if (maintainLog.getItemId() == majorList.get(i).getItemId()) {
                    index = i;
                    break;
                }
            }

            fitMt = majorList.get(0);
            double intervalHour = fitMt.getWorkHoursBegin();
            double intervalMonth = fitMt.getBuyMonthsBegin();
            if (index + 1 < majorList.size()) {
                fitMt = majorList.get(index + 1);
                intervalHour = majorList.get(index + 1).getWorkHoursBegin() - majorList.get(index).getWorkHoursBegin();
                intervalMonth = majorList.get(index + 1).getBuyMonthsBegin() - majorList.get(index).getBuyMonthsBegin();
            }

            if (workHourGap >= intervalHour || monthGap >= intervalMonth) {
                isFit = true;
            }
        } else {
            for (int i = majorList.size() - 1; i >= 0; i--) {
                MaintainInfo mtInfo = majorList.get(i);
                if (workHour >= mtInfo.getWorkHoursBegin() || month >= mtInfo.getBuyMonthsBegin()) {
                    isFit = true;
                    fitMt = mtInfo;
                    break;
                }
            }
        }

        if (isFit) {
            List<MaintainRemind> reminds = maintainRemindJpa.findByEquipIdAndPolicyIdAndIsMajor(deviceInfo.getId(), fitMt.getPolicyId(), 1, Sort.by(Sort.Direction.DESC, new String[]{"CreateTime", "workHours"}));
            if (CollectionUtils.isNotEmpty(reminds)) {
                MaintainRemind lastRemind = reminds.get(0);
                if (lastRemind.getStatus() == 1) {

                    return;
                }
            }

            MaintainRemind remind = new MaintainRemind();
            remind.setEquipId(deviceInfo.getId());
            remind.setPolicyId(fitMt.getPolicyId());
            remind.setPolicyDetailId(fitMt.getId());
            remind.setWorkHours(workHour);
            remind.setBuyMonths(Double.valueOf(month));

            remind.setTimes(reminds.size() + 1);
            remind.setCreateTime(new Date());
            remind.setStatus(1);
            remind.setIsMajor(1);
            remind.setIntervalMonth(monthGap);

            maintainRemindJpa.save(remind);
        }
    }

    private double calcMonth(Date date1, Date date2) {
        Calendar past = Calendar.getInstance();
        past.setTime(date1);
        Calendar now = Calendar.getInstance();
        now.setTime(date2);
        int year = now.get(Calendar.YEAR) - past.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) - past.get(Calendar.MONTH);
        double day = (now.get(Calendar.DAY_OF_MONTH) - past.get(Calendar.DAY_OF_MONTH)) / 30;

        return CommonUtil.keepDecimal(year * 12 + month + day, 1);
    }
}
