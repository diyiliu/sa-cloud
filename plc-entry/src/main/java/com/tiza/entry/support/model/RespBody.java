package com.tiza.entry.support.model;

import lombok.Data;

/**
 * Description: RespBody
 * Author: DIYILIU
 * Update: 2019-05-08 10:09
 */

@Data
public class RespBody<T> {

    public RespBody() {

    }

    public RespBody(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /**
     * 1 请求成功
     * 2 系统异常
     */
    private int status = 1;

    private String message;

    private T data;
}
