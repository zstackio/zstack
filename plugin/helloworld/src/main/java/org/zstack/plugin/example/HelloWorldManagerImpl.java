package org.zstack.plugin.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.operr;

public class HelloWorldManagerImpl extends AbstractService implements HelloWorldManager, Component {
    private static final CLogger logger = Utils.getLogger(HelloWorldManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof GreetingMessage) {
            passThrough((GreetingMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleAPIMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(GreetingMessage msg) {
        GreetingVO vo = dbf.findByUuid(msg.getGreetingUuid(), GreetingVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find GreetingVO[uuid:%s], it may have been deleted", msg.getGreetingUuid()));
        }

        new GreetingBase(vo).handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleAPIMessage(APIMessage msg) {
        if (msg instanceof APIHelloWorldMsg) {
            handle((APIHelloWorldMsg) msg);
        } else if (msg instanceof APICreateGreetingMsg) {
            handle((APICreateGreetingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateGreetingMsg msg) {
        GreetingVO vo = new GreetingVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setGreeting(msg.getGreeting());
        vo = dbf.updateAndRefresh(vo);

        APICreateGreetingEvent evt = new APICreateGreetingEvent(msg.getId());
        evt.setInventory(vo.toInventory());
        bus.publish(evt);
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
