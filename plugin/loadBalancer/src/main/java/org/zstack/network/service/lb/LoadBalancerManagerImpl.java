package org.zstack.network.service.lb;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerManagerImpl extends AbstractService {
    @Autowired
    private CloudBus bus;

    @Override
    public void handleMessage(Message msg) {

    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LoadBalancerConstants.SERVICE_ID);
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
