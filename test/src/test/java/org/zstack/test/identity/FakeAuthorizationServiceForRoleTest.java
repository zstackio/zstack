package org.zstack.test.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.AbstractService;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.Message;

public class FakeAuthorizationServiceForRoleTest extends AbstractService {
    @Autowired
    private CloudBus bus;

    public static final String SERVICE_ID = "FakeAuthorizationServiceForRoleTest";
    public static final String ALLOW_POLICY_ROLE = "test:allow";
    public static final String DENY_POLICY_ROLE = "test:deny";

    @Override
    public boolean start() {
        bus.registerService(this);
        return true;
    }

    @Override
    public boolean stop() {
        bus.unregisterService(this);
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        APIEvent evt = new APIEvent(msg.getId());
        if (msg instanceof FakePolicyAllowMsg) {
            bus.publish(evt);
        } else if (msg instanceof FakePolicyDenyMsg) {
            bus.publish(evt);
        } else if (msg instanceof FakePolicyAllowHas2RoleMsg) {
            bus.publish(evt);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

}
