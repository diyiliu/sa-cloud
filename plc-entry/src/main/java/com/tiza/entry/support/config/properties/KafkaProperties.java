package com.tiza.entry.support.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description: KafkaProperties
 * Author: DIYILIU
 * Update: 2019-04-28 11:44
 */

@Data
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private String brokerList;

    private String rawTopic;

    private String dataTopic;
}
