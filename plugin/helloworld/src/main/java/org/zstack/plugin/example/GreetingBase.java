package org.zstack.plugin.example;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GreetingBase implements Greeting {
    protected GreetingVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    public GreetingBase(GreetingVO self) {
        this.self = self;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteGreetingMsg) {
            handle((APIDeleteGreetingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteGreetingMsg msg) {
        dbf.remove(self);

        bus.publish(new APIDeleteGreetingEvent(msg.getId()));
    }
}
