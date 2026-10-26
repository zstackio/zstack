package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class MemoryHostConnectedObservationTest {
    private static class Repository extends MemoryRepository {
        int observations;
        int partialKsmObservations;
        int policyAdmissions;
        MemoryAgentResponse lastObservation;
        MemoryPolicyConfig servicePolicy;

        @Override MemoryPolicyConfig serviceObservationPolicy(String host) { return servicePolicy; }

        @Override public void observe(String hostUuid, MemoryAgentResponse response) {
            observations++; lastObservation = response;
        }

        @Override public void observePartialKsm(String hostUuid, MemoryAgentResponse response) {
            partialKsmObservations++; lastObservation = response;
        }

        @Override public boolean applyEffectivePolicyAfterConnect(String hostUuid) {
            policyAdmissions++; return true;
        }
    }

    private static class Hooks extends MemoryLifecycleHooks {
        int reconciliations;
        @Override void reconcileObservedBindings(String host, MemoryAgentResponse response) { reconciliations++; }
    }

    private static class Manager extends MemoryOptimizationManager {
        int agentCalls;
        Consumer<MemoryAgentResponse> pending;
        final List<Task<?>> submitted = new ArrayList<>();

        @Override void call(String host, String action, Map<String, Object> command,
                            Consumer<MemoryAgentResponse> completion) {
            assertEquals("host-a", host);
            assertEquals("state", action);
            agentCalls++;
            pending = completion;
        }

        void runConnectionTask(int index) throws Exception { submitted.get(index).call(); }
        void runScan(String host) { observeHostAfterConnect(host); }
        void complete(MemoryAgentResponse response) { Consumer<MemoryAgentResponse> callback = pending; pending = null; callback.accept(response); }
    }

    @Test public void connectionCallbackOnlyEnqueuesAndEventScanRaceSharesOneFreshObservation() throws Exception {
        Manager manager = configuredManager();
        HostInventory host = connectedHost();

        manager.afterHostConnected(host);
        assertEquals("connection flow must not call the Agent", 0, manager.agentCalls);
        assertEquals(1, manager.submitted.size());

        manager.runConnectionTask(0);
        assertEquals(1, manager.agentCalls);
        manager.runScan("host-a");
        manager.afterHostConnected(host);
        assertEquals("event plus periodic scan/repeated event coalesce while the read is outstanding", 1, manager.agentCalls);
        assertEquals(2, manager.submitted.size());

        MemoryAgentResponse fresh = successfulObservation(System.currentTimeMillis());
        manager.complete(fresh);
        Repository repository = field(manager, "repository", Repository.class);
        Hooks hooks = field(manager, "lifecycleHooks", Hooks.class);
        assertEquals(1, repository.observations);
        assertSame(fresh, repository.lastObservation);
        assertEquals("fresh read reaches the existing guarded policy coordinator", 1, repository.policyAdmissions);
        assertEquals(1, hooks.reconciliations);

        // A queued duplicate that runs after completion may take another read; durable plan/hash
        // admission in MemoryRepository, not this in-memory coalescer, is the cross-MN idempotency fence.
        manager.runConnectionTask(1);
        assertEquals(2, manager.agentCalls);
        manager.complete(successfulObservation(System.currentTimeMillis()));
        assertEquals(2, repository.observations);
        assertEquals(2, repository.policyAdmissions);
    }

    @Test public void connectingHostAndFailedOrStaleObservationsNeverReachPolicyCoordinator() throws Exception {
        Manager manager = configuredManager();
        HostInventory connecting = connectedHost(); connecting.setStatus("Connecting");
        manager.afterHostConnected(connecting);
        assertTrue(manager.submitted.isEmpty());

        HostInventory otherHypervisor = connectedHost(); otherHypervisor.setHypervisorType("Other");
        manager.afterHostConnected(otherHypervisor);
        assertTrue("KVM plugin must not issue KVM Agent calls for other hypervisors", manager.submitted.isEmpty());

        manager.afterHostConnected(connectedHost());
        manager.runConnectionTask(0);
        manager.complete(successfulObservation(System.currentTimeMillis() - MemoryOptimizationGlobalConfig.displayTtlMillis() - 10));
        Repository repository = field(manager, "repository", Repository.class);
        assertEquals(0, repository.observations);
        assertEquals(0, repository.policyAdmissions);

        manager.afterHostConnected(connectedHost());
        manager.runConnectionTask(1);
        MemoryAgentResponse failed = successfulObservation(System.currentTimeMillis());
        failed.setSuccess(false); failed.status = "Unknown";
        manager.complete(failed);
        assertEquals(0, repository.observations);
        assertEquals(0, repository.policyAdmissions);
    }

    @Test public void onlyRepeatedObservationFailuresBackOffTheHost() throws Exception {
        Manager manager = configuredManager();
        Map<?, ?> backoff = field(manager, "observationBackoff", Map.class);
        MemoryAgentResponse failed = successfulObservation(System.currentTimeMillis());
        failed.setSuccess(false); failed.status = "Unknown";
        for (int i = 0; i < 2; i++) {
            manager.observeHostAfterConnect("host-a"); manager.complete(failed);
            assertFalse("a transient failure is retried on the next scan", backoff.containsKey("host-a"));
        }
        manager.observeHostAfterConnect("host-a"); manager.complete(successfulObservation(System.currentTimeMillis()));
        for (int i = 0; i < 2; i++) {
            manager.observeHostAfterConnect("host-a"); manager.complete(failed);
            assertFalse("a success resets the failure count", backoff.containsKey("host-a"));
        }
        manager.observeHostAfterConnect("host-a"); manager.complete(failed);
        assertTrue("repeated failures slow the host down", backoff.containsKey("host-a"));
    }

    @Test public void coldNativeKsmObservationIsStoredSeparatelyWithoutControlReconciliation() throws Exception {
        Manager manager = configuredManager();
        manager.afterHostConnected(connectedHost());
        manager.runConnectionTask(0);
        // Use the platform JSON utility, as CloudBus does. A plain Gson
        // instance decodes generic Map numbers as Double, while ZStack's
        // configured LONG_OR_DOUBLE strategy preserves integral KSM values
        // as Long and matches the actual Agent wire contract.
        MemoryAgentResponse partial = org.zstack.utils.gson.JSONObjectUtil.toObject(
                "{\"success\":false,\"status\":\"Unknown\",\"observationKind\":\"KSM_NATIVE_PARTIAL\","
                        + "\"reasonCode\":\"LOCAL_SERVICE_UNAVAILABLE\",\"bootId\":\"boot-a\","
                        + "\"coldKsmAdmissionReady\":true,\"coldKsmAdmissionReason\":\"LOCAL_CONTROL_FENCE_CLEAR\","
                        + "\"sampleTime\":" + System.currentTimeMillis()
                        + ",\"capabilities\":{\"supported\":true,\"ksm\":true},"
                        + "\"state\":{\"bootId\":\"boot-a\",\"actual\":{\"ksm\":{\"enabled\":true,"
                        + "\"pagesToScan\":1000,\"sleepMillis\":10}}}}",
                MemoryAgentResponse.class);
        assertTrue(partial.state.get("actual") instanceof Map);
        Map<?, ?> actual = (Map<?, ?>) partial.state.get("actual");
        Map<?, ?> nativeKsm = (Map<?, ?>) actual.get("ksm");
        assertTrue("platform decoder must preserve integral Agent metrics", nativeKsm.get("pagesToScan") instanceof Long);
        assertTrue(MemoryObservationRules.isKsmNativePartial(partial, "host-a", System.currentTimeMillis()));
        // KVMHostAsyncHttpCallReply rehashes the CloudBus map, which carries Double values.
        Map<String, Object> rehashedKsm = new java.util.LinkedHashMap<>();
        rehashedKsm.put("enabled", true); rehashedKsm.put("pagesToScan", 1000.0); rehashedKsm.put("sleepMillis", 10.0);
        partial.state.put("actual", java.util.Collections.singletonMap("ksm", rehashedKsm));
        assertTrue(MemoryObservationRules.isKsmNativePartial(partial, "host-a", System.currentTimeMillis()));
        rehashedKsm.put("pagesToScan", 1000.5);
        assertFalse(MemoryObservationRules.isKsmNativePartial(partial, "host-a", System.currentTimeMillis()));
        rehashedKsm.put("pagesToScan", 1000.0);
        manager.complete(partial);

        Repository repository = field(manager, "repository", Repository.class);
        Hooks hooks = field(manager, "lifecycleHooks", Hooks.class);
        assertFalse("a KSM-only reply before ZRAM activation is not a failed observation",
                field(manager, "observationBackoff", Map.class).containsKey("host-a"));
        assertEquals("partial native data must not overwrite full observation state", 0, repository.observations);
        assertEquals(1, repository.partialKsmObservations);
        assertEquals("partial capability is not proof that desired control applied", 0, repository.policyAdmissions);
        assertEquals(0, hooks.reconciliations);
        assertSame(partial, repository.lastObservation);
    }

    @Test public void memoryManagerIsRegisteredForHostAfterConnectedExtension() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("conf/springConfigXml/MemoryOptimization.xml"))) {
            root = root.getParent();
        }
        assertNotNull("repository root containing memory Spring config", root);
        String xml = new String(Files.readAllBytes(root.resolve("conf/springConfigXml/MemoryOptimization.xml")),
                StandardCharsets.UTF_8);
        assertTrue(xml.contains("id=\"MemoryOptimizationManager\""));
        assertTrue(xml.contains("interface=\"org.zstack.header.host.HostAfterConnectedExtensionPoint\""));
    }

    @Test public void explicitNativeFailureUsesExistingObservationWithoutAdmittingUnknownControl() throws Exception {
        Manager manager = configuredManager();
        Repository repository = field(manager, "repository", Repository.class);
        repository.servicePolicy = MemoryPolicyRules.decode("{\"ksm\":{\"enabled\":true}}");
        List<org.zstack.header.host.HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData> events = new ArrayList<>();
        org.zstack.core.cloudbus.EventFacade facade = (org.zstack.core.cloudbus.EventFacade) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{org.zstack.core.cloudbus.EventFacade.class}, (proxy, method, args) -> {
                    if ("fire".equals(method.getName())) {
                        assertEquals(org.zstack.header.host.HostCanonicalEvents.HOST_MEMORY_OPTIMIZATION_SERVICE_STATE_CHANGED_PATH, args[0]);
                        events.add((org.zstack.header.host.HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData) args[1]);
                    }
                    return null;
                });
        inject(manager, "evtf", facade);
        MemoryAgentResponse failed = org.zstack.utils.gson.JSONObjectUtil.toObject(
                "{\"success\":false,\"status\":\"Unknown\",\"reasonCode\":\"LOCAL_SERVICE_UNAVAILABLE\","
                        + "\"sampleTime\":" + System.currentTimeMillis()
                        + ",\"state\":{\"bootId\":\"boot-a\",\"actual\":{\"ksm\":{\"enabled\":null,"
                        + "\"serviceState\":\"failed\",\"tunedServiceState\":\"activating\"}}}}", MemoryAgentResponse.class);
        manager.runScan("host-a"); manager.complete(failed);
        assertEquals(1, events.size());
        assertEquals("ksm", events.get(0).getService());
        assertEquals("Failed", events.get(0).getStatus());
        assertEquals(0, repository.observations);
        assertEquals(0, repository.partialKsmObservations);
        assertEquals(0, repository.policyAdmissions);
        // A pause/disconnect/unknown fence re-read at callback time suppresses service events.
        repository.servicePolicy = null;
        manager.runScan("host-a"); manager.complete(failed);
        assertEquals(1, events.size());
        assertEquals(2, manager.agentCalls);
    }

    @Test public void eventDeliveryFailureDoesNotBlockExistingObservationAndPolicyReconciliation() throws Exception {
        Manager manager = configuredManager();
        Repository repository = field(manager, "repository", Repository.class);
        repository.servicePolicy = MemoryPolicyRules.decode("{\"ksm\":{\"enabled\":true}}");
        org.zstack.core.cloudbus.EventFacade broken = (org.zstack.core.cloudbus.EventFacade) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{org.zstack.core.cloudbus.EventFacade.class},
                (proxy, method, args) -> { throw new IllegalStateException("event delivery unavailable"); });
        inject(manager, "evtf", broken);
        MemoryAgentResponse response = successfulObservation(System.currentTimeMillis());
        response.state = new LinkedHashMap<>(response.state);
        response.state.put("actual", Collections.singletonMap("ksm", Collections.singletonMap("serviceState", "failed")));
        manager.runScan("host-a"); manager.complete(response);
        assertEquals(1, repository.observations);
        assertEquals(1, repository.policyAdmissions);
        manager.runScan("host-a");
        assertEquals("failed notification must release the observation slot", 2, manager.agentCalls);
    }

    private static Manager configuredManager() throws Exception {
        Manager manager = new Manager();
        Repository repository = new Repository(); Hooks hooks = new Hooks();
        ThreadFacade threadFacade = (ThreadFacade) Proxy.newProxyInstance(ThreadFacade.class.getClassLoader(),
                new Class<?>[]{ThreadFacade.class}, (proxy, method, args) -> {
                    if ("submit".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Task) {
                        manager.submitted.add((Task<?>) args[0]); return null;
                    }
                    if (method.getReturnType() == boolean.class) { return false; }
                    if (method.getReturnType() == int.class) { return 0; }
                    if (method.getReturnType() == long.class) { return 0L; }
                    return null;
                });
        PluginRegistry registry = (PluginRegistry) Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (proxy, method, args) -> {
                    if ("getExtensionList".equals(method.getName()) && args != null
                            && args[0] == MemoryLicenseExtensionPoint.class) {
                        return Collections.singletonList((MemoryLicenseExtensionPoint) () -> System.currentTimeMillis() + 60_000L);
                    }
                    return Collections.emptyList();
                });
        inject(manager, "thdf", threadFacade); inject(manager, "pluginRgty", registry);
        inject(manager, "repository", repository); inject(manager, "lifecycleHooks", hooks);
        return manager;
    }

    private static HostInventory connectedHost() {
        HostInventory host = new HostInventory(); host.setUuid("host-a"); host.setStatus("Connected");
        host.setHypervisorType("KVM"); return host;
    }

    private static MemoryAgentResponse successfulObservation(long sampleTime) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.status = "Succeeded"; response.sampleTime = sampleTime;
        response.state = Collections.<String, Object>singletonMap("bootId", "boot-a");
        response.setSuccess(true); return response;
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    private static <T> T field(Object target, String name, Class<T> type) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name); field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
