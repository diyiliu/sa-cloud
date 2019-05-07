package com.tiza.air.cluster;

import com.tiza.air.model.KafkaMsg;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Description: KafkaUtil
 * Author: DIYILIU
 * Update: 2019-04-24 14:26
 */

public class KafkaUtil {
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final static Queue<KafkaMsg> pool = new ConcurrentLinkedQueue();

    private Producer producer;
    private String topic;

    public KafkaUtil() {

    }

    public KafkaUtil(Producer producer, String topic) {
        this.topic = topic;
        this.producer = producer;
    }

    public void init() {
        // 延时 2s 执行一次
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            while (!pool.isEmpty()) {
                KafkaMsg data = pool.poll();

                producer.send(new KeyedMessage(topic, data.getKey(), data.getValue()));
            }
        }, 10, 2, TimeUnit.SECONDS);
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public static void send(KafkaMsg msg) {
        pool.add(msg);
    }
}
