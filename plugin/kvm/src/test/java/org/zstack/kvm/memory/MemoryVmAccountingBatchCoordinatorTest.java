package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Exercise the real coordinator and asynchronous completion path, not a host-list slice. */
public class MemoryVmAccountingBatchCoordinatorTest {
    private static class Repository extends MemoryRepository {
        final Map<String, String> placements = new LinkedHashMap<>();
        @Override public Map<String, String> currentVmHosts(Collection<String> requested) {
            Map<String, String> result = new LinkedHashMap<>();
            for (String vm : requested) if (placements.containsKey(vm)) result.put(vm, placements.get(vm));
            return result;
        }
        @Override public List<String> targets(String scope, String uuid) {
            assertEquals("VM", scope);
            String host = placements.get(uuid);
            return host == null ? Collections.emptyList() : Collections.singletonList(host);
        }
    }

    private static class Fixture {
        final MemoryOptimizationManager manager = new MemoryOptimizationManager();
        final Repository repository = new Repository();
        final List<Task<?>> calls = new ArrayList<>();
        final AtomicInteger replies = new AtomicInteger();
        final AtomicReference<APIGetVmMemoryOptimizationsReply> response = new AtomicReference<>();
        final AtomicReference<APIGetVmMemoryOptimizationReply> singleResponse = new AtomicReference<>();
        final List<String> requested = new ArrayList<>();
        Runnable deadline;

        Fixture(int hosts, int vmsPerHost, boolean noHostVm) throws Exception {
            this(hosts, vmsPerHost, noHostVm, true);
        }

        Fixture(int hosts, int vmsPerHost, boolean noHostVm, boolean startBatch) throws Exception {
            for (int h = 0; h < hosts; h++) {
                for (int v = 0; v < vmsPerHost; v++) {
                    String vm = "vm-" + h + "-" + v;
                    requested.add(vm); repository.placements.put(vm, "host-" + h);
                }
            }
            if (noHostVm) requested.add("vm-no-host");
            inject(manager, "repository", repository);
            inject(manager, "bus", Proxy.newProxyInstance(CloudBus.class.getClassLoader(), new Class<?>[]{CloudBus.class},
                    (proxy, method, args) -> {
                        if ("reply".equals(method.getName())) {
                            replies.incrementAndGet();
                            if (args[1] instanceof APIGetVmMemoryOptimizationReply) {
                                singleResponse.set((APIGetVmMemoryOptimizationReply) args[1]);
                            } else { response.set((APIGetVmMemoryOptimizationsReply) args[1]); }
                        } else if ("replyErrorByMessageType".equals(method.getName())) {
                            throw new AssertionError("unexpected observation rejection: " + args[1]);
                        }
                        return null;
                    }));
            inject(manager, "thdf", Proxy.newProxyInstance(ThreadFacade.class.getClassLoader(), new Class<?>[]{ThreadFacade.class},
                    (proxy, method, args) -> {
                        if ("submitTimeoutTask".equals(method.getName())) {
                            assertEquals(TimeUnit.SECONDS, args[1]); assertEquals(60L, args[2]);
                            deadline = (Runnable) args[0];
                        } else if ("submit".equals(method.getName())) { calls.add((Task<?>) args[0]); }
                        return null;
                    }));
            MemoryMetricsExtensionPoint metrics = new MemoryMetricsExtensionPoint() {
                @Override public Map<String, Map<String, Object>> hostSavings(List<String> hosts, long now, long ttl) {
                    throw new AssertionError("not a Host savings query");
                }
                @Override public Map<String, Object> vmAccounting(String host, List<String> vms, long now, long ttl) {
                    Map<String, Object> samples = new LinkedHashMap<>();
                    for (String vm : vms) {
                        Map<String, Object> sample = new LinkedHashMap<>();
                        sample.put("quality", "fresh"); sample.put("metrics", Collections.singletonMap("ram_payload_bytes", 123L));
                        samples.put(vm, sample);
                    }
                    Map<String, Object> report = new LinkedHashMap<>();
                    report.put("observed_at", Instant.ofEpochMilli(now).toString()); report.put("vms", samples);
                    return report;
                }
            };
            inject(manager, "pluginRgty", Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(), new Class<?>[]{PluginRegistry.class},
                    (proxy, method, args) -> "getExtensionList".equals(method.getName())
                            ? Collections.singletonList(metrics) : null));
            if (startBatch) {
                APIGetVmMemoryOptimizationsMsg msg = new APIGetVmMemoryOptimizationsMsg(); msg.setVmUuids(requested);
                Method start = MemoryOptimizationManager.class.getDeclaredMethod("vmAccountings", APIGetVmMemoryOptimizationsMsg.class);
                start.setAccessible(true); start.invoke(manager, msg);
            }
        }

