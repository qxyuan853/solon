package org.noear.solon.cloud.extend.rabbitmq.service;

import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.extend.rabbitmq.RabbitmqProps;
import org.noear.solon.cloud.extend.rabbitmq.impl.RabbitChannelFactory;
import org.noear.solon.cloud.extend.rabbitmq.impl.RabbitConfig;
import org.noear.solon.cloud.extend.rabbitmq.impl.RabbitConsumer;
import org.noear.solon.cloud.extend.rabbitmq.impl.RabbitProducer;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.service.CloudEventObserverEntity;
import org.noear.solon.cloud.service.CloudEventService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author noear
 * @since 1.2
 */
public class CloudEventServiceImp implements CloudEventService {

    RabbitProducer producer;
    RabbitConsumer consumer;

    public CloudEventServiceImp(String server) {

        try {
            RabbitConfig config = new RabbitConfig();
            config.server = server;
            config.username = RabbitmqProps.instance.getUsername();
            config.password = RabbitmqProps.instance.getPassword();

            RabbitChannelFactory factory = new RabbitChannelFactory(config);

            producer = new RabbitProducer(factory);
            consumer = new RabbitConsumer(factory);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean publish(Event event) {
        // 设置消息属性 发布消息 (exchange:交换机名, Routing key, props:消息属性, body:消息体);
        try {
            return producer.publish(event);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    Map<String, CloudEventObserverEntity> observerMap = new HashMap<>();

    @Override
    public void attention(EventLevel level, String group, String topic, CloudEventHandler observer) {
        if (observerMap.containsKey(topic)) {
            return;
        }

        observerMap.put(topic, new CloudEventObserverEntity(level, group, topic, observer));
    }

    public void subscribe() {
        try {
            for (Map.Entry<String, CloudEventObserverEntity> kv : observerMap.entrySet()) {
                //绑定观察者
                consumer.attach(kv.getKey(), kv.getValue());
            }
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
}
