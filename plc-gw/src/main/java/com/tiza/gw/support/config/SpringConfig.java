package com.tiza.gw.support.config;

import com.tiza.gw.netty.server.DtuServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

/**
 * Description: SpringConfig
 * Author: DIYILIU
 * Update: 2019-04-18 10:54
 */

@Configuration
@PropertySource("classpath:config.properties")
public class SpringConfig {

    @Resource
    private Environment environment;

    @Value("${dtu-port}")
    private Integer port;

    @Bean
    public DtuServer dtuServer() {

        DtuServer dtuServer = new DtuServer();
        dtuServer.setPort(port);
        dtuServer.init();

        return dtuServer;
    }
}
