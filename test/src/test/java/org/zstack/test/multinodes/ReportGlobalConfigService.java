package org.zstack.test.multinodes;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.AbstractService;
import org.zstack.header.message.Message;
import org.zstack.test.core.config.GlobalConfigForTest;

/**
 */
public class ReportGlobalConfigService extends AbstractService {
    public static String SERVICE_ID = "ReportGlobalConfigService";

    @Autowired
    private CloudBus bus;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof ReportGlobalConfigMsg) {
            ReportGlobalConfigReply reply = new ReportGlobalConfigReply();
            reply.setValue(GlobalConfigForTest.TEST.value());
            reply.setValue2(GlobalConfigForTest.TEST2.value());
            bus.reply(msg, reply);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
