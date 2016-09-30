package org.zstack.core.cloudbus;

import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.io.IOException;
import java.util.List;

/**
 */
public class MessageTracker extends AbstractConsumer implements Component {
    private Connection conn;
    private Channel chan;

    @Autowired
    private CloudBusImpl2 bus;

    void init() {
        try {
            ConnectionFactory connFactory = new ConnectionFactory();
            List<Address> addresses = CollectionUtils.transformToList(bus.getServerIps(), new Function<Address, String>() {
                @Override
                public Address call(String arg) {
                    return Address.parseAddress(arg);
                }
            });

            conn = connFactory.newConnection(addresses.toArray(new Address[]{}));
            chan = conn.createChannel();
            String name = MessageTracker.class.getName();
            chan.queueDeclare(name, true, false, true, null);
            chan.basicConsume(name, true, this);
            chan.queueBind(name, BusExchange.P2P.toString(), "#");
            chan.queueBind(name, BusExchange.BROADCAST.toString(), "#");
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {

    }
}
