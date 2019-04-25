package com.tiza.gw.support.model;

/**
 * Description: KafkaMsg
 * Author: DIYILIU
 * Update: 2018-08-08 11:30
 */
public class KafkaMsg {
    private String key;

    private String value;

    public KafkaMsg() {

    }

    public KafkaMsg(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
