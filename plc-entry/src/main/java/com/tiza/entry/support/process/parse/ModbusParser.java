package com.tiza.entry.support.process.parse;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.model.DataProcessAdapter;
import com.diyiliu.plugin.model.Header;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.DateUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.air.cluster.KafkaUtil;
import com.tiza.air.cluster.RedisUtil;
import com.tiza.air.model.KafkaMsg;
import com.tiza.entry.support.facade.FaultInfoJpa;
import com.tiza.entry.support.facade.dto.*;
import com.tiza.entry.support.model.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: ModbusParser
 * Author: DIYILIU
 * Update: 2019-04-28 09:16
 */

@Slf4j
@Service
public class ModbusParser extends DataProcessAdapter {

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private ICache faultCacheProvider;

    @Resource
    private ICache alarmCacheProvider;

    @Resource
    private FaultInfoJpa faultInfoJpa;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RedisUtil redisUtil;

    @Value("${redis.equipment-info-key}")
    private String summaryKey;

    @Value("${redis.equipment-detail-key}")
    private String detailKey;

    @Override
    public void parse(byte[] content, Header header) {
        DtuHeader dtuHeader = (DtuHeader) header;
        String deviceId = dtuHeader.getDeviceId();
        if (!deviceCacheProvider.containsKey(deviceId)) {

            log.warn("设备不存在[{}]!", deviceId);
            return;
        }
        ByteBuf buf = Unpooled.copiedBuffer(content);

        DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);
        long equipId = deviceInfo.getId();

        MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
        SendMsg sendMsg = msgMemory.getCurrent();
        if (1 == sendMsg.getResult()) {
            return;
        }
        sendMsg.setResult(1);

        // 加入历史下发缓存
        msgMemory.getMsgMap().put(sendMsg.getKey(), sendMsg);

        StoreGroup storeGroup = new StoreGroup();
        // 当前表
        Map summary = storeGroup.getSummary();
        // 字典表
        List<DetailInfo> detailList = storeGroup.getDetailList();

        // 从站地址:功能码:起始地址:数量
        String key = sendMsg.getKey();
        String[] strArray = key.split(":");
        int start = Integer.parseInt(strArray[2]);

        List<PointUnit> unitList = sendMsg.getUnitList();
        for (int i = 0; i < unitList.size(); i++) {
            PointUnit pointUnit = unitList.get(i);
            int address = pointUnit.getAddress();
            int type = pointUnit.getType();

            // 数字量单独处理
            if (5 == type) {
                unpackUnit(content, pointUnit, summary, detailList);
                break;
            }

            // 只有dword是四个字节,其他(除数字量)均为二个字节
            int len = type == 4 ? 4 : 2;
            int begin = (address - start) * 2;

            // 按字(两个字节)解析
            byte[] bytes = new byte[len];
            buf.getBytes(begin, bytes);
            unpackUnit(bytes, pointUnit, summary, detailList);
        }

