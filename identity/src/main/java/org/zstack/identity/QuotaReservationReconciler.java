package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class QuotaReservationReconciler implements ManagementNodeReadyExtensionPoint {
    @Autowired
    private ThreadFacade thdf;

    private Future<Void> cleanupTask;

    @Override
    public void managementNodeReady() {
        startCleanupTask();
    }

    private synchronized void startCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel(true);
        }

        cleanupTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return IdentityGlobalConfig.QUOTA_RESERVATION_CLEANUP_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "clean-up-quota-reservations";
            }

            @Override
            @ExceptionSafe
            public void run() {
                new QuotaUtil().releaseExpiredQuotaReservations();
            }
        });
    }
}
