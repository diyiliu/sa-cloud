package com.tiza.entry.support.model;

import com.diyiliu.plugin.model.Header;

/**
 * Description: DtuHeader
 * Author: DIYILIU
 * Update: 2018-01-30 10:12
 */

public class DtuHeader extends Header {

    private String deviceId;

    private int address;

    private int code;

    private byte[] content = new byte[0];

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
