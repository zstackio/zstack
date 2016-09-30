package org.zstack.test.multinodes;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 */
public class SilentService extends AbstractService {
    private static final CLogger logger = Utils.getLogger(SilentService.class);

    public static final String SERVICE_ID = "silentService";

    @Autowired
    private CloudBus bus;

    @Override
    public void handleMessage(Message msg) {
        logger.debug(String.format("I am silent service, ignore message %s", JSONObjectUtil.toJsonString(msg)));
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

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
}
