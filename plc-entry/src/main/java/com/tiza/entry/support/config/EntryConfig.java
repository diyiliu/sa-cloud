package com.tiza.entry.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.cache.ram.RamCacheProvider;
import com.diyiliu.plugin.util.SpringUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

/**
 * Description: EntryConfig
 * Author: DIYILIU
 * Update: 2019-04-25 15:51
 */

@Configuration
@EnableScheduling
public class EntryConfig {


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
     * spring jdbcTemplate
     *
     * @param dataSource
     * @return
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }

    /**
     * 自定义报警缓存
     *
     * @return
     */
    @Bean
    public ICache alarmCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 故障缓存
     *
     * @return
     */
    @Bean
    public ICache faultCacheProvider() {

        return new RamCacheProvider();
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

    /**
     * 下发缓存
     *
     * @return
     */
    @Bean
    public ICache sendCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 数据库设备缓存
     *
     * @return
     */
    @Bean
    public ICache deviceCacheProvider() {

        return new RamCacheProvider();
    }


    /**
     * 读功能集缓存
     *
     * @return
     */
    @Bean
    public ICache readFnCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 写功能集缓存
     *
     * @return
     */
    @Bean
    public ICache writeFnCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 定时任务缓存
     *
     * @return
     */
    @Bean
    public ICache timerCacheProvider() {

        return new RamCacheProvider();
    }
}
