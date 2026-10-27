package org.zstack.kvm.memory;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmInstanceInventory;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Exercises the migration callback chain with the same request payloads used by the Agent adapter. */
public class MemoryLifecycleHooksCallbackTest {
    // Lifecycle failures now use the platform's declared memory error metadata.
    // Reuse the real metadata/i18n fixture, not a hand-built ErrorCode or the
    // incidental static state left by another test class.
    @BeforeClass public static void initializePlatformErrors() throws Exception {
        MemoryOperationExceptionI18nTest.initializePlatformErrorServices();
    }
    @AfterClass public static void restorePlatformErrors() throws Exception {
        MemoryOperationExceptionI18nTest.restorePlatformStatics();
    }
    private Object encryptAspect;
    private Field registryField;
    private Object previousRegistry;
    @Before public void initializeWovenPersistenceAdvice() throws Exception {
        Class<?> aspect = Class.forName("org.zstack.core.aspect.EncryptColumnAspect");
        encryptAspect = aspect.getMethod("aspectOf").invoke(null);
        registryField = aspect.getDeclaredField("pluginRegistry"); registryField.setAccessible(true);
        previousRegistry = registryField.get(encryptAspect);
        registryField.set(encryptAspect, Proxy.newProxyInstance(PluginRegistry.class.getClassLoader(),
                new Class<?>[]{PluginRegistry.class}, (proxy, method, args) -> Collections.emptyList()));
        java.lang.reflect.Method init = org.zstack.core.db.EntityMetadata.class.getDeclaredMethod("staticInit");
        init.setAccessible(true); init.invoke(null);
    }
    @After public void restoreWovenPersistenceAdvice() throws Exception {
        if (registryField != null) registryField.set(encryptAspect, previousRegistry);
    }
    @Test
    public void migratedVmUsesTargetHostAndDifferentTargetGeneration() throws Exception {
        FakeRepository repository = new FakeRepository();
        MemoryMigrationVO hold = new MemoryMigrationVO();
        hold.vmUuid = "vm-1"; hold.operationUuid = "op-1"; hold.sourceHostUuid = "host-a";
        hold.targetHostUuid = "host-b"; hold.poolGeneration = "pool-1";
        hold.sourceInstanceGeneration = "instance-source"; hold.status = "Held";
        repository.hold = hold;

        FakeManager manager = new FakeManager();
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository);
        inject(hooks, "manager", manager);

        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setUuid("vm-1");
        AtomicDone done = new AtomicDone();
        hooks.afterMigrateVm(vm, "host-a", done);

