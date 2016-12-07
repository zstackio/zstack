package org.zstack.test.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.AbstractService;
import org.zstack.header.host.ChangeVmPasswordMsg;
import org.zstack.header.host.ChangeVmPasswordReply;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/12/1.
 */

public class MevocoHostBaseSimulator extends AbstractService {
    private CLogger logger = Utils.getLogger(MevocoHostBaseSimulator.class);
    public static final String SERVICE_ID = "mevocoHostBaseService";
    @Autowired
    private CloudBus bus;

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
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof ChangeVmPasswordMsg) {
            handle((ChangeVmPasswordMsg) msg);
        } else {
            logger.error(String.format("can't process msg type: %s", msg.getClass().getSimpleName()));
        }
    }

    private void handle(final ChangeVmPasswordMsg msg) {
        logger.debug(String.format("SimulatorHost handle the message, hostid = %s ", msg.getHostUuid()));
        ChangeVmPasswordReply reply = new ChangeVmPasswordReply();
        reply.setSuccess(true);
        reply.setVmAccountPreference(msg.getAccountPerference());
        bus.reply(msg, reply);
    }
}