        flushSummary(equipId, summary);
        flushDetail(deviceInfo, detailList);
    }

    /**
     * 解析单元包
     *
     * @param bytes
     * @param pointUnit
     * @param summary
     * @param detailList
     */
    private void unpackUnit(byte[] bytes, PointUnit pointUnit, Map summary, List<DetailInfo> detailList) {
        int type = pointUnit.getType();
        // bit类型
        if (5 == type || 1 == type) {
            String binaryStr = CommonUtil.bytes2BinaryStr(bytes);

            int address = pointUnit.getAddress();
            int length = pointUnit.getTags().length;
            if (binaryStr.length() - length >= 0) {
                for (int i = 0; i < length; i++) {
                    PointInfo p = pointUnit.getPoints()[i];

                    int offset = (p.getAddress() - address) * 8;
                    int position = offset + p.getPosition();
                    String v = binaryStr.substring(position, position + 1);

                    DetailInfo d = fill(p, v, summary, detailList);
                    // 故障点
                    if (p.getFaultId() != null && p.getFaultType() > 0) {
                        d.setFaultId(p.getFaultId());
                        d.setFaultType(p.getFaultType());
                    }
                }
            }

            return;
        }

        if (3 == type || 4 == type) {
            PointInfo p = pointUnit.getPoints()[0];
            int val = CommonUtil.byte2int(bytes);
            String v = Integer.toString(val);

            // 1:float;2:int;3:hex
            int dataType = p.getDataType();
            if (1 == dataType) {
                v = String.valueOf(Float.intBitsToFloat(val));
            } else if (3 == dataType) {
                v = String.format("%02X", val);
            }

            fill(p, v, summary, detailList);
        }
    }

    private DetailInfo fill(PointInfo pointInfo, String value, Map summary, List<DetailInfo> detailList) {
        // 当前表
        if (pointInfo.getSaveType() == 1) {
            String f = pointInfo.getField();
            summary.put(f, value);
        }

        // 详情表
        DetailInfo d = new DetailInfo();
        d.setPointId(pointInfo.getId());
        d.setTag(pointInfo.getTag());
        d.setValue(value);
        detailList.add(d);

        return d;
    }

    /**
     * 更新当前信息
     *
     * @param equipId
     * @param paramValues
     */
    private void flushSummary(long equipId, Map paramValues) {
        paramValues.put("dtuStatus", 1);
        paramValues.put("lastTime", DateUtil.dateToString(new Date()));

        redisUtil.hset(summaryKey + equipId, paramValues);
    }

    /**
     * 更新详细信息
     *
     * @param deviceInfo
     * @param detailInfoList
     */
    private void flushDetail(DeviceInfo deviceInfo, List<DetailInfo> detailInfoList) {
        if (CollectionUtils.isNotEmpty(detailInfoList)) {
            long equipId = deviceInfo.getId();

            Map redisMap = new HashMap();
            for (DetailInfo detailInfo : detailInfoList) {
                detailInfo.setEquipId(equipId);
                detailInfo.setLastTime(new Date());

                // 处理故障报警
                if (detailInfo.getFaultId() != null) {
                    dealFault(detailInfo);
                }

                redisMap.put(detailInfo.getTag(), detailInfo.getValue() + "|" + detailInfo.getLastTime().getTime());
            }
            redisMap.put("lastTime", DateUtil.dateToString(new Date()));

            // 写入 redis
            redisUtil.hset(detailKey + equipId, redisMap);
            // 写入 kafka
            toKafka(String.valueOf(equipId), detailInfoList);
            // 处理自定义报警
            doAlarm(deviceInfo, detailInfoList);
        }
    }

    /**
     * 故障处理
     *
     * @param detailInfo
     */
    private void dealFault(DetailInfo detailInfo) {
        Long key = detailInfo.getEquipId();
        int value = Integer.parseInt(detailInfo.getValue());

        synchronized (key) {
            List<FaultInfo> list = (List<FaultInfo>) faultCacheProvider.get(key);

            boolean flag = false;
            FaultInfo currentFault = null;
            if (CollectionUtils.isNotEmpty(list)) {
                for (FaultInfo info : list) {
                    if (info.getTag().equals(detailInfo.getTag())) {
                        flag = true;
                        currentFault = info;
                        break;
                    }
                }
            }

            // 产生报警
            if (1 == value && !flag) {
                FaultInfo faultInfo = new FaultInfo();
                faultInfo.setEquipId(detailInfo.getEquipId());
                faultInfo.setFaultId(detailInfo.getFaultId());
                faultInfo.setPointId(detailInfo.getPointId());
                faultInfo.setTag(detailInfo.getTag());
                faultInfo.setValue(detailInfo.getValue());
                faultInfo.setStartTime(new Date());
                faultInfo.setFaultType(detailInfo.getFaultType());
                faultInfo.setAlarmType(1);

                faultInfo = faultInfoJpa.save(faultInfo);
                if (faultInfo != null) {
                    if (CollectionUtils.isEmpty(list)) {
                        list = new ArrayList();
                        list.add(faultInfo);
                        faultCacheProvider.put(key, list);
                    } else {
                        list.add(faultInfo);
                    }

                    // 更新当前状态
                    updateAlarm(list);
                }

                return;
            }

            // 解除报警
            if (0 == value && flag) {
                long id = currentFault.getId();
                FaultInfo faultInfo = faultInfoJpa.findById(id);
                faultInfo.setEndTime(new Date());
                faultInfo.setValue("0");

                faultInfo = faultInfoJpa.save(faultInfo);
                if (faultInfo != null) {
                    list.removeIf(f -> f.getTag().equals(detailInfo.getTag()));

                    // 更新当前状态
                    if (CollectionUtils.isEmpty(list)) {
                        String sql = "UPDATE equipment_info SET HardAlarmStatus = 0 where equipmentId = " + key;
                        jdbcTemplate.update(sql);
                    } else {
                        updateAlarm(list);
                    }
                }
            }
        }
    }


    /**
     * 更新当前报警等级
     *
     * @param list
     */
    private void updateAlarm(List<FaultInfo> list) {
        list = list.stream().sorted(Comparator.comparing(FaultInfo::getFaultType)).collect(Collectors.toList());
        FaultInfo faultInfo = list.get(0);
        String sql = "UPDATE equipment_info SET HardAlarmStatus = " + faultInfo.getFaultType() + " where equipmentId = " + faultInfo.getEquipId();
        jdbcTemplate.update(sql);
    }


    public void doAlarm(DeviceInfo deviceInfo, List<DetailInfo> detailInfoList) {
        List<AlarmInfo> alarmInfoList = deviceInfo.getAlarmInfoList();
        if (CollectionUtils.isEmpty(alarmInfoList)) {

            return;
        }

        for (DetailInfo detailInfo : detailInfoList) {
            long pointId = detailInfo.getPointId();
            String value = detailInfo.getValue();

            for (AlarmInfo alarmInfo : alarmInfoList) {

                List<Long> pointIds = alarmInfo.getPointIds();
                if (pointIds.contains(pointId)) {
                    int i = pointIds.indexOf(pointId);
                    AlarmDetail alarmDetail = alarmInfo.getAlarmDetails().get(i);
                    boolean flag = executeScript(alarmDetail.getExpression(), value);

                    dealAlarm(deviceInfo, alarmInfo, alarmDetail, flag);
                }
            }
        }
    }

    private void dealAlarm(DeviceInfo deviceInfo, AlarmInfo alarmInfo, AlarmDetail alarmDetail, boolean flag) {
        long equipId = deviceInfo.getId();
        String key = equipId + ":" + alarmInfo.getId();

        long detailId = alarmDetail.getId();
        if (alarmCacheProvider.containsKey(key)) {
            AlarmGroup alarmGroup = (AlarmGroup) alarmCacheProvider.get(key);
            Map<Long, AlarmItem> itemMap = alarmGroup.getItemMap();

            // 是否报警
            if (flag) {
                if (!itemMap.containsKey(detailId)) {
                    AlarmItem item = new AlarmItem();
                    item.setId(detailId);
                    item.setDuration(alarmDetail.getDuration());
                    item.setStartTime(System.currentTimeMillis());

                    itemMap.put(detailId, item);
                }

                // 报警处理
                updateAlarm(deviceInfo, alarmInfo, alarmGroup, 1);
            }
            // 解除报警
            else {
                if (itemMap.containsKey(detailId) && alarmGroup.getStartTime() != null) {

                    updateAlarm(deviceInfo, alarmInfo, alarmGroup, 0);
                }
            }

        } else {
            if (flag) {
                AlarmItem item = new AlarmItem();
                item.setId(detailId);
                item.setDuration(alarmDetail.getDuration());
                item.setStartTime(System.currentTimeMillis());

                AlarmGroup alarmGroup = new AlarmGroup();
                alarmGroup.setItemMap(new HashMap() {
                    {
                        this.put(detailId, item);
                    }
                });

                alarmCacheProvider.put(key, alarmGroup);
            }
        }
    }

    private void updateAlarm(DeviceInfo deviceInfo, AlarmInfo alarmInfo, AlarmGroup alarmGroup, int result) {
        // 产生报警
        if (result == 1) {
            Map<Long, AlarmItem> itemMap = alarmGroup.getItemMap();

            boolean alarm = true;
            Set<Long> set = itemMap.keySet();
            for (Iterator<Long> iterator = set.iterator(); iterator.hasNext(); ) {
                long key = iterator.next();
                AlarmItem item = itemMap.get(key);
                if (System.currentTimeMillis() - item.getStartTime() > item.getDuration() * 1000) {
                    item.setStatus(1);
                } else {
                    alarm = false;
                }
            }

            if (alarm && alarmGroup.getStartTime() == null
                    && alarmInfo.getAlarmDetails().size() == itemMap.size()) {

                FaultInfo faultInfo = new FaultInfo();
                faultInfo.setFaultId(alarmInfo.getFaultId());
                faultInfo.setEquipId(deviceInfo.getId());
                faultInfo.setStartTime(new Date());
                faultInfo.setAlarmType(2);
                faultInfo.setAlarmPolicyId(alarmInfo.getId());

                faultInfo = faultInfoJpa.save(faultInfo);
                if (faultInfo != null) {
                    log.info("产生报警[{}, {}]", deviceInfo.getId(), faultInfo.getAlarmPolicyId());
                    alarmGroup.setStartTime(new Date());
                    alarmGroup.setId(faultInfo.getId());
                }
            }

            return;
        }
        // 解除报警
        if (result == 0 && alarmGroup.getId() != null) {
            long equipId = deviceInfo.getId();
            String key = equipId + ":" + alarmInfo.getId();

            long fId = alarmGroup.getId();

            FaultInfo faultInfo = faultInfoJpa.findById(fId);
            faultInfo.setValue("0");
            faultInfo.setEndTime(new Date());
            faultInfo = faultInfoJpa.save(faultInfo);
            if (faultInfo != null) {
                log.info("解除报警[{}, {}]", deviceInfo.getId(), faultInfo.getAlarmPolicyId());
                alarmCacheProvider.remove(key);
            }
        }
    }

    private boolean executeScript(String script, String value) {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("$value", value);
        try {
            Object object = engine.eval(script, bindings);
            if (object == null || !(object instanceof Boolean)) {
                return false;
            } else {
                return (boolean) object;
            }
        } catch (ScriptException e) {
            e.printStackTrace();
            log.error("解析表达式错误[{}, {}]", script, e.getMessage());
        }

        return false;
    }

    /**
     * Kafka 解析数据存入队列
     *
     * @param id
     * @param list
     */
    public void toKafka(String id, List list) {
        //log.info("设备[{}]解析数据...", id);

        long time = System.currentTimeMillis();
        Map map = new HashMap();
        map.put("id", id);
        map.put("timestamp", time);
        map.put("metrics", JacksonUtil.toJson(list));

        KafkaUtil.send(new KafkaMsg(String.valueOf(id), JacksonUtil.toJson(map)));
    }
}