        void complete(int index) throws Exception { calls.get(index).call(); }
        MemoryVmAccountingInventory entry(String vm) { return response.get().getInventories().get(vm); }
        void assertCompleteReply() {
            assertEquals(1, replies.get()); assertNotNull(response.get());
            assertEquals(new LinkedHashSet<>(requested), response.get().getInventories().keySet());
        }
        void assertTimeout(String vm) {
            assertNotNull("missing requested VM " + vm, entry(vm));
            assertEquals("unavailable", entry(vm).getQuality());
            assertEquals("BATCH_DEADLINE_EXCEEDED", entry(vm).getReason());
            assertNull(entry(vm).getMetrics());
        }
    }

    @Test public void singleInflightHostTimesOutWithoutDroppingAnyOfItsVms() throws Exception {
        Fixture f = new Fixture(1, 2, false);
        assertEquals(1, f.calls.size()); f.deadline.run(); f.assertCompleteReply();
        for (String vm : f.requested) f.assertTimeout(vm);
    }

    @Test public void deadlineIncludesInflightAndQueuedHostsAndPreservesExistingResults() throws Exception {
        Fixture f = new Fixture(7, 2, true);
        assertEquals(4, f.calls.size()); f.complete(0); assertEquals(5, f.calls.size());
        f.deadline.run(); f.assertCompleteReply();
        assertEquals("fresh", f.entry("vm-0-0").getQuality());
        assertEquals("VM_HAS_NO_CURRENT_HOST", f.entry("vm-no-host").getReason());
        for (int h = 1; h < 7; h++) for (int v = 0; v < 2; v++) f.assertTimeout("vm-" + h + "-" + v);
        assertEquals("deadline must not schedule queued hosts", 5, f.calls.size());
    }

    @Test public void multipleRoundsStillFillEveryUnfinishedVmAtDeadline() throws Exception {
        Fixture f = new Fixture(10, 1, false);
        for (int i = 0; i < 6; i++) f.complete(i);
        assertEquals(10, f.calls.size()); f.deadline.run(); f.assertCompleteReply();
        for (int i = 0; i < 6; i++) assertEquals("fresh", f.entry("vm-" + i + "-0").getQuality());
        for (int i = 6; i < 10; i++) f.assertTimeout("vm-" + i + "-0");
    }

    @Test public void lateCompletionDoesNotMutateAlreadyReturnedResultOrReplyAgain() throws Exception {
        Fixture f = new Fixture(5, 1, false); f.deadline.run(); f.assertCompleteReply();
        Map<String, Object> before = new LinkedHashMap<>(f.response.get().getInventories());
        for (int i = 0; i < 4; i++) f.complete(i);
        f.deadline.run(); f.assertCompleteReply();
        assertEquals(before, f.response.get().getInventories()); assertEquals(4, f.calls.size());
    }

    @Test public void completionBeforeDeadlineKeepsSuccessfulResultAndOneReply() throws Exception {
        Fixture f = new Fixture(6, 1, false);
        for (int i = 0; i < 6; i++) f.complete(i);
        f.assertCompleteReply(); f.deadline.run(); f.assertCompleteReply();
        for (String vm : f.requested) assertEquals("fresh", f.entry(vm).getQuality());
    }

    @Test public void deadlineCompletionRaceReturnsEveryVmExactlyOnce() throws Exception {
        for (int repeat = 0; repeat < 20; repeat++) {
            Fixture f = new Fixture(1, 1, false);
            CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread timeout = contender(ready, go, failure, f.deadline);
            Thread completed = contender(ready, go, failure, () -> {
                try { f.complete(0); } catch (Exception e) { throw new RuntimeException(e); }
            });
            timeout.start(); completed.start();
            assertTrue(ready.await(5, TimeUnit.SECONDS)); go.countDown();
            timeout.join(5000); completed.join(5000);
            assertFalse(timeout.isAlive()); assertFalse(completed.isAlive());
            if (failure.get() != null) throw new AssertionError(failure.get());
            f.assertCompleteReply();
            Object quality = f.entry("vm-0-0").getQuality();
            assertTrue("fresh".equals(quality) || "unavailable".equals(quality));
        }
    }

    @Test public void singleVmPrometheusCallbackDoesNotRequireAgentEnvelope() throws Exception {
        Fixture f = new Fixture(1, 1, false, false);
        APIGetVmMemoryOptimizationMsg msg = new APIGetVmMemoryOptimizationMsg();
        msg.setVmUuid("vm-0-0");
        Method query = MemoryOptimizationManager.class.getDeclaredMethod("vmAccounting", APIGetVmMemoryOptimizationMsg.class);
        query.setAccessible(true); query.invoke(f.manager, msg);
        assertEquals(1, f.calls.size());
        f.complete(0); // Executes the real callVmMetrics task and its real single-VM callback.
        assertEquals(1, f.replies.get());
        assertNotNull(f.singleResponse.get());
        MemoryVmAccountingInventory inventory = f.singleResponse.get().getInventory();
        assertEquals("host-0", inventory.getHostUuid());
        assertEquals("vm-0-0", inventory.getVmUuid());
        assertEquals("fresh", inventory.getQuality());
        assertEquals(Long.valueOf(123L), inventory.getMetrics().getRam_payload_bytes());
    }

    private static Thread contender(CountDownLatch ready, CountDownLatch go, AtomicReference<Throwable> failure, Runnable work) {
        return new Thread(() -> {
            ready.countDown();
            try { if (!go.await(5, TimeUnit.SECONDS)) throw new AssertionError("start timeout"); work.run(); }
            catch (Throwable e) { failure.compareAndSet(null, e); }
        }, "memory-batch-deadline-test");
    }
    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }
}
