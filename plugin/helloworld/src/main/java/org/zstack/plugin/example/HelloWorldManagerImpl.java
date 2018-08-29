package org.zstack.plugin.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class HelloWorldManagerImpl extends AbstractService implements HelloWorldManager, Component {
    private static final CLogger logger = Utils.getLogger(HelloWorldManagerImpl.class);

    @Autowired
    private CloudBus bus;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleAPIMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleAPIMessage(APIMessage msg) {
        if (msg instanceof APIHelloWorldMsg) {
            handle((APIHelloWorldMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIHelloWorldMsg msg) {
        logger.debug(String.format("say hello: %s", msg));

        APIHelloWorldEvent evt = new APIHelloWorldEvent(msg.getId());
        evt.setGreeting(msg.getGreeting());
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
