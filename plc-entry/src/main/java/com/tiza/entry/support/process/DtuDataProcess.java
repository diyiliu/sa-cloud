package com.tiza.entry.support.process;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.entry.support.model.DtuHeader;
import com.tiza.entry.support.model.MsgMemory;
import com.tiza.entry.support.model.PointUnit;
import com.tiza.entry.support.model.SendMsg;
import com.tiza.entry.support.process.parse.ModbusParser;
import com.tiza.entry.support.task.SenderTask;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Description: DtuDataProcess
 * Author: DIYILIU
 * Update: 2019-04-26 10:12
 */

@Slf4j
public class DtuDataProcess implements Runnable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private String topic;

    private ConsumerConnector consumer;

    @Resource
    private ICache onlineCacheProvider;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private SenderTask senderTask;

    @Resource
    private ModbusParser modbusParser;

    public void init() {
        executor.execute(this);
    }

    @Override
    public void run() {
        Map<String, Integer> topicCountMap = new HashMap();
        topicCountMap.put(topic, new Integer(1));

        try {
            Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);

            KafkaStream<byte[], byte[]> stream = consumerMap.get(topic).get(0);
            ConsumerIterator<byte[], byte[]> it = stream.iterator();
            while (it.hasNext()) {
                String data = new String(it.next().message());
                Map dataMap = JacksonUtil.toObject(data, HashMap.class);

                String device = (String) dataMap.get("id");
                int flow = (int) dataMap.get("flow");
                long time = (long) dataMap.get("timestamp");

                String bytesStr = (String) dataMap.get("data");
                byte[] bytes = CommonUtil.hexStringToBytes(bytesStr);

                ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
                // 从站地址
                int site = byteBuf.readUnsignedByte();
                int code = byteBuf.readUnsignedByte();

                // 上行
                if (1 == flow) {
                    onlineCacheProvider.put(device, time);

                    // 心跳、注册包
                    if (bytesStr.startsWith("404040") || bytesStr.startsWith("242424")) {
                        log.info("空数据包: [{}]", bytesStr);
                        continue;
                    }

                    if (!sendCacheProvider.containsKey(device)) {
                        log.error("数据异常, 找不到下行数据与之对应。");
                        continue;
                    }

                    MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(device);
                    SendMsg sendMsg = msgMemory.getCurrent();
                    if (sendMsg == null || sendMsg.getResult() == 1) {
                        log.error("过滤异常数据。");
                        continue;
                    }

                    int type = sendMsg.getType();
                    // 查询匹配 从站地址, 功能码
                    if (type == 0) {
                        PointUnit unit = sendMsg.getUnitList().get(0);
                        if (site != unit.getSiteId() || code != unit.getReadFunction()) {
                            log.error("设备[{}], 从站地址[{}, {}], 功能码[{}, {}], 上下行不匹配, 断开连接!",
                                    device, site, unit.getSiteId(), code, unit.getReadFunction());
                            continue;
                        }
                    }

                    // 设置应答
                    if (type == 1 && sendMsg.getResult() == 0) {
                        sendMsg.setResult(1);
                        String str = CommonUtil.bytesToStr(bytes);

                        log.info("[设置] 设备[{}]应答[{}, {}]成功。", device, sendMsg.getTags(), str);
                        // 查询设置
                        dealMsg(sendMsg, str);

                        continue;
                    }

                    DtuHeader dtuHeader = new DtuHeader();
                    dtuHeader.setDeviceId(device);
                    dtuHeader.setAddress(site);
                    dtuHeader.setCode(code);
                    dtuHeader.setContent(bytes);

                    modbusParser.parse(bytes, dtuHeader);
                    continue;
                }

                // 下行
                if (2 == flow) {

                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.warn("Kafka 消费线程退出!!!");
    }

    /**
     * 查询设置的参数
     *
     * @param msg
     */
    private void dealMsg(SendMsg msg, String content) {
        senderTask.updateLog(msg, 2, content);

        PointUnit pointUnit = msg.getUnitList().get(0);
        if (pointUnit.getFrequency() < 30) {

            return;
        }

        // 类型
        int type = pointUnit.getType();

        int site = pointUnit.getSiteId();
        int code = pointUnit.getReadFunction();
        int start = pointUnit.getAddress();
        int count = pointUnit.getType() == 4 ? 2 : 1;

        // 数字量
        if (type == 5) {
            count = pointUnit.getTags().length;
        }

        SendMsg sendMsg = senderTask.produceMsg(site, code, start, count);
        sendMsg.setDeviceId(msg.getDeviceId());
        // 0: 查询; 1: 设置
        sendMsg.setType(0);
        sendMsg.setUnitList(msg.getUnitList());
        sendMsg.setFirst(true);

        senderTask.toSend(sendMsg);
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setConsumer(ConsumerConnector consumer) {
        this.consumer = consumer;
    }
}
