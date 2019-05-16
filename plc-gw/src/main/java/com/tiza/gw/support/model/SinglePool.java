package com.tiza.gw.support.model;

import lombok.Data;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Description: SinglePool
 * Author: DIYILIU
 * Update: 2019-04-25 09:01
 */

@Data
public class SinglePool {

    private String device;

    private Deque<byte[]> pool = new LinkedList();
}
