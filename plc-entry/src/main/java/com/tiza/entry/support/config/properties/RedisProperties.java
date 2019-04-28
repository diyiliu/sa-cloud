package com.tiza.entry.support.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description: RedisProperties
 * Author: DIYILIU
 * Update: 2019-04-28 11:45
 */

@Data
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {

    private String host;

    private Integer port;

    private String password;

    private Integer database;

    private String channel;
}
