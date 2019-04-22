package com.tiza.gw.netty.handler.codec;

import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.gw.support.config.SaConstant;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;

/**
 * Description: DtuEncoder
 * Author: DIYILIU
 * Update: 2018-01-26 10:41
 */

public class DtuEncoder extends MessageToByteEncoder {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {

        if (msg == null) {

            return;
        }
        ByteBuf byteBuf = (ByteBuf) msg;

        // 下发内容
        byte[] content = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(content);
        // 校验位
        byte[] crc = CommonUtil.checkCRC(content);

        byte[] bytes = Unpooled.copiedBuffer(content, crc).array();

        // 记录原始数据
        toRecord(ctx, bytes);

        out.writeBytes(bytes);
    }

    /**
     * 写入原始数据日志
     *
     * @param context
     * @param bytes
     */
    private void toRecord(ChannelHandlerContext context, byte[] bytes) {
        Attribute attribute = context.channel().attr(AttributeKey.valueOf(SaConstant.NETTY_DEVICE_ID));
        String deviceId = (String) attribute.get();

        if (StringUtils.isNotEmpty(deviceId)){
//            KafkaClient kafkaClient = SpringUtil.getBean("kafkaClient");
//            kafkaClient.toKafka(deviceId, bytes, 2);
        }
    }
}
