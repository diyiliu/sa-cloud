package com.tiza.entry.support.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description: HBaseProperties
 * Author: DIYILIU
 * Update: 2019-04-28 11:45
 */

@Data
@ConfigurationProperties(prefix = "hbase")
public class HBaseProperties {

    private String zkQuorum;

    private String zkPort;

    private String dataTable;

//    private byte[] family = Bytes.toBytes("1");
}
