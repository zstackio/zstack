package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class BackupStoragePingTracker extends PingTracker implements ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private final static CLogger logger = Utils.getLogger(BackupStoragePingTracker.class);

    private final Map<String, BackupStorageStatus> statusMap = Collections.synchronizedMap(new HashMap<String, BackupStorageStatus>());

    @Autowired
    private ResourceDestinationMaker destMaker;

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
        // nothing to do
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

    private void reScanBackupStorage() {
        untrackAll();
        statusMap.clear();

        new SQLBatch() {
            @Override
            protected void scripts() {
                long count = sql("select count(bs) from BackupStorageVO bs", Long.class).find();
                sql("select bs.uuid from BackupStorageVO bs", String.class).limit(1000).paginate(count, (List<String> bsUuids) -> {
                    List<String> byUs = bsUuids.stream()
                            .filter(bsUuid ->
                            destMaker.isManagedByUs(bsUuid))
                            .collect(Collectors.toList());

                    track(byUs);
                });
            }
        }.execute();
    }

    @Override
    public void managementNodeReady() {
        reScanBackupStorage();
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        reScanBackupStorage();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reScanBackupStorage();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }
}
