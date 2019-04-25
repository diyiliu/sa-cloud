package com.tiza.entry.support.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Description: MsgPool
 * Author: DIYILIU
 * Update: 2019-03-28 10:11
 */

@Data
public class MsgPool {

    private String deviceId;

    private List<String> keyList = new ArrayList();

    private Deque<SendMsg> msgQueue = new LinkedList();
}


