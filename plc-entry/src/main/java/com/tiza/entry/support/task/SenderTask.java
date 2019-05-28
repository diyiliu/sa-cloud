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
import com.tiza.entry.support.model.MsgMemory;
import com.tiza.entry.support.model.MsgPool;
import com.tiza.entry.support.model.QueryFrame;
import com.tiza.entry.support.model.SendMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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
public class SenderTask implements ITask, InitializingBean {
    /**
     * 主定时任务
     **/
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

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
    private JdbcTemplate jdbcTemplate;

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

    public void execute() {
        Set set = onlineCacheProvider.getKeys();
        log.info("在线设备列表: {}", JacksonUtil.toJson(set));

        for (Iterator iterator = set.iterator(); iterator.hasNext(); ) {
            String deviceId = (String) iterator.next();
            if (!deviceCacheProvider.containsKey(deviceId)) {
                log.warn("设备[{}]未注册", deviceId);
                continue;
            }

            DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);
            String version = deviceInfo.getSoftVersion();
            Map<Integer, List<QueryFrame>> fnQuery = (Map<Integer, List<QueryFrame>>) queryGroupCache.get(version);

            if (MapUtils.isNotEmpty(fnQuery)) {
                for (Iterator<Integer> iter = fnQuery.keySet().iterator(); iter.hasNext(); ) {
                    int fnCode = iter.next();

                    List<QueryFrame> frameList = fnQuery.get(fnCode);
                    if (CollectionUtils.isNotEmpty(frameList)) {
                        for (QueryFrame frame : frameList) {
                            SendMsg msg = buildMsg(deviceId, frame);
                            if (onTime(deviceId, msg.getKey(), frame.getFrequency())) {
                                toSend(msg);
                            }
                        }
                    }
                }
            }
        }
    }

    private SendMsg buildMsg(String deviceId, QueryFrame queryFrame) {
        int site = queryFrame.getSite();
        int code = queryFrame.getCode();
        int start = queryFrame.getStart();
        int count = queryFrame.getCount();

        SendMsg sendMsg = produceMsg(site, code, start, count);
        sendMsg.setDeviceId(deviceId);
        // 0: 查询; 1: 设置
        sendMsg.setType(0);
        sendMsg.setUnitList(queryFrame.getPointUnits());

        return sendMsg;
    }

    public SendMsg produceMsg(int site, int code, int start, int count) {
        ByteBuf byteBuf = Unpooled.buffer(6);
        byteBuf.writeByte(site);
        byteBuf.writeByte(code);
        byteBuf.writeShort(start);
        byteBuf.writeShort(count);
        byte[] bytes = byteBuf.array();

        String key = site + ":" + code + ":" + start + ":" + count;
        SendMsg sendMsg = new SendMsg();
        sendMsg.setCmd(code);
        sendMsg.setBytes(bytes);
        sendMsg.setKey(key);

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
            log.info("设备[{}]开启线程 ... ", deviceId);
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
                    if (isBlock(deviceId)) {
                        // 阻塞休眠
                        Thread.sleep(10000);
                    }else {
                        SendMsg msg = msgPool.poll();

                        // 清除查询指令队列
                        String key = msg.getKey();
                        if (StringUtils.isNotEmpty(key)) {
                            pool.getKeyList().remove(key);
                        }

                        // 发布
                        push(msg);
                        // 响应间隔
                        Thread.sleep(5000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                log.info("设备[{}]退出线程 ... ", deviceId);
                threadPool.remove(deviceId);
            }
        });
    }

    public void push(SendMsg msg) {
        String deviceId = msg.getDeviceId();
        String key = msg.getKey();
        long now = System.currentTimeMillis();

        JedisPool jedisPool = redisUtil.getPool();
        try (Jedis jedis = jedisPool.getResource()) {

            // 添加下发缓存
            MsgMemory msgMemory;
            if (sendCacheProvider.containsKey(deviceId)) {
                msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            } else {
                msgMemory = new MsgMemory();
                msgMemory.setDeviceId(deviceId);
                sendCacheProvider.put(deviceId, msgMemory);
            }

            msg.setDateTime(now);
            msgMemory.setCurrent(msg);

            SubMsg subMsg = new SubMsg();
            subMsg.setDevice(deviceId);
            subMsg.setKey(key);
            subMsg.setType(msg.getType());
            subMsg.setData(CommonUtil.bytesToStr(msg.getBytes()));
            subMsg.setTime(now);

            String dataJson = JacksonUtil.toJson(subMsg);
            jedis.publish(pubChannel, dataJson);
            // log.info("发布 Redis: [{}, {}, {}]", deviceId, key, subMsg.getData());

            // 参数设置
            if (1 == msg.getType()) {
                updateLog(msg, 1, "");
            }
        }
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

        // 设备离线由 mysql 事件自动检测
        // String sql = "UPDATE equipment_info SET DtuStatus = 0, LastTime = ? WHERE EquipmentId = ?";
        // jdbcTemplate.update(sql, new Object[]{new Date(), deviceId});
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
                int re = current.getRepeat();
                if (re < 3) {
                    push(current);
                    log.warn("设备[{}]第[{}]次重发[{}] ... ", deviceId, re, current.getKey());
                }
                //  超时丢弃未应答指令
                else if (System.currentTimeMillis() - current.getDateTime() > 30 * 1000) {
                    if (current.getType() == 1) {
                        updateLog(current, 4, "");
                        log.warn("设备[{}]参数设置超时[{}] ... ", deviceId, CommonUtil.bytesToStr(current.getBytes()));
                    }
                }

                return true;
            }
        }

        return false;
    }

    /**
     * 校验查询频率
     *
     * @param qKey
     * @param interval
     * @return
     */
    private boolean onTime(String deviceId, String qKey, long interval) {
        if (sendCacheProvider.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            SendMsg msg = msgMemory.getMsgMap().get(qKey);
            if (msg == null) {

                return true;
            }

            // 时间间隔 秒
            double nowGap = System.currentTimeMillis() - msg.getDateTime() * 0.001;
            // 系统默认最大间隔
            int max = 3 * 60;

            interval = interval > max ? max : interval;
            if (nowGap < interval) {
                return false;
            }
        }

        return true;
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

    @Override
    public void afterPropertiesSet() {
        scheduledExecutor.scheduleAtFixedRate(() -> execute(), 10, 5, TimeUnit.SECONDS);
    }
}
