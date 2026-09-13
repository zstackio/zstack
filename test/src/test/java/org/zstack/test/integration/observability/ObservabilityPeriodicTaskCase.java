package org.zstack.test.integration.observability;

import org.zstack.core.thread.PeriodicTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ObservabilityPeriodicTaskCase implements PeriodicTask {
    private final String name;
    private final CountDownLatch latch;

    public ObservabilityPeriodicTaskCase(String name, CountDownLatch latch) {
        this.name = name;
        this.latch = latch;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Override
    public long getInterval() {
        return 10;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void run() {
        latch.countDown();
    }
}
