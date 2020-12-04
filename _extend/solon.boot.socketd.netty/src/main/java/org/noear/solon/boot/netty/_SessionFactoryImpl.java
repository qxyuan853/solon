package org.noear.solon.boot.netty;

import io.netty.channel.Channel;
import org.noear.solon.core.message.Session;
import org.noear.solon.extend.socketd.SessionFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

class _SessionFactoryImpl extends SessionFactory {
    @Override
    protected Session getSession(Object conn) {
        if (conn instanceof Channel) {
            return _SocketSession.get((Channel) conn);
        } else {
            throw new IllegalArgumentException("This conn requires a netty Channel type");
        }
    }

    @Override
    protected Collection<Session> getOpenSessions() {
        return Collections.unmodifiableCollection(_SocketSession.sessions.values());
    }

    @Override
    protected void removeSession(Object conn) {
        if (conn instanceof Channel) {
            _SocketSession.remove((Channel) conn);
        } else {
            throw new IllegalArgumentException("This conn requires a netty Channel type");
        }
    }

    @Override
    protected Session createSession(URI uri, boolean autoReconnect) {
        NioConnector connector = new NioConnector(uri.getHost(), uri.getPort());

        return new _SocketSession(connector, autoReconnect);
    }
}
