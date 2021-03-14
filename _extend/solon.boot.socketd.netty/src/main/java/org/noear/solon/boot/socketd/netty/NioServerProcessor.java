package org.noear.solon.boot.socketd.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.noear.solon.core.message.Message;
import org.noear.solon.core.message.Session;
import org.noear.solon.socketd.ListenerProxy;
import org.noear.solon.socketd.client.netty.NioSocketSession;

@ChannelHandler.Sharable
public class NioServerProcessor extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Session session = NioSocketSession.get(ctx.channel());
        ListenerProxy.getGlobal().onMessage(session, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        Session session = NioSocketSession.get(ctx.channel());
        ListenerProxy.getGlobal().onOpen(session);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);

        Session session = NioSocketSession.get(ctx.channel());
        ListenerProxy.getGlobal().onClose(session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Session session = NioSocketSession.get(ctx.channel());
        ListenerProxy.getGlobal().onError(session, cause);

        ctx.close();
    }
}