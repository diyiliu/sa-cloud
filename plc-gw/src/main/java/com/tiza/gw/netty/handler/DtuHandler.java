package com.tiza.gw.netty.handler;

import com.tiza.gw.support.config.SaConstant;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Description: DtuHandler
 * Author: DIYILIU
 * Update: 2018-01-26 10:39
 */

@Slf4j
public class DtuHandler extends ChannelInboundHandlerAdapter {

    private Attribute attribute;


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("建立连接...");
        attribute = ctx.channel().attr(AttributeKey.valueOf(SaConstant.NETTY_DEVICE_ID));

        // 断开连接
        ctx.channel().closeFuture().addListener(
                (ChannelFuture future) -> {
                    String deviceId = (String) attribute.get();
                    if (StringUtils.isNotEmpty(deviceId)) {
                        log.info("设备[{}]断开连接...", deviceId);

                    }
                }
        );
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String deviceId = (String) attribute.get();
        ByteBuf byteBuf = (ByteBuf) msg;
        int address = byteBuf.readUnsignedByte();
        int code = byteBuf.readUnsignedByte();


        int length = byteBuf.readUnsignedByte();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("服务器异常...{}", cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        attribute = ctx.channel().attr(AttributeKey.valueOf(SaConstant.NETTY_DEVICE_ID));
        String deviceId = (String) attribute.get();

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (IdleState.READER_IDLE == event.state()) {
                //log.info("读超时(5s)[{}]..", deviceId);

            } else if (IdleState.WRITER_IDLE == event.state()) {
                //log.info("写超时...");
            } else if (IdleState.ALL_IDLE == event.state()) {

            }
        }
    }
}
