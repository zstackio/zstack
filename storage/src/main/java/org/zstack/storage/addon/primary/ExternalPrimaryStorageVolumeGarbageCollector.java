package org.zstack.storage.addon.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.storage.primary.ImageCacheGarbageCollector;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ExternalPrimaryStorageVolumeGarbageCollector implements Component, ManagementNodeChangeListener, PeriodicTask {
    private static CLogger logger = Utils.getLogger(ImageCacheGarbageCollector.class);

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private int garbageCollectionInterval;
    private Future<Void> garbageCollectionThread;

    @Override
    public boolean start() {
        garbageCollectionInterval = PrimaryStorageGlobalConfig.ACTIVE_VOLUME_GARBAGE_COLLECTOR_INTERVAL.value(Integer.class);
        PrimaryStorageGlobalConfig.ACTIVE_VOLUME_GARBAGE_COLLECTOR_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                garbageCollectionInterval = newConfig.value(Integer.class);
                startGarbageCollectionThread();
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private synchronized void startGarbageCollectionThread() {
        if (garbageCollectionThread != null) {
            garbageCollectionThread.cancel(true);
        }

        garbageCollectionThread = thdf.submitPeriodicTask(this);
        logger.debug(String.format("Image cache garbage collector starts running by interval[%ss]", garbageCollectionInterval));
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        startGarbageCollectionThread();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {}

    @Override
    public void iAmDead(ManagementNodeInventory inv) {}

    @Override
    public void iJoin(ManagementNodeInventory inv) {}


    @Override
    public void run() {
        try {

        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public long getInterval() {
        return garbageCollectionInterval;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
