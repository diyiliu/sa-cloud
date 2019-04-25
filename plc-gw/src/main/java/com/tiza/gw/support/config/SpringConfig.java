package com.tiza.gw.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.cache.ram.RamCacheProvider;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.netty.server.DtuServer;
import com.tiza.gw.support.listener.RedisMsgListener;
import com.tiza.gw.support.util.KafkaUtil;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Description: SpringConfig
 * Author: DIYILIU
 * Update: 2019-04-18 10:54
 */

@Configuration
@ComponentScan("com.tiza.gw")
@PropertySource("classpath:config.properties")
public class SpringConfig {

    @Resource
    private Environment environment;

    @Order(1)
    @Bean(initMethod = "init")
    public DtuServer dtuServer() throws Exception {
        String profile = environment.getProperty("env");
        loadProperties("conf" + File.separator + profile + ".properties");

        int port = environment.getProperty("dtu-port", Integer.class);
        DtuServer dtuServer = new DtuServer();
        dtuServer.setPort(port);

        return dtuServer;
    }

    @Bean(initMethod = "init")
    public RedisMsgListener redisMsgListener() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(30);
        config.setMaxIdle(3);
        config.setMaxWaitMillis(5000);

        String host = environment.getProperty("redis.host");
        int port = environment.getProperty("redis.port", Integer.class);
        String password = environment.getProperty("redis.password");
        int database = environment.getProperty("redis.database", Integer.class);
        JedisPool pool = new JedisPool(config, host, port, 2000, StringUtils.isEmpty(password) ? null : password, database);

        String subChannel = environment.getProperty("redis.channel");
        RedisMsgListener listener = new RedisMsgListener();
        listener.setJedisPool(pool);
        listener.setSubChannel(subChannel);

        return listener;
    }

    @Bean(initMethod = "init")
    public KafkaUtil kafkaUtil() {
        Map kafkaProp = new HashMap();
        // 消息传递到broker时的序列化方式
        kafkaProp.put("serializer.class", StringEncoder.class.getName());
        // 0 不获取反馈(消息有可能传输失败)
        // 1 获取消息传递给leader后反馈(其他副本有可能接受消息失败)
        // -1 所有in-sync replicas接受到消息时的反馈
        kafkaProp.put("request.required.acks", "1");
        // 内部发送数据是异步还是同步 sync：同步(来一条数据提交一条不缓存), 默认 async：异步
        kafkaProp.put("producer.type", "async");
        // 重试次数
        kafkaProp.put("message.send.max.retries", "3");


        String brokerList = environment.getProperty("kafka.broker-list");
        String topic = environment.getProperty("kafka.raw-topic");

        Properties props = new Properties();
        props.put("metadata.broker.list", brokerList);
        props.putAll(kafkaProp);
        Producer<String, String> producer = new Producer(new ProducerConfig(props));

        return new KafkaUtil(producer, topic);
    }

    /**
     * 加载配置文件
     *
     * @param path
     * @throws IOException
     */
    public void loadProperties(String path) throws IOException {
        Properties properties = new Properties();
        properties.load(new ClassPathResource(path).getInputStream());

        ConfigurableEnvironment confEnv = (ConfigurableEnvironment) environment;
        confEnv.getPropertySources().addFirst(new PropertiesPropertySource("activeProperties", properties));
    }

    /**
     * spring 工具类
     *
     * @return
     */
    @Bean
    public SpringUtil springUtil() {

        return new SpringUtil();
    }

    /**
     * 设备在线缓存
     *
     * @return
     */
    @Bean
    public ICache onlineCacheProvider() {

        return new RamCacheProvider();
    }
}
