package org.noear.solon.sessionstate.redisson;

import org.noear.solon.Solon;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.Bridge;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.util.LogUtil;

public class XPluginImp implements Plugin {
    @Override
    public void start(AopContext context) {
        if (Solon.app().enableSessionState() == false) {
            return;
        }

        if (Bridge.sessionStateFactory().priority() >= RedissonSessionStateFactory.SESSION_STATE_PRIORITY) {
            return;
        }
        /*
         *
         * server.session.state.redis:
         * server:
         * password:
         * db: 31
         * maxTotal: 200
         *
         * */

        if (RedissonSessionStateFactory.getInstance().redisClient() == null) {
            return;
        }

        Bridge.sessionStateFactorySet(RedissonSessionStateFactory.getInstance());

        LogUtil.global().info("Session: Redis session state plugin is loaded");
    }
}