package org.noear.solon.boot.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.noear.solon.Solon;
import org.noear.solon.SolonApp;
import org.noear.solon.core.Plugin;
import org.noear.solon.extend.xsocket.SessionFactory;

public class XPluginImp implements Plugin {
    ServerBootstrap _server;

    public static String solon_boot_ver() {
        return "netty-xsocket/" + Solon.cfg().version();
    }

    @Override
    public void start(SolonApp app) {
        //注册会话工厂
        SessionFactory.setInstance(new _SessionFactoryImpl());

        if (app.enableSocket() == false) {
            return;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup wokerGroup = new NioEventLoopGroup();

        long time_start = System.currentTimeMillis();

        System.out.println("solon.Server:main: java.net.ServerSocket(netty-xsocket)");

        int _port = app.cfg().getInt("server.socket.port", 0);
        if (_port < 1) {
            _port = 20000 + app.port();
        }

        try {
            _server = new ServerBootstrap();
            //在服务器端的handler()方法表示对bossGroup起作用，而childHandler表示对wokerGroup起作用
            _server.group(bossGroup, wokerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new NioChannelInitializer());

            ChannelFuture channelFuture = _server.bind(_port).sync();
            //channelFuture.channel().closeFuture().sync();


            long time_end = System.currentTimeMillis();

            System.out.println("solon.Connector:main: netty-xsocket: Started ServerConnector@{[Socket]}{0.0.0.0:" + _port + "}");
            System.out.println("solon.Server:main: netty-xsocket: Started @" + (time_end - time_start) + "ms");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
//            bossGroup.shutdownGracefully();
//            wokerGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() throws Throwable {
        if (_server == null) {
            return;
        }

        System.out.println("solon.Server:main: netty-xsocket: Has Stopped " + solon_boot_ver());
    }
}
