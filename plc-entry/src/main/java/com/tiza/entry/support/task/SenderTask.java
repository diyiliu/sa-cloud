package com.tiza.entry.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.air.cluster.RedisUtil;
import com.tiza.air.model.SubMsg;
import com.tiza.entry.support.facade.SendLogJpa;
import com.tiza.entry.support.facade.dto.DeviceInfo;
import com.tiza.entry.support.facade.dto.SendLog;
import com.tiza.entry.support.model.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

/**
 * Description: SenderTask
 * Author: DIYILIU
 * Update: 2019-04-26 09:28
 */

@Slf4j
@Service
public class SenderTask implements ITask {
    /**
     * 线程管理器
     **/
    private final ExecutorService executor = Executors.newCachedThreadPool();
    /**
     * 线程缓存
     **/
    private ConcurrentLinkedQueue threadPool = new ConcurrentLinkedQueue();

    @Resource
    private SendLogJpa sendLogJpa;

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private ICache onlineCacheProvider;

    @Resource
    private ICache queryGroupCache;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private ICache singlePoolCache;

    @Resource
    private RedisUtil redisUtil;

    @Value("${redis.channel}")
    private String pubChannel;


    @Scheduled(fixedRate = 10 * 1000, initialDelay = 5 * 1000)
    public void execute() {

        Set set = onlineCacheProvider.getKeys();
        for (Iterator iterator = set.iterator(); iterator.hasNext(); ) {
            String deviceId = (String) iterator.next();

            if (!deviceCacheProvider.containsKey(deviceId)) {
                continue;
            }
            DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);
            String version = deviceInfo.getSoftVersion();

            Map<Integer, List<QueryFrame>> fnQuery = (Map<Integer, List<QueryFrame>>) queryGroupCache.get(version);
            for (Iterator<Integer> iter = fnQuery.keySet().iterator(); iter.hasNext(); ) {
                int fnCode = iter.next();

                List<QueryFrame> frameList = fnQuery.get(fnCode);
                if (frameList.size() < 1) {
                    continue;
                }

                for (QueryFrame frame : frameList) {
                    // String qKey = frame.getSite() + ":" + frame.getCode() + ":" + frame.getStart();
                    // long frequency = frame.getPointUnits().get(0).getFrequency();
                    SendMsg msg = buildMsg(deviceId, frame);
                    toSend(msg);
                }
            }
        }
    }

    private SendMsg buildMsg(String deviceId, QueryFrame queryFrame) {
        List<PointUnit> units = queryFrame.getPointUnits();
        int type = units.get(0).getType();

        int site = queryFrame.getSite();
        int code = queryFrame.getCode();
        int star = queryFrame.getStart();
        int count = queryFrame.getCount().get();

        List<PointUnit> unitList = queryFrame.getPointUnits();
        if (type == 5) {
            count = unitList.get(0).getPoints().length;
        }

        ByteBuf byteBuf = Unpooled.buffer(6);
        byteBuf.writeByte(site);
        byteBuf.writeByte(code);
        byteBuf.writeShort(star);
        byteBuf.writeShort(count);
        byte[] bytes = byteBuf.array();

        String key = site + ":" + code + ":" + star;
        SendMsg sendMsg = new SendMsg();
        sendMsg.setDeviceId(deviceId);
        sendMsg.setCmd(code);
        sendMsg.setBytes(bytes);
        // 0: 查询; 1: 设置
        sendMsg.setType(0);
        sendMsg.setKey(key);
        sendMsg.setUnitList(unitList);

        return sendMsg;
    }

    public void toSend(SendMsg sendMsg) {
        String deviceId = sendMsg.getDeviceId();

        MsgPool pool;
        if (singlePoolCache.containsKey(deviceId)) {
            pool = (MsgPool) singlePoolCache.get(deviceId);
        } else {
            pool = new MsgPool();
            pool.setDeviceId(deviceId);
            singlePoolCache.put(deviceId, pool);
        }

        // 指令标记
        String key = sendMsg.getKey();
        // 指令类型
        int type = sendMsg.getType();

        // 设置指令优先执行
        if (1 == type || sendMsg.isFirst()) {
            pool.getMsgQueue().addFirst(sendMsg);
        } else {
            // 过滤重复查询指令
            if (!pool.getKeyList().contains(key)) {
                pool.getKeyList().add(key);
                pool.getMsgQueue().add(sendMsg);
            }
        }

        if (!threadPool.contains(deviceId)) {
            threadPool.add(deviceId);
            publish(pool);
        }
    }

    /**
     * 发布到 redis
     *
     * @param pool
     */
    private void publish(MsgPool pool) {
        final String deviceId = pool.getDeviceId();
        executor.execute(() -> {
            try {
                Queue<SendMsg> msgPool = pool.getMsgQueue();
                while (!msgPool.isEmpty()) {
                    if (!isBlock(deviceId)) {
                        SendMsg msg = msgPool.poll();

                        // 清除查询指令队列
                        String key = msg.getKey();
                        if (StringUtils.isNotEmpty(key)) {
                            pool.getKeyList().remove(key);
                        }

                        JedisPool jedisPool = redisUtil.getPool();
                        try (Jedis jedis = jedisPool.getResource()){
                            SubMsg subMsg = new SubMsg();
                            subMsg.setDevice(deviceId);
                            subMsg.setKey(key);
                            subMsg.setType(msg.getType());
                            subMsg.setData(CommonUtil.bytesToStr(msg.getBytes()));
                            subMsg.setTime(System.currentTimeMillis());

                            jedis.publish(pubChannel, JacksonUtil.toJson(subMsg));
                        }

                        // 参数设置
                        if (1 == msg.getType()) {
                            updateLog(msg, 1, "");
                        }

                        // 添加下发缓存
                        MsgMemory msgMemory;
                        if (sendCacheProvider.containsKey(deviceId)) {
                            msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
                        } else {
                            msgMemory = new MsgMemory();
                            msgMemory.setDeviceId(deviceId);
                            sendCacheProvider.put(deviceId, msgMemory);
                        }
                        msg.setDateTime(System.currentTimeMillis());
                        msgMemory.setCurrent(msg);
                    }
                    Thread.sleep(2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                threadPool.remove(deviceId);
            }
        });
    }


    /**
     * 设备离线
     *
     * @param deviceId
     */
    public void offline(String deviceId) {
        onlineCacheProvider.remove(deviceId);

        if (sendCacheProvider.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            msgMemory.setCurrent(null);
        }

        // 清除线程缓存
        if (singlePoolCache.containsKey(deviceId)) {
            MsgPool pool = (MsgPool) singlePoolCache.get(deviceId);
            pool.getMsgQueue().clear();
            pool.getKeyList().clear();
        }
    }

    /**
     * 判断设备下行指令是否阻塞
     *
     * @param deviceId
     * @return
     */
    private boolean isBlock(String deviceId) {
        if (sendCacheProvider.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            SendMsg current = msgMemory.getCurrent();
            if (current != null && current.getResult() == 0) {
                //  超时丢弃未应答指令
                if (System.currentTimeMillis() - current.getDateTime() > 5 * 1000) {
                    log.info("设备[{}]丢弃超时未应答指令[{}]!", current.getDeviceId(), CommonUtil.bytesToStr(current.getBytes()));
                    current.setResult(1);
                    if (current.getType() == 1) {
                        updateLog(current, 4, "");
                    }

                    return false;

                }

                return true;
            }
        }

        return false;
    }

    /**
     * 更新下发指令状态
     *
     * @param msg
     * @param result
     * @param replyMsg
     */
    public void updateLog(SendMsg msg, int result, String replyMsg) {
        SendLog sendLog = sendLogJpa.findById(msg.getRowId().longValue());
        // 0:未发送;1:已发送;2:成功;3:失败;4:超时;
        if (sendLog != null) {
            sendLog.setResult(result);
            sendLog.setSendData(CommonUtil.bytesToStr(msg.getBytes()));
            sendLog.setReplyData(replyMsg);
            sendLogJpa.save(sendLog);
        }
    }
}
