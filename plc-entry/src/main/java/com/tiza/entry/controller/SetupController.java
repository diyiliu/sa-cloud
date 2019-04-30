package com.tiza.entry.controller;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.entry.support.facade.DetailInfoJpa;
import com.tiza.entry.support.facade.DeviceInfoJpa;
import com.tiza.entry.support.facade.dto.DetailInfo;
import com.tiza.entry.support.facade.dto.DeviceInfo;
import com.tiza.entry.support.facade.dto.PointInfo;
import com.tiza.entry.support.model.PointUnit;
import com.tiza.entry.support.model.SendMsg;
import com.tiza.entry.support.task.SenderTask;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: SetupController
 * Author: DIYILIU
 * Update: 2019-04-30 09:28
 */

@Slf4j
@RestController
@Api(description = "设备指令下发接口")
public class SetupController {

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

    /**
     * 参数设置
     *
     * @param key
     * @param value
     * @param equipId
     * @param rowId
     * @param response
     * @return
     */
    @PostMapping("/setup")
    @ApiOperation(value = "参数设置", notes = "设置设备参数")
    public String setup(@RequestParam("key") String key, @RequestParam("value") String value,
                        @RequestParam("equipId") String equipId, @RequestParam("rowId") Long rowId, HttpServletResponse response) {

        DeviceInfo deviceInfo = deviceInfoJpa.findById(Long.parseLong(equipId)).get();
        String dtuId = deviceInfo.getDtuId();
        if (!onlineCacheProvider.containsKey(dtuId)) {

            response.setStatus(500);
            return "设备离线。";
        }

        String softVersion = deviceInfo.getSoftVersion();
        if (!writeFnCacheProvider.containsKey(softVersion)) {

            response.setStatus(500);
            return "未配置设备功能集。";
        }

        List<PointUnit> writeUnitList = (List<PointUnit>) writeFnCacheProvider.get(softVersion);
        PointUnit pointUnit = null;
        for (PointUnit unit : writeUnitList) {
            List<String> tagList = Arrays.asList(unit.getTags());
            if (tagList.contains(key)) {

                pointUnit = unit;
                break;
            }
        }

        if (pointUnit == null) {
            response.setStatus(500);
            return "功能集中未找到TAG[" + key + "]。";
        }

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
            String[] tags = pointUnit.getTags();
            List<DetailInfo> details = detailInfoJpa.findByEquipIdAndTagIn(deviceInfo.getId(), tags);
            if (CollectionUtils.isEmpty(details)) {

                response.setStatus(500);
                return "TA[{" + key + "}]节点异常。";
            }

            int length = tags.length;
            Map<String, String> tagValue = details.stream().collect(Collectors.toMap(DetailInfo::getTag, DetailInfo::getValue));
            StringBuilder strBuf = new StringBuilder();
            // 最小单元为字(两个字节)
            for (int i = 0; i < 16; i++) {
                if (i < length) {
                    String tag = tags[i];
                    String v = "0";
                    if (tagValue.containsKey(tag)) {
                        v = tagValue.get(tag);
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

        return "设置成功。";
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
}
