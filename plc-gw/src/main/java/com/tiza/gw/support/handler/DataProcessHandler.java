package com.tiza.gw.support.handler;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.model.MsgPipeline;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.DateUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.air.cluster.KafkaUtil;
import com.tiza.air.model.KafkaMsg;
import com.tiza.air.model.SubMsg;
import com.tiza.gw.support.model.SinglePool;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.security.auth.Subject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description: DataProcessHandler
 * Author: DIYILIU
 * Update: 2019-04-24 14:02
 */

@Slf4j
@Service
public class DataProcessHandler {
    /**
     * 设备消息队列
     **/
    public final static Map<String, SinglePool> DEVICE_POOL = new ConcurrentHashMap();

    @Resource
    private ICache onlineCacheProvider;

    /**
     * 设备注册
     *
     * @param deviceId
     * @param context
     * @return
     */
    public void online(String deviceId, ChannelHandlerContext context) {
        MsgPipeline msgPipeline = new MsgPipeline();
        msgPipeline.setContext(context);

        onlineCacheProvider.put(deviceId, msgPipeline);
    }

    /**
     * 设备离线
     *
     * @param deviceId
     * @return
     */
    public void offline(String deviceId) {

        onlineCacheProvider.remove(deviceId);
    }

    /**
     * 设备就绪
     *
     * @param deviceId
     * @return
     */
    public void idle(String deviceId, ChannelHandlerContext ctx) {
        if (DataProcessHandler.DEVICE_POOL.containsKey(deviceId)) {
            SinglePool singlePool = DataProcessHandler.DEVICE_POOL.get(deviceId);
            Queue<SubMsg> queue = singlePool.getPool();

            if (!queue.isEmpty()) {
                SubMsg msg = queue.poll();
                if (System.currentTimeMillis() - msg.getTime() < 5 * 1000) {
                    byte[] bytes = CommonUtil.hexStringToBytes(msg.getData());
                    ctx.writeAndFlush(Unpooled.copiedBuffer(bytes));
                } else {
                    log.warn("设备[{}]指令过期[{{}, {}]", deviceId, DateUtil.dateToString(new Date(msg.getTime())), JacksonUtil.toJson(msg));
                }

                while (!queue.isEmpty()) {
                    msg = queue.poll();
                    log.warn("设备[{}]清理历史数据[{}]", deviceId, JacksonUtil.toJson(msg));
                }
            }
        }
    }


    /**
     * 准备写入 Kafka
     */
    public void toKafka(String deviceId, byte[] bytes, int flow) {
        log.info("[{}] 设备[{}]原始数据[{}]...", flow == 1 ? "上行" : "下行", deviceId, CommonUtil.bytesToStr(bytes));

        long time = System.currentTimeMillis();
        Map map = new HashMap();
        map.put("id", deviceId);
        map.put("timestamp", time);
        map.put("data", CommonUtil.bytesToStr(bytes));
        map.put("flow", flow);

        try {
            KafkaUtil.send(new KafkaMsg(deviceId, JacksonUtil.toJson(map)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
