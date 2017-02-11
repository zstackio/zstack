package org.zstack.test.multinodes;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.AbstractService;
import org.zstack.header.message.LockResourceMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class SilentLockService extends AbstractService {
    public static final String SERVICE_ID = "SilentLockService";

    public static class DoLockMessage extends NeedReplyMessage {
        public String toLockServiceId;
        public String toManagementNodeUuid;
    }

    public static class SilentLockResourceMsg extends LockResourceMessage {
    }

    @Autowired
    private CloudBus bus;

    @Override
    public void handleMessage(final Message msg) {
        if (msg instanceof DoLockMessage) {
            DoLockMessage dmsg = (DoLockMessage) msg;
            SilentLockResourceMsg smsg = new SilentLockResourceMsg();
            smsg.setUnlockKey(Platform.getUuid());
            smsg.setReason("test");
            bus.makeServiceIdByManagementNodeId(smsg, dmsg.toLockServiceId, dmsg.toManagementNodeUuid);
            bus.send(smsg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    MessageReply r = new MessageReply();
                    bus.reply(msg, r);
                }
            });
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
