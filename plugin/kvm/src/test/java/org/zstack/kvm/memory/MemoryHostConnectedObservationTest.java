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
        int policyAdmissions;
        MemoryAgentResponse lastObservation;

        @Override public void observe(String hostUuid, MemoryAgentResponse response) {
            observations++; lastObservation = response;
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
