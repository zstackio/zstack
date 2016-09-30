package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PingPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

/**
 */
public class PrimaryStoragePingTracker extends PingTracker {
    @Autowired
    private CloudBus bus;

    @Override
    public String getResourceName() {
        return "primary storage";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(resUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, resUuid);
        return msg;
    }

    @Override
    public int getPingInterval() {
        return PrimaryStorageGlobalConfig.PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return PrimaryStorageGlobalConfig.PING_PARALLELISM_DEGREE.value(Integer.class);
    }

    @Override
    public void handleReply(String resourceUuid, MessageReply reply) {
        // nothing to do
    }

    @Override
    protected void startHook() {
        PrimaryStorageGlobalConfig.PING_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                pingIntervalChanged();
            }
        });
    }
}
