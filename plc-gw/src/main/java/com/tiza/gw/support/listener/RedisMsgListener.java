package com.tiza.gw.support.listener;

import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.gw.support.handler.DataProcessHandler;
import com.tiza.gw.support.model.SinglePool;
import com.tiza.gw.support.model.SubMsg;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Description: RedisMsgListener
 * Author: DIYILIU
 * Update: 2019-04-24 09:34
 *
 */

@Slf4j
public class RedisMsgListener extends JedisPubSub {
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    private JedisPool jedisPool;

    private String subChannel;

    @Override
    public void onMessage(String channel, String message) {
        log.info("准备下发消息: [{}]", message);

        try {
            SubMsg msg = JacksonUtil.toObject(message, SubMsg.class);

            String device = msg.getDevice();
            byte[] bytes = CommonUtil.hexStringToBytes(msg.getData());

            SinglePool singlePool;
            if (DataProcessHandler.DEVICE_POOL.containsKey(device)){
                singlePool = DataProcessHandler.DEVICE_POOL.get(device);
            }else {
                singlePool = new SinglePool();
                singlePool.setDevice(device);
                DataProcessHandler.DEVICE_POOL.put(device, singlePool);
            }

            singlePool.getPool().add(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(){
        service.execute(() -> {
            try(Jedis jedis = jedisPool.getResource()){
                log.info("订阅 Redis 频道[{}]", subChannel);

                jedis.subscribe(this, subChannel);
            }
        });
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void setSubChannel(String subChannel) {
        this.subChannel = subChannel;
    }
}