        assertTrue(done.done);
        assertEquals("host-b", manager.bindCommand.get("hostUuid"));
        assertEquals("instance-target", manager.bindCommand.get("expectedInstanceGeneration"));
        assertEquals("host-a", manager.revokeCommand.get("hostUuid"));
        assertEquals("Released", repository.hold.status);
    }

    private static void inject(Object target, String name, Object value) throws Exception {
        Field field = MemoryLifecycleHooks.class.getDeclaredField(name);
        field.setAccessible(true); field.set(target, value);
    }

    @Test public void unavailableParticipationPolicyFailsClosed() throws Exception {
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", new FakeRepository() {
            @Override String vmPolicy(String vm) {
                throw new IllegalStateException("database unavailable");
            }
        });
        assertEquals("deny", hooks.vmParticipation("vm-1"));
    }

    @Test public void expiredLicenseDoesNotPreventConfirmedMigrationRebind() throws Exception {
        FakeRepository repository = heldMigration();
        FakeManager manager = new FakeManager();
        migrate(repository, manager);
        assertEquals("inherit", manager.bindCommand.get("participation"));
        assertFalse(manager.bindCommand.containsKey("licensePermit"));
        assertEquals("Released", repository.hold.status);
    }

    @Test public void confirmedUnmanagedDestinationDoesNotNeedOptimizerBinding() throws Exception {
        FakeRepository repository = heldMigration();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("host-b".equals(host) && "state".equals(action)) {
                    MemoryAgentResponse answer = response(host, "instance-target");
                    answer.capabilities = Collections.singletonMap("service", false);
                    answer.state.put("managed", false);
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        migrate(repository, manager);
        assertNull(manager.bindCommand);
        assertNotNull(manager.revokeCommand);
        assertEquals("Released", repository.hold.status);
    }

    @Test public void completeKsmOnlyDestinationSkipsZramBindingWithoutNativeGeneration() throws Exception {
        FakeRepository repository = heldMigration();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("host-b".equals(host) && "state".equals(action)) {
                    MemoryAgentResponse answer = response(host, null);
                    answer.state.put("managed", true);
                    answer.state.put("vms", Collections.singletonMap("vm-1", Collections.emptyMap()));
                    answer.capabilities = new HashMap<>(); answer.capabilities.put("service", true);
                    answer.capabilities.put("zram", false);
                    answer.capabilities.put("zramReasonCode", "UNSUPPORTED_RECLAIM_KERNEL");
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        migrate(repository, manager);
        assertNull(manager.bindCommand);
        assertNotNull(manager.revokeCommand);
        assertEquals("Released", repository.hold.status);
    }

    @Test public void completeZramServiceWithoutGenerationRemainsUnknown() throws Exception {
        FakeRepository repository = heldMigration();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("host-b".equals(host) && "state".equals(action)) {
                    MemoryAgentResponse answer = response(host, null);
                    answer.state.put("managed", true);
                    answer.state.put("vms", Collections.singletonMap("vm-1", Collections.emptyMap()));
                    answer.capabilities = new HashMap<>(); answer.capabilities.put("service", true);
                    answer.capabilities.put("zram", false);
                    answer.capabilities.put("zramReasonCode", "SERVICE_RECLAIM_CAPABILITY_UNKNOWN");
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        migrate(repository, manager);
        assertNull(manager.bindCommand);
        assertNull(manager.revokeCommand);
        assertEquals("Unknown", repository.hold.status);
    }

    @Test public void incompleteDestinationInventoryCannotReleaseSourceHold() throws Exception {
        FakeRepository repository = heldMigration();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("host-b".equals(host) && "state".equals(action)) {
                    MemoryAgentResponse answer = response(host, "instance-target");
                    answer.state.put("inventoryComplete", false);
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        migrate(repository, manager);
        assertNull(manager.bindCommand);
        assertNull(manager.revokeCommand);
        assertEquals("Unknown", repository.hold.status);
    }

    @Test public void preflightFailureBeforeAcquireCanReconcileOnSourceWithoutNativeRelease() throws Exception {
        FakeRepository repository = new FakeRepository() {
            @Override public List<String> targets(String scope, String resource) {
                return Collections.singletonList("host-a");
            }
            @Override public void abortMigrationParticipation(String vm) { }
        };
        MemoryMigrationVO hold = new MemoryMigrationVO();
        hold.vmUuid = "vm-1"; hold.operationUuid = "op-1"; hold.sourceHostUuid = "host-a";
        hold.targetHostUuid = "host-b"; hold.status = "Unknown";
        hold.reason = "Source pool identity cannot be confirmed";
        repository.hold = hold;
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("state".equals(action)) {
                    callback.accept(response(host, "host-a".equals(host) ? "instance-source" : null));
                } else {
                    fail("No native release/revoke is allowed before acquire: " + action);
                }
            }
        };
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        final boolean[] success = {false};
        hooks.reconcileMigrationHold("vm-1", new org.zstack.header.core.Completion(new org.zstack.header.core.AsyncBackup() {}) {
            @Override public void success() { success[0] = true; }
            @Override public void fail(org.zstack.header.errorcode.ErrorCode errorCode) { org.junit.Assert.fail(errorCode.getDetails()); }
        });
        assertTrue(success[0]);
        assertEquals("Released", repository.hold.status);
    }

    @Test public void acquiredUnknownHoldDoesNotUsePreflightShortcut() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown";
        repository.hold.reason = "Migration hold result must be reconciled";
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("state".equals(action)) {
                    MemoryAgentResponse answer = response(host, "host-b".equals(host) ? "instance-target" : null);
                    answer.state.put("inventoryComplete", false);
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        final boolean[] success = {false};
        final org.zstack.header.errorcode.ErrorCode[] failure = {null};
        hooks.reconcileMigrationHold("vm-1", new org.zstack.header.core.Completion(new org.zstack.header.core.AsyncBackup() {}) {
            @Override public void success() { success[0] = true; }
            @Override public void fail(org.zstack.header.errorcode.ErrorCode errorCode) { failure[0] = errorCode; }
        });
        assertFalse(success[0]);
        assertNotNull("unresolved identities must return a failure callback", failure[0]);
        assertEquals("MEMORY_RESULT_UNKNOWN", failure[0].getCode());
        assertEquals("ORG_ZSTACK_MEMORY_" + MemoryErrors.fromBusinessCode("MEMORY_RESULT_UNKNOWN").getId(),
                failure[0].getGlobalErrorCode());
        assertEquals("Unknown", repository.hold.status);
    }

    @Test public void observedSourceRetriesAnUnknownHoldUntilItIsProven() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown";
        List<String> inspected = new ArrayList<>();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("state".equals(action)) {
                    inspected.add(host);
                    MemoryAgentResponse answer = response(host, "host-b".equals(host) ? "instance-target" : null);
                    answer.state.put("inventoryComplete", false);
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        hooks.reconcileUnknownMigrationHolds("host-b");
        assertTrue("only the hold's source observation reconciles it", inspected.isEmpty());
        hooks.reconcileUnknownMigrationHolds("host-a");
        assertFalse("the Unknown hold is inspected after its source is observed", inspected.isEmpty());
        assertEquals("unresolved identities keep the hold Unknown", "Unknown", repository.hold.status);
        int afterFirst = inspected.size();
        hooks.reconcileUnknownMigrationHolds("host-a");
        assertEquals("an unproven hold is tried again at the next observation", 2 * afterFirst, inspected.size());
    }

    /** Agent replies by Host: the VM instance each Host reports, and the bind, revoke and hold calls made. */
    private static class PlacementManager extends FakeManager {
        final Map<String, String> instances = new HashMap<>();
        final Set<String> listedWithoutGeneration = new HashSet<>();
        final List<String> calls = new ArrayList<>();
        @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
            if ("state".equals(action)) {
                MemoryAgentResponse answer = response(host, instances.get(host));
                if (listedWithoutGeneration.contains(host)) {
                    ((Map<String, Object>) answer.state.get("vms")).put("vm-1", new HashMap<>());
                }
                callback.accept(answer);
                return;
            }
            calls.add(host + ":" + ("migration-hold".equals(action) ? command.get("holdAction") : command.get("action")));
            super.call(host, action, command, callback);
        }
    }

    private static boolean recover(MemoryLifecycleHooks hooks, String host, String[] failure) {
        final boolean[] success = {false};
        hooks.recoverMigrationHold("vm-1", host, new org.zstack.header.core.Completion(new org.zstack.header.core.AsyncBackup() {}) {
            @Override public void success() { success[0] = true; }
            @Override public void fail(org.zstack.header.errorcode.ErrorCode errorCode) { failure[0] = errorCode.getDetails(); }
        });
        return success[0];
    }

    @Test public void operatorRecoversAHoldWhoseVmRestartedOnTheSource() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown";
        PlacementManager manager = new PlacementManager();
        manager.instances.put("host-a", "instance-restarted");
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        String[] failure = {null};
        assertTrue(failure[0], recover(hooks, "host-a", failure));
        assertEquals(Arrays.asList("host-a:bind", "host-a:release"), manager.calls);
        assertEquals("the restarted source instance is bound", "instance-restarted", manager.bindCommand.get("expectedInstanceGeneration"));
        assertEquals("Released", repository.hold.status);
    }

    @Test public void operatorRecoversAMigratedVmAndAStoppedVm() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown"; repository.vmHost = "host-b";
        PlacementManager manager = new PlacementManager();
        manager.instances.put("host-b", "instance-target");
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        String[] failure = {null};
        assertTrue(failure[0], recover(hooks, "host-b", failure));
        assertEquals(Arrays.asList("host-b:bind", "host-a:revoke", "host-a:release"), manager.calls);
        assertEquals("Released", repository.hold.status);

        FakeRepository stopped = heldMigration();
        stopped.hold.status = "Unknown"; stopped.vmState = "Stopped"; stopped.vmHost = null;
        PlacementManager none = new PlacementManager();
        inject(hooks, "repository", stopped); inject(hooks, "manager", none);
        assertTrue(failure[0], recover(hooks, null, failure));
        assertEquals("a stopped VM only needs the source hold released", Collections.singletonList("host-a:release"), none.calls);
        assertEquals("Released", stopped.hold.status);
    }

    @Test public void operatorRecoveryTreatsADeletedHostAsNotHavingTheVm() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown"; repository.deletedHosts.add("host-b");
        PlacementManager manager = new PlacementManager();
        manager.instances.put("host-a", "instance-source");
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        String[] failure = {null};
        assertTrue(failure[0], recover(hooks, "host-a", failure));
        assertEquals("Released", repository.hold.status);
    }

    @Test public void operatorRecoveryNeedsTheGenerationOfTheInstanceItBinds() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown";
        PlacementManager manager = new PlacementManager();
        manager.listedWithoutGeneration.add("host-a");
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        String[] failure = {null};
        assertFalse(recover(hooks, "host-a", failure));
        assertEquals("the recorded generation is kept", "instance-source", repository.hold.sourceInstanceGeneration);
        assertTrue("nothing is bound without a generation", manager.calls.isEmpty());
    }

    @Test public void operatorRecoveryChangesNothingUnlessThePlatformAndBothHostsAgree() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown";
        PlacementManager manager = new PlacementManager();
        manager.instances.put("host-a", "instance-source"); manager.instances.put("host-b", "instance-target");
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        String[] failure = {null};
        assertFalse("both Hosts still have the VM", recover(hooks, "host-a", failure));
        assertTrue(failure[0], failure[0].contains("target has it"));
        assertFalse("the platform places the VM on the source", recover(hooks, "host-b", failure));
        assertFalse("a running VM needs its Host", recover(hooks, null, failure));
        assertTrue("nothing is bound, revoked or released", manager.calls.isEmpty());
        assertEquals("Unknown", repository.hold.status);

        repository.hold.status = "Released";
        assertFalse(recover(hooks, "host-a", failure));
        assertTrue(failure[0], failure[0].contains("no unresolved memory migration hold"));
    }

    @Test public void preMigrateRequiresCompleteSourceInventoryBeforeAcquire() throws Exception {
        FakeRepository repository = new FakeRepository() {
            @Override public void captureMigrationParticipation(String vm, String source, String target) { }
        };
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("state".equals(action)) {
                    MemoryAgentResponse answer = response(host, "instance-source");
                    answer.state.put("poolGeneration", "pool-1");
                    answer.state.put("inventoryComplete", false);
                    callback.accept(answer);
                    return;
                }
                fail("incomplete source inventory must not acquire a native hold: " + action);
            }
        };
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        VmInstanceInventory vm = new VmInstanceInventory(); vm.setUuid("vm-1"); vm.setHostUuid("host-a");
        final boolean[] failed = {false};
        hooks.preMigrateVm(vm, "host-b", new org.zstack.header.core.Completion(new org.zstack.header.core.AsyncBackup() {}) {
            @Override public void success() { org.junit.Assert.fail("incomplete inventory must not pass pre-migration"); }
            @Override public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                assertEquals("MEMORY_RESULT_UNKNOWN", errorCode.getCode());
                assertEquals("ORG_ZSTACK_MEMORY_" + MemoryErrors.fromBusinessCode("MEMORY_RESULT_UNKNOWN").getId(),
                        errorCode.getGlobalErrorCode());
                failed[0] = true;
            }
        });
        assertTrue(failed[0]);
        assertEquals("Unknown", repository.hold.status);
    }

    private static FakeRepository heldMigration() {
        FakeRepository repository = new FakeRepository();
        MemoryMigrationVO hold = new MemoryMigrationVO();
        hold.vmUuid = "vm-1"; hold.operationUuid = "op-1"; hold.sourceHostUuid = "host-a";
        hold.targetHostUuid = "host-b"; hold.poolGeneration = "pool-1";
        hold.sourceInstanceGeneration = "instance-source"; hold.status = "Held";
        repository.hold = hold;
        return repository;
    }

    private static void migrate(FakeRepository repository, FakeManager manager) throws Exception {
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        VmInstanceInventory vm = new VmInstanceInventory(); vm.setUuid("vm-1");
        AtomicDone done = new AtomicDone();
        hooks.afterMigrateVm(vm, "host-a", done);
        assertTrue(done.done);
    }

    @Test public void existingVmBindingsRespectStoredDenyAndSkipAlreadyBoundInstances() throws Exception {
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        FakeManager manager = new FakeManager();
        inject(hooks, "manager", manager);
        inject(hooks, "repository", new FakeRepository() {
            @Override public List<MemoryStateVO> states(List<String> hosts, int start, int limit) {
                MemoryStateVO state = new MemoryStateVO();
                state.setStatus("Succeeded"); state.setDesiredRevision(1L); state.setAppliedRevision(1L);
                return Collections.singletonList(state);
            }
            @Override String vmPolicy(String vm) { return "{\"participation\":\"deny\"}"; }
        });
        MemoryAgentResponse response = new MemoryAgentResponse();
        Map<String, Object> vm = new HashMap<>(); vm.put("instanceGeneration", "new-instance");
        Map<String, Object> state = new HashMap<>(); state.put("inventoryComplete", true);
        state.put("vms", Collections.singletonMap("vm-1", vm)); response.state = state;
        hooks.reconcileObservedBindings("host-a", response);
        assertEquals("deny", manager.bindCommand.get("participation"));
        assertEquals("new-instance", manager.bindCommand.get("expectedInstanceGeneration"));
        manager.bindCommand = null;
        vm.put("bindingGeneration", "new-instance"); vm.put("bindingMode", "deny");
        hooks.reconcileObservedBindings("host-a", response);
        assertNull(manager.bindCommand);
        vm.remove("bindingGeneration"); state.put("inventoryComplete", false);
        hooks.reconcileObservedBindings("host-a", response);
        assertNull(manager.bindCommand);
    }

    private static MemoryAgentResponse response(String host, String generation) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = "op-1"; response.status = "Succeeded";
        Map<String, Object> vms = new HashMap<>();
        if (generation != null) { vms.put("vm-1", Collections.singletonMap("instanceGeneration", generation)); }
        Map<String, Object> state = new HashMap<>();
        state.put("vms", vms); state.put("inventoryComplete", true);
        response.state = state;
        return response;
    }

    private static class FakeManager extends MemoryOptimizationManager {
        Map<String, Object> bindCommand;
        Map<String, Object> revokeCommand;
        @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
            if ("state".equals(action)) {
                callback.accept(response(host, "host-b".equals(host) ? "instance-target" : null));
            } else if ("vm-efficiency".equals(action)) {
                if ("bind".equals(command.get("action"))) { bindCommand = new HashMap<>(command); }
                if ("revoke".equals(command.get("action"))) { revokeCommand = new HashMap<>(command); }
                callback.accept(response(host, null));
            } else if ("migration-hold".equals(action)) {
                callback.accept(response(host, null));
            } else { callback.accept(null); }
        }
    }

    private static class FakeRepository extends MemoryRepository {
        @Override public String vmParticipation(String uuid) {
            String value = MemoryPolicyRules.decode(vmPolicy(uuid)).participation;
            return value == null ? "inherit" : value;
        }
        MemoryMigrationVO hold;
        private final EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, (proxy, method, args) -> {
                    if ("find".equals(method.getName()) && args != null && args.length >= 2
                            && args[0] == MemoryMigrationVO.class) { return hold; }
                    if ("merge".equals(method.getName())) { hold = (MemoryMigrationVO) args[0]; return hold; }
                    if ("createQuery".equals(method.getName())) { return query((String) args[0]); }
                    if ("flush".equals(method.getName())) { return null; }
                    if (method.getReturnType().isPrimitive()) {
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                    }
                    return null;
                });
        String vmState = "Running";
        String vmHost = "host-a";
        final Set<String> deletedHosts = new HashSet<>();
        /** Answers the VM placement, Host existence and source Unknown hold queries from the fixtures. */
        private javax.persistence.TypedQuery<?> query(String jpql) {
            Map<String, Object> parameters = new HashMap<>();
            return (javax.persistence.TypedQuery<?>) Proxy.newProxyInstance(javax.persistence.TypedQuery.class.getClassLoader(),
                    new Class[]{javax.persistence.TypedQuery.class}, (proxy, method, args) -> {
                        if ("setParameter".equals(method.getName())) { parameters.put((String) args[0], args[1]); return proxy; }
                        if (!"getResultList".equals(method.getName())) { return null; }
                        if (jpql.contains("VmInstanceVO")) {
                            return Collections.singletonList(new Object[]{vmState, vmHost});
                        }
                        if (jpql.contains("HostVO")) {
                            return deletedHosts.contains(parameters.get("uuid")) ? Collections.emptyList()
                                    : Collections.singletonList(parameters.get("uuid"));
                        }
                        return hold != null && "Unknown".equals(hold.status) && hold.sourceHostUuid.equals(parameters.get("host"))
                                ? Collections.singletonList(hold) : Collections.emptyList();
                    });
        }
        @Override protected <T> T transaction(java.util.function.Function<EntityManager, T> function) {
            return function.apply(entityManager);
        }
        String vmPolicy(String vm) { return "{\"schemaVersion\":1,\"participation\":\"inherit\"}"; }
        @Override public List<MemoryStateVO> states(List<String> hosts, int start, int limit) {
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host-a");
            state.setStatus("Succeeded"); state.setDesiredRevision(1L); state.setAppliedRevision(1L);
            state.setState("{\"poolGeneration\":\"pool-1\"}");
            return Collections.singletonList(state);
        }
    }

    private static class AtomicDone extends NoErrorCompletion {
        boolean done;
        AtomicDone() { super(); }
        @Override public void done() { done = true; }
    }
}
