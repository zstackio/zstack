package org.zstack.plugin.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

public class HelloWorldManagerImpl extends AbstractService implements HelloWorldManager, Component {
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
        bus.dealWithUnknownMessage(msg);
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
