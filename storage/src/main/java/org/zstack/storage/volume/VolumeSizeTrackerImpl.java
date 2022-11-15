package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.NopeWhileDoneCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VolumeSizeTrackerImpl implements VolumeSizeTracker, Component {
    private final static CLogger logger = Utils.getLogger(VolumeSizeTrackerImpl.class);

    private final Set<String> volumeUuids = Collections.synchronizedSet(new HashSet<>());
    private Set<String> volumeInTracking = Collections.synchronizedSet(new HashSet<>());
    private Future<Void> trackerThread = null;

    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;


    @Override
    public void trackVolume(String volUuid) {
        volumeUuids.add(volUuid);
    }

    @Override
    public void untrackVolume(String volUuid) {
        volumeUuids.remove(volUuid);
    }

    @Override
    public void trackVolume(Collection<String> volUuids) {
        volumeUuids.addAll(volUuids);
    }

    @Override
    public void untrackVolume(Collection<String> volUuids) {
        volumeUuids.removeAll(volUuids);
    }

    @Override
    public void reScanVolume() {
        volumeUuids.clear();
        volumeUuids.addAll(getNeedRefreshSizeVolumeUuids().stream()
                .filter(it -> destMaker.isManagedByUs(it))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean start() {
        setupTracker();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void setupTracker() {
        VolumeGlobalConfig.REFRESH_VOLUME_SIZE_INTERVAL.installUpdateExtension(this::startIntervalTracker);
        VolumeGlobalConfig.AUTO_REFRESH_ALL_ACTIVE_VOLUME.installUpdateExtension(this::startRefreshTypeTracker);
        startIntervalTracker(null, VolumeGlobalConfig.REFRESH_VOLUME_SIZE_INTERVAL);
    }

    private synchronized void startIntervalTracker(GlobalConfig oldConfig, GlobalConfig newConfig) {
        final long oldValue = oldConfig == null ? 0L : oldConfig.value(Long.class);
        final long newValue = newConfig.value(Long.class);

        if (oldValue == newValue) {
            return;
        }

        boolean refreshAll = VolumeGlobalConfig.AUTO_REFRESH_ALL_ACTIVE_VOLUME.value(Boolean.class);
        submitTrackerTask(refreshAll, newValue);
    }

    private synchronized void startRefreshTypeTracker(GlobalConfig oldConfig, GlobalConfig newConfig) {
        final boolean oldValue = oldConfig.value(Boolean.class);
        final boolean newValue = newConfig.value(Boolean.class);

        if (oldValue == newValue) {
            return;
        }

        long interval = VolumeGlobalConfig.REFRESH_VOLUME_SIZE_INTERVAL.value(Long.class);
        submitTrackerTask(newValue, interval);
    }

    private void submitTrackerTask(boolean refreshAll, long interval) {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }

        trackerThread = thdf.submitPeriodicTask(new PeriodicTask() {

            @Override
            @ExceptionSafe
            public void run() {
                if (refreshAll) {
                    logger.info("begin to sync all online volumes");
                    List<String> clusterUuids = Q.New(ClusterVO.class).select(ClusterVO_.uuid).listValues();
                    new While<>(clusterUuids).each((clusterUuid, completion) -> {
                        LocalBatchSyncVolumeSizeMsg msg = new LocalBatchSyncVolumeSizeMsg();
                        msg.setClusterUuid(clusterUuid);
                        bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
                        bus.send(msg);
                    }).run(new NopeWhileDoneCompletion());
                } else {
                    logger.info("begin to sync all monitored volumes");
                    reScanVolume();
                    syncVolumeSize();
                }
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "VolumeSizeTracker";
            }

        });
    }

    private void syncVolumeSize(){
        new While<>(volumeUuids).step((volUuid, completion) -> {
            if (!volumeInTracking.add(volUuid)) {
                completion.done();
                return;
            }

            SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
            msg.setVolumeUuid(volUuid);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volUuid);
            bus.send(msg, new CloudBusCallBack(msg) {
                @Override
                public void run(MessageReply reply) {
                    volumeInTracking.remove(msg.getVolumeUuid());
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("fail to refresh volume[uuid:%s] size, try again soon", volUuid));
                    }
                    completion.done();
                }
            });
        }, 50).run(new NopeWhileDoneCompletion());
    }

    private List<String> getNeedRefreshSizeVolumeUuids(){
        Set<String> volUuids = new HashSet<>();
        for (RefreshVolumeSizeExtensionPoint ext : pluginRgty.getExtensionList(RefreshVolumeSizeExtensionPoint.class)) {
            volUuids.addAll(ext.getNeedRefreshVolumeSizeVolume());
        }
        return volUuids.isEmpty() ? new ArrayList<>() : Q.New(VolumeVO.class).in(VolumeVO_.uuid, volUuids)
                .eq(VolumeVO_.state, VolumeState.Enabled)
                .eq(VolumeVO_.status, VolumeStatus.Ready)
                .select(VolumeVO_.uuid)
                .listValues();
    }
}
