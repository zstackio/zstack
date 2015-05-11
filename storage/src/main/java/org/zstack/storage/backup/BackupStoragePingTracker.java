package org.zstack.storage.backup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class BackupStoragePingTracker extends PingTracker {
    private final static CLogger logger = Utils.getLogger(BackupStoragePingTracker.class);

    private final Map<String, BackupStorageStatus> statusMap = Collections.synchronizedMap(new HashMap<String, BackupStorageStatus>());

    @Override
    public String getResourceName() {
        return "backup storage";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        PingBackupStorageMsg msg = new PingBackupStorageMsg();
        msg.setBackupStorageUuid(resUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, resUuid);
        return msg;
    }

    @Override
    public int getPingInterval() {
        return BackupStorageGlobalConfig.PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return BackupStorageGlobalConfig.PING_PARALLELISM_DEGREE.value(Integer.class);
    }

    @Override
    public void handleReply(String resourceUuid, MessageReply reply) {
        if (!reply.isSuccess()) {
            logger.warn(String.format("[Backup Storage Tracker]: unable track backup storage[uuid:%s], %s", resourceUuid, reply.getError()));
            return;
        }

        PingBackupStorageReply preply = (PingBackupStorageReply) reply;
        BackupStorageStatus status = preply.isAvailable() ? BackupStorageStatus.Connected : BackupStorageStatus.Disconnected;
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[Backup Storage Tracker]: discovered backup storage[uuid:%s] status[%s]", resourceUuid, status));
        }

        BackupStorageStatus old = statusMap.get(resourceUuid);
        if (old == status) {
            return;
        }

        statusMap.put(resourceUuid, status);
        ChangeBackupStorageStatusMsg cmsg = new ChangeBackupStorageStatusMsg();
        cmsg.setBackupStorageUuid(resourceUuid);
        cmsg.setStatus(status.toString());
        bus.makeTargetServiceIdByResourceUuid(cmsg, BackupStorageConstant.SERVICE_ID, resourceUuid);
        bus.send(cmsg);
    }

    @Override
    protected void untrackHook(String resUuid) {
        statusMap.remove(resUuid);
    }

    @Override
    protected void startHook() {
        BackupStorageGlobalConfig.PING_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                pingIntervalChanged();
            }
        });
    }
}
