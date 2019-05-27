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
    private ICache deviceCacheProvider;

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

                if (!deviceCacheProvider.containsKey(device)){
                    log.warn("设备[{}]未注册", device);
                    continue;
                }

                ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
                // 从站地址
                int site = byteBuf.readUnsignedByte();
                int code = byteBuf.readUnsignedByte();

                // 上行
                if (1 == flow) {
                    onlineCacheProvider.put(device, time);

                    // 心跳、注册包
                    if (bytesStr.startsWith("404040") || bytesStr.startsWith("242424")) {
                        log.info("设备上线: [{}]", bytesStr);
                        continue;
                    }

                    if (!sendCacheProvider.containsKey(device)) {
                        continue;
                    }
                    // log.info("设备[{}]收到数据: [{}]", device, bytesStr);

                    MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(device);
                    SendMsg sendMsg = msgMemory.getCurrent();
                    if (sendMsg == null || sendMsg.getResult() == 1) {
                        log.warn("忽略设备[{}]过期数据[{}]", device, bytesStr);
                        continue;
                    }

                    int type = sendMsg.getType();
                    // 查询匹配 从站地址, 功能码
                    if (type == 0) {
                        // 加入下发缓存
                        msgMemory.getMsgMap().put(sendMsg.getKey(), sendMsg);

                        // 字节数
                        int count = byteBuf.readUnsignedByte();
                        byte[] content = new byte[count];
                        byteBuf.readBytes(content);

                        /** 类型(1:bit;2:byte;3:word;4:dword;5:digital)*/
                        PointUnit pointUnit = sendMsg.getUnitList().get(0);
                        int dataType = pointUnit.getType();

                        // 从站地址:功能码:起始地址:数量
                        String key = sendMsg.getKey();
                        String[] strArray = key.split(":");
                        int realSite = Integer.valueOf(strArray[0]);
                        int realCode = Integer.valueOf(strArray[1]);
                        int realCount = Integer.valueOf(strArray[3]);

                        if (5 == dataType) {
                            int n = realCount / 8;
                            realCount = realCount % 8 == 0 ? n : n + 1;
                        } else {
                            realCount *= 2;
                        }

                        if (site != realSite || code != realCode || count != realCount) {
                            log.warn("设备[{}]应答不匹配[下行{}, 上行{}]",
                                    device, CommonUtil.bytesToStr(sendMsg.getBytes()), bytesStr);
                            continue;
                        }
                        // 修改下发消息状态
                        sendMsg.setResult(1);

                        DtuHeader dtuHeader = new DtuHeader();
                        dtuHeader.setDeviceId(device);
                        dtuHeader.setContent(content);
                        dtuHeader.setSendMsg(sendMsg);
                        modbusParser.parse(content, dtuHeader);
                    }

                    // 设置应答
                    else if (type == 1 && sendMsg.getResult() == 0) {
                        sendMsg.setResult(1);
                        String str = CommonUtil.bytesToStr(bytes);

                        log.info("[设置] 设备[{}]应答[{}, {}]成功。", device, sendMsg.getTags(), str);
                        // 查询设置
                        dealMsg(sendMsg, str);

                        continue;
                    }
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
