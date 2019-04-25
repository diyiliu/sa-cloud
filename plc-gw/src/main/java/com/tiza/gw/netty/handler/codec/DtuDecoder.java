package com.tiza.gw.netty.handler.codec;

import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.config.SaConstant;
import com.tiza.gw.support.handler.DataProcessHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Description: DtuDecoder
 * Author: DIYILIU
 * Update: 2018-01-26 10:41
 */

@Slf4j
public class DtuDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {

            return;
        }

        // 绑定数据
        Attribute attribute = ctx.channel().attr(AttributeKey.valueOf(SaConstant.NETTY_DEVICE_ID));
        // 数据处理类
        DataProcessHandler processHandler = SpringUtil.getBean("dataProcessHandler");

        in.markReaderIndex();
        byte b1 = in.readByte();
        byte b2 = in.readByte();
        byte b3 = in.readByte();

        String deviceId;
        if (b1 == b2 && b1 == b3) {
            byte[] content = new byte[in.readableBytes()];
            in.readBytes(content);

            deviceId = new String(content);
            byte[] bytes = Unpooled.copiedBuffer(new byte[]{b1, b2, b2}, content).array();

            if (0x40 == b1 || 0x24 == b1) {
                attribute.set(deviceId);
                processHandler.online(deviceId, ctx);
            }
            processHandler.toKafka(deviceId, bytes, 1);
        } else {
            deviceId = (String) attribute.get();
            if (deviceId == null) {
                log.error("设备未注册, 断开连接!");
                ctx.close();
                return;
            }
            in.resetReaderIndex();

            // 从站地址
            int site = in.readByte();
            // 功能码
            int code = in.readByte();

            byte[] content;
            switch (code) {
                case 0x10: // 设置应答
                    if (in.readableBytes() < 6) {
                        in.resetReaderIndex();
                        return;
                    }

                    in.resetReaderIndex();
                    content = new byte[6];
                    in.readBytes(content);
                    break;
                default:
                    // 查询应答
                    int length = in.readUnsignedByte();
                    if (in.readableBytes() < length + 2) {

                        in.resetReaderIndex();
                        return;
                    }
                    in.resetReaderIndex();

                    content = new byte[3 + length];
                    in.readBytes(content);
            }

            // CRC校验码
            byte crc0 = in.readByte();
            byte crc1 = in.readByte();
            byte[] bytes = Unpooled.copiedBuffer(content, new byte[]{crc0, crc1}).array();

            // 验证校验位
            byte[] checkCRC = CommonUtil.checkCRC(content);
            if (crc0 != checkCRC[0] || crc1 != checkCRC[1]) {
                log.error("设备[{}]数据[{}], CRC校验码[{}, {}]错误, 断开连接!", deviceId,
                        CommonUtil.bytesToStr(bytes), String.format("%x", checkCRC[0]), String.format("%x", checkCRC[1]));
                ctx.close();
                return;
            }
            processHandler.toKafka(deviceId, bytes, 1);

            out.add(Unpooled.copiedBuffer(content));
        }
    }
}
