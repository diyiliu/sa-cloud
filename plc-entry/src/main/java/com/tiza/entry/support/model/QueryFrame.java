package com.tiza.entry.support.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description: QueryFrame
 * Author: DIYILIU
 * Update: 2018-01-30 09:10
 */

public class QueryFrame {

    /** 从站地址 */
    private int site;

    /** 功能码 */
    private int code;

    /** 起始地址 */
    private int start;

    /** 字(word)数 max:60 */
    private AtomicInteger count = new AtomicInteger(0);

    /** 功能集中的点 */
    private List<PointUnit> pointUnits;

    public AtomicInteger getCount() {
        return count;
    }

    public void addCount() {

        this.count.incrementAndGet();
    }

    public void addCount(int i) {

        this.count.addAndGet(i);
    }

    public int getSite() {
        return site;
    }

    public void setSite(int site) {
        this.site = site;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public List<PointUnit> getPointUnits() {
        return pointUnits;
    }

    public void setPointUnits(List<PointUnit> pointUnits) {
        this.pointUnits = pointUnits;
    }
}
