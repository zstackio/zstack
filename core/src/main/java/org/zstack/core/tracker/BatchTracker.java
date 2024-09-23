package org.zstack.core.tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 */
public abstract class BatchTracker implements Component {
    public abstract String getResourceName();

    public abstract long getTaskInterval();
    public abstract void executeTask(List<String> resourceUuids);
    public abstract void rescan();

    private final static CLogger logger = Utils.getLogger(PingTracker.class);

    private final List<String> resourceUuids = Collections.synchronizedList(new ArrayList<>());
    private Future<Void> trackerThread = null;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;

    private class Tracker implements PeriodicTask {
        @Override
        public TimeUnit getTimeUnit() {
            return TimeUnit.SECONDS;
        }

        @Override
        public long getInterval() {
            return getTaskInterval();
        }

        @Override
        public String getName() {
            return String.format("pingTracker-for-%s-managementNode-%s", getResourceName(), Platform.getManagementServerId());
        }

        @Override
        public void run() {
            try {
                synchronized (resourceUuids) {
                    executeTask(resourceUuids);
                }
            } catch (Throwable t) {
                logger.warn("unhandled throwable", t);
            }
        }
    }

    protected void trackHook(String resourceUuid) {
    }

    protected void untrackHook(String resourceUuid) {
    }

    protected void startHook() {
    }

    protected void pingIntervalChanged() {
        startTracker();
    }

    public void track(String resUuid) {
        synchronized (resourceUuids) {
            if (!resourceUuids.contains(resUuid)) {
                resourceUuids.add(resUuid);
                trackHook(resUuid);
                logger.debug(String.format("start tracking %s[uuid:%s]", getResourceName(), resUuid));
            }
        }
    }

    public void untrackIf(Predicate<String> predicate) {
        synchronized (resourceUuids) {
            resourceUuids.removeIf(predicate);
        }
    }

    public void untrackAll() {
        synchronized (resourceUuids) {
            resourceUuids.clear();
            logger.debug(String.format("untrack all %s", getResourceName()));
        }
    }

    public void untrack(String resUuid) {
        synchronized (resourceUuids) {
            resourceUuids.remove(resUuid);
            untrackHook(resUuid);
            logger.debug(String.format("stop tracking %s[uuid:%s]", getResourceName(), resUuid));
        }
    }

    public void track(Collection<String> resUuids) {
        synchronized (resourceUuids) {
            for (String resUuid : resUuids) {
                if (!resourceUuids.contains(resUuid)) {
                    resourceUuids.add(resUuid);
                    trackHook(resUuid);
                    logger.debug(String.format("start tracking %s[uuid:%s]", getResourceName(), resUuid));
                }
            }
        }
    }

    public void untrack(Collection<String> resUuids) {
        synchronized (resourceUuids) {
            for (String resUuid : resUuids) {
                resourceUuids.remove(resUuid);
                untrackHook(resUuid);
                logger.debug(String.format("stop tracking %s[uuid:%s]", getResourceName(), resUuid));
            }
        }
    }

    protected synchronized void startTracker() {
        if (trackerThread != null) {
            trackerThread.cancel(true);
        }

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            trackerThread = thdf.submitPeriodicTask(new Tracker(), getTaskInterval());
        } else {
            trackerThread = thdf.submitPeriodicTask(new Tracker(), (long) getTaskInterval() + new Random().nextInt(30));
        }
    }

    @Override
    public boolean start() {
        startTracker();
        startHook();
        return true;
    }

    @Override
    public boolean stop() {
        trackerThread.cancel(true);
        return true;
    }
}
