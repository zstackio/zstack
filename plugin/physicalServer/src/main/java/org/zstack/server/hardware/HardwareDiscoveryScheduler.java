package org.zstack.server.hardware;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.server.PhysicalServerGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory scheduler for hardware discovery requests.
 * Uses a bounded ThreadPoolExecutor (size = DISCOVERY_CONCURRENCY global config, default 8)
 * with exponential backoff retry (up to DISCOVERY_RETRY_MAX, default 3).
 *
 * <p>Retry backoff: implemented via Thread.sleep inside the worker task (simpler than a
 * side ScheduledExecutorService for the low-volume retry use case).</p>
 *
 * <p>Timeout: each discoverHardware() call records its worker thread and is interrupted after
 * DISCOVERY_TIMEOUT_SEC seconds (default 60); timeout counts as a failure for retry.</p>
 */
public class HardwareDiscoveryScheduler {
    private static final CLogger logger = Utils.getLogger(HardwareDiscoveryScheduler.class);

    @Autowired
    private PhysicalServerHardwareService hardwareService;

    private ThreadPoolExecutor executor;
    private ScheduledExecutorService timeoutExecutor;
    private final ConcurrentHashMap<String, Integer> retryCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap.KeySetView<String, Boolean> inFlightServers = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        Integer cfg = PhysicalServerGlobalConfig.DISCOVERY_CONCURRENCY.value(Integer.class);
        int core = cfg != null ? cfg : 8;
        // LinkedBlockingQueue is the work queue; tasks submitted via executor.submit() are placed here
        // when all core threads are busy. No separate 'queue' field needed — accessible via
        // executor.getQueue() when tests need to inspect queue depth.
        executor = new ThreadPoolExecutor(
                core, core,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                // ZStack does not ship NamedThreadFactory; use plain defaultThreadFactory.
                // TODO(polish): introduce a NamedThreadFactory("hw-discovery-") utility in utils/
                Executors.defaultThreadFactory());
        timeoutExecutor = Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());
        logger.debug(String.format("HardwareDiscoveryScheduler started with concurrency=%d", core));
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdown();
        }
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdown();
        }
    }

    /**
     * Enqueues a hardware discovery request for the given server UUID.
     * Returns immediately; discovery runs asynchronously on the thread pool.
     */
    public void enqueueDiscovery(String serverUuid) {
        if (serverUuid == null || serverUuid.isEmpty()) {
            return;
        }
        if (!inFlightServers.add(serverUuid)) {
            logger.debug(String.format("Hardware discovery for server[uuid:%s] is already in flight, coalescing enqueue", serverUuid));
            return;
        }
        try {
            submitDiscovery(serverUuid);
        } catch (RuntimeException e) {
            inFlightServers.remove(serverUuid);
            throw e;
        }
    }

    // ---- private ----

    private void submitDiscovery(String serverUuid) {
        Integer toCfg = PhysicalServerGlobalConfig.DISCOVERY_TIMEOUT_SEC.value(Integer.class);
        int timeoutSec = toCfg != null ? toCfg : 60;
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicReference<Thread> workerRef = new AtomicReference<>();
        executor.execute(() -> {
            workerRef.set(Thread.currentThread());
            try {
                hardwareService.discoverHardware(serverUuid);
                if (finished.compareAndSet(false, true)) {
                    retryCount.remove(serverUuid);
                    inFlightServers.remove(serverUuid);
                    logger.debug(String.format("Hardware discovery succeeded for server[uuid:%s]", serverUuid));
                }
            } catch (Exception e) {
                if (finished.compareAndSet(false, true)) {
                    logger.warn(String.format(
                            "Hardware discovery failed for server[uuid:%s]: %s", serverUuid, e.getMessage()));
                    scheduleRetry(serverUuid);
                }
            } finally {
                workerRef.set(null);
            }
        });

        timeoutExecutor.schedule(() -> {
            if (finished.compareAndSet(false, true)) {
                Thread worker = workerRef.get();
                if (worker != null) {
                    worker.interrupt();
                }
                logger.warn(String.format(
                        "Hardware discovery timed out after %ds for server[uuid:%s]", timeoutSec, serverUuid));
                scheduleRetry(serverUuid);
            }
        }, timeoutSec, TimeUnit.SECONDS);
    }

    private void scheduleRetry(String serverUuid) {
        Integer rmCfg = PhysicalServerGlobalConfig.DISCOVERY_RETRY_MAX.value(Integer.class);
        int retryMax = rmCfg != null ? rmCfg : 3;
        int attempts = retryCount.merge(serverUuid, 1, Integer::sum);
        if (attempts >= retryMax) {
            logger.error(String.format(
                    "Hardware discovery for server[uuid:%s] failed after %d attempts; giving up",
                    serverUuid, attempts));
            retryCount.remove(serverUuid);
            inFlightServers.remove(serverUuid);
            return;
        }

        // Exponential backoff: 30 * 2^(attempts-1) seconds
        long delaySec = 30L * (1L << (attempts - 1));
        logger.warn(String.format(
                "Scheduling retry #%d for server[uuid:%s] in %ds", attempts, serverUuid, delaySec));

        try {
            timeoutExecutor.schedule(() -> submitDiscovery(serverUuid), delaySec, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            inFlightServers.remove(serverUuid);
            throw e;
        }
    }

    // Exposed for testing
    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    public ScheduledExecutorService getTimeoutExecutor() {
        return timeoutExecutor;
    }
}
