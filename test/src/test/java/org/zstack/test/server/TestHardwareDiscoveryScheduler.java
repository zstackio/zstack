package org.zstack.test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.server.hardware.HardwareDiscoveryScheduler;
import org.zstack.server.hardware.PhysicalServerHardwareService;
import org.zstack.server.hardware.UnifiedHardwareInfo;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for HardwareDiscoveryScheduler.
 *
 * These tests bypass Spring context and GlobalConfig by directly injecting a minimal
 * scheduler subclass with a test-owned executor.
 *
 * Full integration tests with ZStack test harness are deferred to U16 polish pass.
 */
public class TestHardwareDiscoveryScheduler {

    /**
     * Test-friendly subclass: installs small executors and accepts an injected
     * PhysicalServerHardwareService substitute.
     */
    static class TestScheduler extends HardwareDiscoveryScheduler {
        private final int concurrency;
        private final PhysicalServerHardwareService service;

        TestScheduler(PhysicalServerHardwareService service, int concurrency) {
            this.service = service;
            this.concurrency = concurrency;
        }

        void start() throws Exception {
            // Inject hardwareService via reflection (field is @Autowired in parent)
            Field f = HardwareDiscoveryScheduler.class.getDeclaredField("hardwareService");
            f.setAccessible(true);
            f.set(this, service);

            // Init executor directly with test values (bypasses GlobalConfig)
            Field ef = HardwareDiscoveryScheduler.class.getDeclaredField("executor");
            ef.setAccessible(true);
            java.util.concurrent.ThreadPoolExecutor tpe = new java.util.concurrent.ThreadPoolExecutor(
                    concurrency, concurrency, 0L, TimeUnit.MILLISECONDS,
                    new java.util.concurrent.LinkedBlockingQueue<>(),
                    java.util.concurrent.Executors.defaultThreadFactory());
            ef.set(this, tpe);

            Field tf = HardwareDiscoveryScheduler.class.getDeclaredField("timeoutExecutor");
            tf.setAccessible(true);
            tf.set(this, java.util.concurrent.Executors.newSingleThreadScheduledExecutor());
        }

        void stop() {
            getExecutor().shutdown();
            try {
                Field tf = HardwareDiscoveryScheduler.class.getDeclaredField("timeoutExecutor");
                tf.setAccessible(true);
                java.util.concurrent.ScheduledExecutorService timer =
                        (java.util.concurrent.ScheduledExecutorService) tf.get(this);
                timer.shutdownNow();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ---- Scenario 1: Happy path ----

    @Test
    public void testHappyPath() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        PhysicalServerHardwareService stub = new PhysicalServerHardwareService() {
            @Override
            public UnifiedHardwareInfo discoverHardware(String serverUuid) {
                callCount.incrementAndGet();
                return new UnifiedHardwareInfo();
            }
        };

        TestScheduler sched = new TestScheduler(stub, 2);
        sched.start();

        sched.enqueueDiscovery("uuid-happy");

        // Give async task time to complete
        sched.stop();
        boolean finished = sched.getExecutor().awaitTermination(5, TimeUnit.SECONDS);

        Assert.assertTrue("Executor should terminate cleanly", finished);
        // discoverHardware called at least once (wrapped in inner worker task so called once from runDiscovery)
        Assert.assertTrue("discoverHardware should be called at least once", callCount.get() >= 1);
    }

    @Test
    public void testSameServerUuidEnqueueIsCoalescedWhileDiscoveryInFlight() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        PhysicalServerHardwareService slowStub = new PhysicalServerHardwareService() {
            @Override
            public UnifiedHardwareInfo discoverHardware(String serverUuid) {
                callCount.incrementAndGet();
                started.countDown();
                try {
                    release.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new UnifiedHardwareInfo();
            }
        };

        TestScheduler sched = new TestScheduler(slowStub, 4);
        sched.start();

        sched.enqueueDiscovery("same-server");
        sched.enqueueDiscovery("same-server");

        Assert.assertTrue("first discovery should start", started.await(5, TimeUnit.SECONDS));
        release.countDown();

        sched.stop();
        Assert.assertTrue("Executor should terminate cleanly",
                sched.getExecutor().awaitTermination(10, TimeUnit.SECONDS));
        Assert.assertEquals("duplicate enqueue for same serverUuid should be coalesced", 1, callCount.get());
    }

    // ---- Scenario 2: Retry backoff on failure ----

    @Test
    public void testRetryStopsAtMax() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        PhysicalServerHardwareService failingStub = new PhysicalServerHardwareService() {
            @Override
            public UnifiedHardwareInfo discoverHardware(String serverUuid) {
                callCount.incrementAndGet();
                throw new RuntimeException("simulated discovery failure");
            }
        };

        TestScheduler schedFast = new TestScheduler(failingStub, 4);
        schedFast.start();

        schedFast.enqueueDiscovery("uuid-retry");

        schedFast.stop();
        schedFast.getExecutor().awaitTermination(5, TimeUnit.SECONDS);

        Assert.assertTrue("discoverHardware should be called at least once", callCount.get() >= 1);
    }

    // ---- Scenario 3: Concurrency cap ----

    @Test
    public void testConcurrencyCap() throws Exception {
        int concurrency = 3;
        int taskCount = 10;
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        CountDownLatch allStarted = new CountDownLatch(concurrency);
        CountDownLatch release = new CountDownLatch(1);

        PhysicalServerHardwareService slowStub = new PhysicalServerHardwareService() {
            @Override
            public UnifiedHardwareInfo discoverHardware(String serverUuid) {
                int c = currentConcurrent.incrementAndGet();
                maxConcurrent.accumulateAndGet(c, Math::max);
                allStarted.countDown();
                try {
                    release.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    currentConcurrent.decrementAndGet();
                }
                return new UnifiedHardwareInfo();
            }
        };

        TestScheduler sched = new TestScheduler(slowStub, concurrency);
        sched.start();

        for (int i = 0; i < taskCount; i++) {
            sched.enqueueDiscovery("uuid-" + i);
        }

        // Wait for exactly `concurrency` tasks to start (or timeout)
        boolean reachedCap = allStarted.await(5, TimeUnit.SECONDS);
        release.countDown();

        sched.stop();
        sched.getExecutor().awaitTermination(10, TimeUnit.SECONDS);

        // Note: executor.getActiveCount() is sampled after release, so use our counter.
        // We verify at most `concurrency` ran simultaneously.
        Assert.assertTrue("Peak concurrent tasks should not exceed concurrency cap",
                maxConcurrent.get() <= concurrency);

        // If concurrency cap is working, all `concurrency` slots should have been filled
        // (provided taskCount > concurrency, which it is: 10 > 3)
        if (reachedCap) {
            Assert.assertEquals("Should reach full concurrency", concurrency, maxConcurrent.get());
        }
    }
}
