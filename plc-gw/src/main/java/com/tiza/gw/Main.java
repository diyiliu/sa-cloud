package com.tiza.gw;

import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.config.GwConfig;

/**
 * Description: Main
 * Author: DIYILIU
 * Update: 2019-04-18 10:34
 */
public class Main {

    public static void main(String[] args) {

        SpringUtil.init(GwConfig.class);
    }
}
