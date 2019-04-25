package com.tiza.gw.support.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Description: SinglePool
 * Author: DIYILIU
 * Update: 2019-04-25 09:01
 */

@Data
public class SinglePool {

    private String device;

    private Queue<byte[]> pool = new LinkedList();
}
