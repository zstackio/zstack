package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 */
public class BackupStoragePingTracker extends PingTracker implements ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    private final static CLogger logger = Utils.getLogger(BackupStoragePingTracker.class);

    private final Map<String, BackupStorageStatus> statusMap = Collections.synchronizedMap(new HashMap<String, BackupStorageStatus>());

    private Map<String, AtomicInteger> backupStorageDisconnectCount = new ConcurrentHashMap<>();

    private BackupStorageReconnectTask reconnectTask;

    @Autowired
    private ResourceDestinationMaker destMaker;

    @Autowired
    protected EventFacade evtf;

    @Override
    public String getResourceName() {
        return "backup storage";
    }

    @Autowired
    private PluginRegistry pluginRgty;

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
        decideWhatToDoNext(resourceUuid, makeReconnectDecision(resourceUuid, reply));
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

    enum ReconnectDecision {
        DoNothing,
        SubmitReconnectTask,
        StopReconnectTask
    }

    @Override
    public boolean start() {
        onBackupStorageStatusChange();
        return super.start();
    }

    private void onBackupStorageStatusChange() {
        evtf.onLocal(BackupStorageCanonicalEvents.BACKUP_STORAGE_STATUS_CHANGED, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                BackupStorageCanonicalEvents.BackupStorageStatusChangedData d = (BackupStorageCanonicalEvents.BackupStorageStatusChangedData) data;
                if (BackupStorageStatus.Connected.toString().equals(d.getNewStatus())) {
                    backupStorageDisconnectCount.remove(d.getBackupStorageUuid());
                } else if (BackupStorageStatus.Disconnected.toString().equals(d.getNewStatus()) &&
                        BackupStorageStatus.Connecting.toString().equals(d.getOldStatus())) {
                    backupStorageDisconnectCount.computeIfAbsent(d.getBackupStorageUuid(), key -> new AtomicInteger(0)).addAndGet(1);
                }
            }
        });
    }

    private void decideWhatToDoNext(String resUuid, ReconnectDecision decision) {
        if (decision == ReconnectDecision.SubmitReconnectTask) {
            logger.debug("xxx SubmitReconnectTask");
            submitReconnectTask(resUuid);
        } else if (decision == ReconnectDecision.StopReconnectTask) {
            logger.debug("xxx StopReconnect");
            cancel(resUuid);
        } else if (decision == ReconnectDecision.DoNothing) {
            logger.debug("xxx DoNothing");
        } else {
            throw new CloudRuntimeException("should not be here");
        }
    }

    private ReconnectDecision makeReconnectDecision(String uuid, MessageReply reply) {
        if (!reply.isSuccess()) {
            logger.warn(String.format("[Backup storage Tracker]: unable track backup storage[uuid:%s], %s", uuid, reply.getError()));
            return ReconnectDecision.DoNothing;
        }

        //reply is success
        AtomicInteger disconnectCount = backupStorageDisconnectCount.get(uuid);
        int threshold = BackupStorageGlobalConfig.AUTO_RECONNECT_ON_ERROR_MAX_ATTEMPT_NUM.value(Integer.class);
        logger.debug(String.format("xxx disconnectCount is  %s ,threshold is %s", disconnectCount, threshold));
        if (threshold > 0 && disconnectCount != null && disconnectCount.get() >= threshold) {
            logger.warn(String.format("[Backup storage Tracker]: stop pinging backup storage[uuid: %s] because it fail to reconnect too many times.", uuid));
            return ReconnectDecision.StopReconnectTask;
        }

        // bs can be successfully pinged
        boolean autoReconnect = BackupStorageGlobalConfig.AUTO_RECONNECT_ON_ERROR.value(Boolean.class);
        boolean isConnected = Q.New(BackupStorageVO.class)
                .eq(BackupStorageVO_.uuid, uuid)
                .eq(BackupStorageVO_.status, BackupStorageStatus.Connected).isExists();

        if (!isConnected) {
            if (autoReconnect) {
                logger.debug("xxx return SubmitReconnectTask");
                return ReconnectDecision.SubmitReconnectTask;
            } else {
                logger.warn(String.format("xxx [Backup storage Tracker]: stop pinging host[uuid: %s] because it's disconnected and connection.autoReconnectOnError is false", uuid));
                return ReconnectDecision.StopReconnectTask;
            }
        }

        // host can be pinged and the current status is Connected
        return ReconnectDecision.DoNothing;
    }

    private void submitReconnectTask(String uuid) {
        if (reconnectTask != null) {
            reconnectTask.cancel();
        }

        reconnectTask = new BackupStorageReconnectTask(uuid, new NoErrorCompletion(){
            @Override
            public void done()
            {
                logger.debug("xxx Successfully create reconnect backup storage task.");
            }
        });

        reconnectTask.start();
    }

    public void cancel(String resUuid) {
        if (reconnectTask != null) {
            reconnectTask.cancel();
        }
        statusMap.remove(resUuid);
    }

}
