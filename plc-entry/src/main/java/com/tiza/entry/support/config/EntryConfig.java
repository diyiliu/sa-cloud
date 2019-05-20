package com.tiza.entry.support.config;

import com.tiza.air.cluster.HBaseUtil;
import com.tiza.air.cluster.KafkaUtil;
import com.tiza.air.cluster.RedisUtil;
import com.tiza.entry.support.config.properties.HBaseProperties;
import com.tiza.entry.support.config.properties.KafkaProperties;
import com.tiza.entry.support.config.properties.RedisProperties;
import com.tiza.entry.support.process.DtuDataProcess;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Properties;

/**
 * Description: EntryConfig
 * Author: DIYILIU
 * Update: 2019-04-25 15:51
 */

@Configuration
@EnableScheduling
@EnableConfigurationProperties({KafkaProperties.class, HBaseProperties.class, RedisProperties.class})
public class EntryConfig {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private HBaseProperties hbaseProperties;

    @Autowired
    private RedisProperties redisProperties;


    @Bean(initMethod = "init")
    public DtuDataProcess dataProcess(){
        String zkConnect = kafkaProperties.getZkConnect();
        String topic = kafkaProperties.getRawTopic();

        Properties props = new Properties();
        props.put("zookeeper.connect", zkConnect);
        // 指定消费组名
        props.put("group.id", "sa");
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        props.put("enable.auto.commit", "true");
        // 设置消费位置 smallest 最小 largest 最大
        props.put("auto.offset.reset", "largest");

        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        ConsumerConnector consumer = kafka.consumer.Consumer.createJavaConsumerConnector(consumerConfig);

        DtuDataProcess dataProcess = new DtuDataProcess();
        dataProcess.setTopic(topic);
        dataProcess.setConsumer(consumer);

        return dataProcess;
    }

    @Bean(initMethod = "init")
    public KafkaUtil kafkaUtil(){
        Properties props = new Properties();
        props.put("metadata.broker.list", kafkaProperties.getBrokerList());

        // 消息传递到broker时的序列化方式
        props.put("serializer.class", StringEncoder.class.getName());
        props.put("request.required.acks", "1");
        // 内部发送数据是异步还是同步 sync：同步(来一条数据提交一条不缓存), 默认 async：异步
        props.put("producer.type", "async");
        // 重试次数
        props.put("message.send.max.retries", "3");
        // 增加1ms的延迟
        props.put("linger.ms", 10);

        Producer<String, String> producer = new Producer(new ProducerConfig(props));
        return new KafkaUtil(producer, kafkaProperties.getDataTopic());
    }

    @Bean
    public HBaseUtil hbaseUtil(){
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", hbaseProperties.getZkQuorum());
        config.set("hbase.zookeeper.property.clientPort", hbaseProperties.getZkPort());

        HBaseUtil hbaseUtil = new HBaseUtil();
        hbaseUtil.setConfig(config);
        hbaseUtil.setTable(hbaseProperties.getDataTable());

        return hbaseUtil;
    }

    @Bean
    public RedisUtil redisUtil(){
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(30);
        config.setMaxIdle(3);
        config.setMaxWaitMillis(5000);

        String password = redisProperties.getPassword();
        if (StringUtils.isEmpty(password)){
            password = null;
        }

        JedisPool pool = new JedisPool(config, redisProperties.getHost(), redisProperties.getPort(), 2000, password, redisProperties.getDatabase());
        return new RedisUtil(pool);
    }

}
