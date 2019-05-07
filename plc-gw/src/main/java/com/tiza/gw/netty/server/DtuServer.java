package com.tiza.gw.netty.server;

import com.tiza.gw.netty.handler.DtuHandler;
import com.tiza.gw.netty.handler.codec.DtuDecoder;
import com.tiza.gw.netty.handler.codec.DtuEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Description: DtuServer
 * Author: DIYILIU
 * Update: 2019-04-18 10:50
 */
@Slf4j
public class DtuServer {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int port;

    public void init() {
        executor.execute(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline().addLast(new DtuEncoder())
                                        .addLast(new DtuDecoder())
                                        .addLast(new IdleStateHandler(0, 0, 3000, TimeUnit.MILLISECONDS))
                                        .addLast(new DtuHandler());
                            }
                        });

                ChannelFuture f = b.bind(port).sync();
                log.info("DTU网关服务器启动, 端口[{}]...", port);
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully();
            }
        });
    }

    public void setPort(int port) {
        this.port = port;
    }
}
