package com.tiza.entry.controller;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.air.cluster.RedisUtil;
import com.tiza.entry.support.facade.DetailInfoJpa;
import com.tiza.entry.support.facade.DeviceInfoJpa;
import com.tiza.entry.support.facade.dto.DetailInfo;
import com.tiza.entry.support.facade.dto.DeviceInfo;
import com.tiza.entry.support.facade.dto.PointInfo;
import com.tiza.entry.support.model.PointUnit;
import com.tiza.entry.support.model.RespBody;
import com.tiza.entry.support.model.SendMsg;
import com.tiza.entry.support.task.SenderTask;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * Description: SetupController
 * Author: DIYILIU
 * Update: 2019-04-30 09:28
 */

@Slf4j
@RestController
@Api(description = "设备指令下发接口")
public class SetupController {

    @Value("${redis.equipment-detail-key}")
    private String detailKey;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ICache onlineCacheProvider;

    @Resource
    private ICache writeFnCacheProvider;

    @Resource
    private DeviceInfoJpa deviceInfoJpa;

    @Resource
    private DetailInfoJpa detailInfoJpa;

    @Resource
    private SenderTask senderTask;

    @GetMapping("/latest")
    @ApiOperation(value = "数据同步", notes = "获取设备最新数据")
    public RespBody latestData(@RequestParam("equipId") String equipId) {
        Map dataMap = redisUtil.hgetAll(detailKey + equipId);
        if (MapUtils.isNotEmpty(dataMap)) {
            dataMap.remove("lastTime");
        }

        return buildResp(dataMap);
    }

    @PostMapping("/confirm")
    @ApiOperation(value = "数据确认", notes = "确认下发内容")
    public RespBody confirm(@RequestParam("equipId") long equipId, @RequestParam("key") String key, @RequestParam("value") String value) {
        DeviceInfo deviceInfo = deviceInfoJpa.findById(equipId).get();

        String dtuId = deviceInfo.getDtuId();
        if (!onlineCacheProvider.containsKey(dtuId)) {

            return buildResp(2, "设备离线");
        }

        PointUnit pointUnit = fetchUnit(deviceInfo.getSoftVersion(), key);
        if (pointUnit == null) {

            return buildResp(2, "功能集异常");
        }

        Map paramMap = new HashMap();
        paramMap.put(key, value);
        if (pointUnit.getTags().length == 1) {

            return buildResp(paramMap);
        }

        String[] tags = pointUnit.getTags();
        List<DetailInfo> details = detailInfoJpa.findByEquipIdAndTagIn(deviceInfo.getId(), tags);
        if (CollectionUtils.isEmpty(details)) {

            return buildResp(2, "功能集异常");
        }

        Map dataMap = redisUtil.hgetAll(detailKey + equipId);
        details.forEach(e -> {
            String tag = e.getTag();
            paramMap.put(tag, dataMap.get(tag));
        });

        return buildResp(paramMap);
    }

    @PostMapping("/setup")
    @ApiOperation(value = "参数设置", notes = "设置设备参数")
    public RespBody setup(@RequestParam("key") String key, @RequestParam("value") String value,
                          @RequestParam("equipId") long equipId, @RequestParam("rowId") Long rowId, @RequestBody(required = false) String jsonBody) throws IOException {

        DeviceInfo deviceInfo = deviceInfoJpa.findById(equipId).get();
        PointUnit pointUnit = fetchUnit(deviceInfo.getSoftVersion(), key);

        // 设置值转int
        int val;
        if (value.indexOf(".") > 0) {
            val = Float.floatToIntBits(Float.parseFloat(value));
        } else {
            val = Integer.parseInt(value);
        }

        // 寄存器数量(dword类型为2, 其他均为1)
        int count;
        // 从站地址
        int side;
        // 起始地址
        int address;

        // 单点下发
        if (1 == pointUnit.getTags().length) {
            PointInfo pointInfo = pointUnit.getPoints()[0];

            int type = pointInfo.getPointType();
            // 寄存器数量
            count = type == 4 ? 2 : 1;
            side = pointInfo.getSiteId();
            address = pointInfo.getAddress();
        } else {
            Map tagMap = JacksonUtil.toObject(jsonBody, HashMap.class);

            String[] tags = pointUnit.getTags();
            int length = tags.length;
            StringBuilder strBuf = new StringBuilder();
            // 最小单元为字(两个字节)
            for (int i = 0; i < 16; i++) {
                if (i < length) {
                    String tag = tags[i];
                    String v = "0";
                    if (tagMap.containsKey(tag)) {
                        v = String.valueOf(tagMap.get(tag));
                    }
                    if (key.equals(tag)) {
                        v = value;
                    }
                    strBuf.append(v);
                } else {
                    strBuf.append("0");
                }
            }
            String binaryStr = strBuf.toString();
            byte[] bytes = CommonUtil.binaryStr2Bytes(binaryStr);
            val = CommonUtil.byte2int(bytes);

            count = 1;
            side = pointUnit.getSiteId();
            address = pointUnit.getAddress();
        }
        // 下发单元
        List<PointUnit> unitList = new ArrayList();
        unitList.add(pointUnit);

        // 设备编号
        String dtuId = deviceInfo.getDtuId();
        // 功能码
        int code = pointUnit.getWriteFunction();

        byte[] bytes = toBytes(side, code, address, count, val);
        SendMsg sendMsg = new SendMsg();
        sendMsg.setRowId(rowId);
        sendMsg.setDeviceId(dtuId);
        sendMsg.setCmd(code);
        sendMsg.setBytes(bytes);
        // 0: 查询; 1: 设置
        sendMsg.setType(1);
        sendMsg.setUnitList(unitList);
        sendMsg.setTags(pointUnit.getTags());

        senderTask.toSend(sendMsg);
        log.info("设备[{}]参数[{},{}]等待下发[{}]...", dtuId, key, value, CommonUtil.bytesToStr(bytes));

        return buildResp(1, "下发成功");
    }

    /**
     * 获取下发单元
     *
     * @param softVersion
     * @param tag
     * @return
     */
    private PointUnit fetchUnit(String softVersion, String tag) {
        PointUnit pointUnit = null;
        if (writeFnCacheProvider.containsKey(softVersion)) {
            List<PointUnit> writeUnitList = (List<PointUnit>) writeFnCacheProvider.get(softVersion);

            for (PointUnit unit : writeUnitList) {
                List<String> tagList = Arrays.asList(unit.getTags());
                if (tagList.contains(tag)) {

                    pointUnit = unit;
                    break;
                }
            }
        }

        return pointUnit;
    }

    private byte[] toBytes(int site, int code, int address, int count, int value) {
        ByteBuf buf = Unpooled.buffer(7 + count * 2);
        buf.writeByte(site);
        buf.writeByte(code);
        buf.writeShort(address);
        buf.writeShort(count);
        buf.writeByte(count * 2);
        buf.writeBytes(CommonUtil.longToBytes(value, count * 2));

        return buf.array();
    }

    private RespBody buildResp(int status, String msg) {

        return new RespBody(status, msg, null);
    }

    private <T> RespBody buildResp(T data) {

        return new RespBody(1, "", data);
    }
}
