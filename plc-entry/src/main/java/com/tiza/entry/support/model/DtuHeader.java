package com.tiza.entry.support.model;

import com.diyiliu.plugin.model.Header;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description: DtuHeader
 * Author: DIYILIU
 * Update: 2018-01-30 10:12
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class DtuHeader extends Header {

    private String deviceId;

    private byte[] content = new byte[0];

    private SendMsg sendMsg;
}
