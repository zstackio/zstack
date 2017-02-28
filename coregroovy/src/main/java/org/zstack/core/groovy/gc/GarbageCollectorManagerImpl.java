package org.zstack.core.groovy.gc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GCGlobalConfig;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2017/3/1.
 */
public class GarbageCollectorManagerImpl implements GarbageCollectorManager, Component, ManagementNodeReadyExtensionPoint {
    static final CLogger logger = Utils.getLogger(GarbageCollectorManagerImpl.class);

    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ThreadFacade thdf;

    private Future<Void> scanOrphanJobsTask;


    private void startScanOrphanJobs() {
        if (scanOrphanJobsTask != null) {
            scanOrphanJobsTask.cancel(true);
        }

        scanOrphanJobsTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "scan-orphan-gc-jobs";
            }

            @Override
            public void run() {
                try {
                    loadOrphanJobs();
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }
        });

        logger.debug(String.format("[GC] starts scanning orphan job thread with the interval[%ss]", GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.value(Integer.class)));
    }

    @Override
    public boolean start() {
        startScanOrphanJobs();

        GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startScanOrphanJobs();
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void loadOrphanJobs() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        List<GarbageCollectorVO> vos = Q.New(GarbageCollectorVO.class)
                .isNull(GarbageCollectorVO_.managementNodeUuid).list();

        int count = 0;

        for (GarbageCollectorVO vo : vos) {
            if (!destinationMaker.isManagedByUs(String.format("%s", vo.getId()))) {
                continue;
            }

            Class clz = Class.forName(vo.getRunnerClass());
            if (vo.getType().equals(GarbageCollectorType.EventBased.toString())) {
                EventBasedGarbageCollector gc = (EventBasedGarbageCollector) clz.newInstance();
                gc.load(vo);
            } else if (vo.getType().equals(GarbageCollectorType.TimeBased.toString())) {
                TimeBasedGarbageCollector gc = (TimeBasedGarbageCollector) clz.newInstance();
                gc.load(vo);
            }

            count ++;
        }

        logger.debug(String.format("[GC] loaded %s orphan jobs", count));
    }

    @Override
    public void managementNodeReady() {
        try {
            loadOrphanJobs();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }
}
