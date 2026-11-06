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

    @Test public void expiredLicenseDoesNotPreventConfirmedMigrationRebind() throws Exception {
        FakeRepository repository = heldMigration();
        FakeManager manager = new FakeManager();
        migrate(repository, manager);
        assertFalse(manager.bindCommand.containsKey("participation"));
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
                    answer.state.put("capabilities", Collections.singletonMap("service", false));
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
                    Map<String, Object> capabilities = new HashMap<>(); capabilities.put("service", true);
                    capabilities.put("zram", false); answer.state.put("capabilities", capabilities);
                    capabilities.put("zramReasonCode", "UNSUPPORTED_RECLAIM_KERNEL");
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
                    Map<String, Object> capabilities = new HashMap<>(); capabilities.put("service", true);
                    capabilities.put("zram", false); answer.state.put("capabilities", capabilities);
                    capabilities.put("zramReasonCode", "SERVICE_RECLAIM_CAPABILITY_UNKNOWN");
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

    @Test public void theHoldOfADeletedVmIsReleased() throws Exception {
        FakeRepository repository = heldMigration();
        repository.vmDeleted = true;
        PlacementManager manager = new PlacementManager();
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        // The VM went away mid-migration; its hold would keep the source's reclaim paused for good.
        hooks.reconcileUnknownMigrationHolds("host-a");
        assertEquals(Collections.singletonList("host-a:release"), manager.calls);
        assertEquals("Released", repository.hold.status);
    }

    @Test public void aHoldLeftByARestartedManagementNodeIsReconciledOnceItsVmSettles() throws Exception {
        FakeRepository repository = heldMigration();
        PlacementManager manager = new PlacementManager();
        manager.instances.put("host-a", "instance-source");
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        repository.vmState = "Migrating";
        hooks.reconcileUnknownMigrationHolds("host-a");
        assertEquals("a migration in progress keeps its hold", "Held", repository.hold.status);
        repository.vmState = "Running";
        hooks.reconcileUnknownMigrationHolds("host-a");
        assertEquals("the migration callback may still release it", "Held", repository.hold.status);
        inject(hooks, "settledHoldGraceMillis", 0L);
        hooks.reconcileUnknownMigrationHolds("host-a");
        assertEquals("Released", repository.hold.status);
        assertTrue(manager.calls.toString(), manager.calls.contains("host-a:release"));
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
            if ("vm-efficiency".equals(action)) {
                // The Agent journals each operation UUID; the hold's UUID was already used by the migration's bind.
                MemoryAgentResponse answer = response(host, null);
                answer.operationUuid = String.valueOf(command.get("operationUuid"));
                if ("op-1".equals(answer.operationUuid)) {
                    answer.status = "Failed"; answer.setSuccess(false); answer.reasonCode = "OPERATION_UUID_REUSE";
                }
                if ("bind".equals(command.get("action"))) { bindCommand = new HashMap<>(command); }
                callback.accept(answer);
                return;
            }
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

    @Test public void aRecoveryStepThatFailsAnswersTheOperationAndFreesTheHoldForARetry() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.status = "Unknown";
        PlacementManager manager = new PlacementManager();
        manager.instances.put("host-a", "instance-restarted");
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        repository.failedMerges = 1;
        String[] failure = {null};
        assertFalse("a recovery whose step failed is not a success", recover(hooks, "host-a", failure));
        assertNotNull("the operation is answered with the failure", failure[0]);
        failure[0] = null;
        assertTrue("the hold is not left as being reconciled: " + failure[0], recover(hooks, "host-a", failure));
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
        FakeRepository repository = new FakeRepository();
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

    @Test public void aHoldTheBusyHostIsStillDecidingIsAskedAgain() throws Exception {
        FakeRepository repository = heldMigration();
        List<String> holds = new ArrayList<>();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                if (!"migration-hold".equals(action)) { super.call(host, action, command, callback); return; }
                holds.add(String.valueOf(command.get("holdAction")));
                MemoryAgentResponse answer = response(host, null);
                if (holds.size() == 1) { answer.status = "Converging"; answer.reasonCode = "HOST_OPERATION_IN_PROGRESS"; }
                callback.accept(answer);
            }
        };
        migrate(repository, manager);
        assertEquals(Arrays.asList("release", "release"), holds);
        assertEquals("Released", repository.hold.status);
    }

    @Test public void aFailedRevokeKeepsTheGenerationToRevokeAgain() throws Exception {
        List<Object> revoked = new ArrayList<>();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                if ("state".equals(action)) {
                    // Present before the stop, then authoritatively gone.
                    MemoryAgentResponse answer = response(host, revoked.isEmpty() && bindCommand == null ? "instance-1" : null);
                    bindCommand = new HashMap<>();
                    callback.accept(answer);
                    return;
                }
                revoked.add(command.get("expectedInstanceGeneration"));
                MemoryAgentResponse answer = response(host, null);
                if (revoked.size() == 1) { answer.status = "Failed"; }
                callback.accept(answer);
            }
        };
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", new FakeRepository()); inject(hooks, "manager", manager);
        VmInstanceInventory vm = new VmInstanceInventory(); vm.setUuid("vm-1"); vm.setHostUuid("host-a");
        hooks.beforeStopVm(vm);
        hooks.afterStopVm(vm);
        hooks.afterStopVm(vm);
        assertEquals(Arrays.asList("instance-1", "instance-1"), revoked);
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

    @Test public void migrationToATargetWithoutAZramPoolReleasesWithoutATargetInstanceGeneration() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hostsWithoutPool.add("host-b");
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("state".equals(action) && "host-b".equals(host)) {
                    // The target lists the VM from libvirt; its stopped ZRAM service gives no generation.
                    MemoryAgentResponse answer = response(host, null);
                    ((Map<String, Object>) answer.state.get("vms")).put("vm-1", new HashMap<>());
                    Map<String, Object> capabilities = new HashMap<>(); answer.state.put("capabilities", capabilities);
                    capabilities.put("zram", false); capabilities.put("zramReasonCode", "SERVICE_UNAVAILABLE");
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        migrate(repository, manager);
        assertEquals("Released", repository.hold.status);
        assertNull("nothing is bound on a target without a pool", manager.bindCommand);
        assertNotNull("the source binding is revoked", manager.revokeCommand);
    }

    @Test public void existingVmBindingsSkipAlreadyBoundInstances() throws Exception {
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        FakeManager manager = new FakeManager();
        inject(hooks, "manager", manager);
        inject(hooks, "repository", new FakeRepository() {
            @Override public MemoryStateVO state(String host) {
                MemoryStateVO state = new MemoryStateVO();
                state.setStatus("Succeeded"); state.setDesiredRevision(1L); state.setAppliedRevision(1L);
                return state;
            }
        });
        MemoryAgentResponse response = new MemoryAgentResponse();
        Map<String, Object> vm = new HashMap<>(); vm.put("instanceGeneration", "new-instance");
        Map<String, Object> state = new HashMap<>(); state.put("inventoryComplete", true);
        state.put("vms", Collections.singletonMap("vm-1", vm)); response.state = state;
        hooks.reconcileObservedBindings("host-a", response);
        assertFalse(manager.bindCommand.containsKey("participation"));
        assertEquals("new-instance", manager.bindCommand.get("expectedInstanceGeneration"));
        manager.bindCommand = null;
        // The service reports a bound VM with these wire names; such a VM is not bound again.
        vm.put("binding_generation", "new-instance"); vm.put("binding_mode", "auto");
        hooks.reconcileObservedBindings("host-a", response);
        assertNull("a VM already bound at its instance generation is not bound again", manager.bindCommand);
        vm.remove("binding_generation"); state.put("inventoryComplete", false);
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
        @Override void later(Runnable task, long seconds) { task.run(); }
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
        MemoryMigrationVO hold;
        int failedMerges;
        private final EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, (proxy, method, args) -> {
                    if ("find".equals(method.getName()) && args != null && args.length >= 2
                            && args[0] == MemoryMigrationVO.class) { return hold; }
                    if ("merge".equals(method.getName()) && failedMerges > 0) {
                        failedMerges--;
                        throw new MemoryOperationException("MEMORY_TEST_DATABASE_UNAVAILABLE", "database unavailable");
                    }
                    if ("merge".equals(method.getName())) { hold = (MemoryMigrationVO) args[0]; return hold; }
                    if ("flush".equals(method.getName())) { return null; }
                    if (method.getReturnType().isPrimitive()) {
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                    }
                    return null;
                });
        String vmState = "Running";
        String vmHost = "host-a";
        boolean vmDeleted;
        final Set<String> deletedHosts = new HashSet<>();
        /** VM placement, Host existence and the source's holds, answered from the fixtures. */
        @Override public VmPlacement vmPlacement(String vmUuid) {
            return vmDeleted ? null : new VmPlacement(org.zstack.header.vm.VmInstanceState.valueOf(vmState), vmHost);
        }
        @Override public boolean hostExists(String host) { return !deletedHosts.contains(host); }
        @Override public List<MemoryMigrationVO> unknownOrOrphanedHolds(String sourceHost) {
            boolean listed = hold != null && ("Unknown".equals(hold.status) || vmDeleted && !"Released".equals(hold.status));
            return listed && hold.sourceHostUuid.equals(sourceHost) ? Collections.singletonList(hold) : Collections.emptyList();
        }
        @Override public List<MemoryMigrationVO> settledHolds(String sourceHost) {
            boolean settled = hold != null && ("Preparing".equals(hold.status) || "Held".equals(hold.status))
                    && !vmDeleted && !"Migrating".equals(vmState);
            return settled && hold.sourceHostUuid.equals(sourceHost) ? Collections.singletonList(hold) : Collections.emptyList();
        }
        @Override protected <T> T transaction(java.util.function.Function<EntityManager, T> function) {
            return function.apply(entityManager);
        }
        final Set<String> hostsWithoutPool = new HashSet<>();
        @Override public MemoryStateVO state(String host) {
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host-a");
            state.setStatus("Succeeded"); state.setDesiredRevision(1L); state.setAppliedRevision(1L);
            state.setState(hostsWithoutPool.contains(host) ? "{}" : "{\"zram\":{\"poolGeneration\":\"pool-1\"}}");
            return state;
        }
    }

    private static class AtomicDone extends NoErrorCompletion {
        boolean done;
        AtomicDone() { super(); }
        @Override public void done() { done = true; }
    }

    @Test public void aLifecycleBindingWithoutAGenerationIsAFailureUnlessTheVmIsProvenAbsent() {
        MemoryAgentResponse absent = response("host-a", null);
        assertNull("a VM proven absent has nothing to revoke",
                MemoryLifecycleHooks.lifecycleFailure("revoke", MemoryLifecycleHooks.vmIdentity(absent, "vm-1"), absent));
        assertEquals("VM_INSTANCE_NOT_FOUND",
                MemoryLifecycleHooks.lifecycleFailure("bind", MemoryLifecycleHooks.vmIdentity(absent, "vm-1"), absent));
        // The complete inventory lists the VM but not its instance generation.
        MemoryAgentResponse listed = response("host-a", null);
        ((Map<String, Object>) listed.state.get("vms")).put("vm-1", new HashMap<>());
        for (String action : Arrays.asList("revoke", "bind")) {
            String failure = MemoryLifecycleHooks.lifecycleFailure(action, MemoryLifecycleHooks.vmIdentity(listed, "vm-1"), listed);
            assertTrue("a present VM without a generation is unknown for " + action + ", got " + failure,
                    failure.startsWith("VM_IDENTITY_UNKNOWN: "));
        }
        MemoryAgentResponse partial = response("host-a", null);
        partial.state.put("inventoryComplete", false);
        assertTrue(MemoryLifecycleHooks.lifecycleFailure("revoke", MemoryLifecycleHooks.vmIdentity(partial, "vm-1"), partial)
                .startsWith("VM_IDENTITY_UNKNOWN: "));
    }

    @Test public void aFailedAgentReplyAlwaysNamesAReason() {
        MemoryAgentResponse reply = new MemoryAgentResponse();
        reply.setSuccess(false);
        assertEquals("a failed reply without any reason is still a failure",
                "INVALID_AGENT_RESPONSE", MemoryOptimizationManager.failure(reply));
        reply.setError("connection reset");
        assertEquals("the Agent error names the failure", "connection reset", MemoryOptimizationManager.failure(reply));
        reply.reasonCode = "STALE_GENERATION";
        assertEquals("STALE_GENERATION", MemoryOptimizationManager.failure(reply));
        assertEquals("AGENT_UNREACHABLE", MemoryOptimizationManager.failure(null));
    }
}
