package com.tiza.entry.support.model;

import lombok.Data;

import java.util.List;

/**
 * Description: WordGroup
 * Author: DIYILIU
 * Update: 2018-04-23 09:58
 */


@Data
public class WordGroup {

    private Integer frequency;

    private List<PointUnit> pointUnits;
}
