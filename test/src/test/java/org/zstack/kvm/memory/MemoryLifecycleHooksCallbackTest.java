package org.zstack.kvm.memory;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.MigrateVmCmd;

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
        assertEquals("allow", manager.bindCommand.get("participation"));
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

    @Test public void directMigrationWithoutTargetGenerationKeepsHoldsUnknown() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.swapDirect = true;
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
        assertNull(manager.revokeCommand);
        assertEquals("Unknown", repository.hold.status);
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

    @Test public void aTargetWhoseRunningServiceCannotBeReadKeepsTheHold() throws Exception {
        FakeRepository repository = heldMigration();
        // The target's last observation could not read its running service, so it recorded no pool.
        repository.hostsWithoutPool.add("host-b");
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command,
                                Consumer<MemoryAgentResponse> callback) {
                if ("state".equals(action) && "host-b".equals(host)) {
                    MemoryAgentResponse answer = response(host, null);
                    ((Map<String, Object>) answer.state.get("vms")).put("vm-1",
                            Collections.singletonMap("identityReason", "VM_IDENTITY_UNAVAILABLE"));
                    answer.state.put("managed", true);
                    answer.state.put("zram", Collections.singletonMap("serviceState", "active"));
                    Map<String, Object> capabilities = new HashMap<>(); answer.state.put("capabilities", capabilities);
                    capabilities.put("service", false); capabilities.put("zram", false);
                    capabilities.put("zramReasonCode", "SERVICE_RECLAIM_CAPABILITY_UNKNOWN");
                    callback.accept(answer);
                } else { super.call(host, action, command, callback); }
            }
        };
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        inject(hooks, "repository", repository); inject(hooks, "manager", manager);
        VmInstanceInventory vm = new VmInstanceInventory(); vm.setUuid("vm-1");
        hooks.afterMigrateVm(vm, "host-a", new AtomicDone());
        hooks.reconcileUnknownMigrationHolds("host-a");
        assertEquals("Unknown", repository.hold.status);
        assertNull(manager.bindCommand);
        assertNull("the source binding stays until the target instance is known", manager.revokeCommand);
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
        assertEquals("vm-1", manager.reconciledVm);
        manager.reconciledVm = null;
        // The service reports a bound VM with these wire names; such a VM is not bound again.
        vm.put("binding_generation", "new-instance"); vm.put("binding_mode", "auto");
        hooks.reconcileObservedBindings("host-a", response);
        assertNull("a VM already bound at its instance generation is not bound again", manager.reconciledVm);
        vm.remove("binding_generation"); state.put("inventoryComplete", false);
        hooks.reconcileObservedBindings("host-a", response);
        assertNull(manager.bindCommand);
    }

    @Test public void aBindingStillWaitingIsNotRequestedAgainByTheNextObservation() throws Exception {
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
        List<Consumer<MemoryAgentResponse>> waiting = new ArrayList<>();
        FakeManager manager = new FakeManager() {
            @Override void reconcileVmConfiguration(String vm, Consumer<MemoryAgentResponse> callback) { waiting.add(callback); }
        };
        inject(hooks, "manager", manager);
        inject(hooks, "repository", new FakeRepository() {
            @Override public MemoryStateVO state(String host) {
                MemoryStateVO state = new MemoryStateVO();
                state.setStatus("Succeeded"); state.setDesiredRevision(1L); state.setAppliedRevision(1L);
                return state;
            }
        });
        MemoryAgentResponse response = new MemoryAgentResponse();
        Map<String, Object> state = new HashMap<>(); state.put("inventoryComplete", true);
        state.put("vms", Collections.singletonMap("vm-1", Collections.singletonMap("instanceGeneration", "new-instance")));
        response.state = state;
        hooks.reconcileObservedBindings("host-a", response);
        hooks.reconcileObservedBindings("host-a", response);
        assertEquals("the VM is busy: its binding is requested once", 1, waiting.size());
        waiting.remove(0).accept(response("host-a", null));
        hooks.reconcileObservedBindings("host-a", response);
        assertEquals("an answered binding may be requested again", 1, waiting.size());
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

    @Test public void directMigrationReleasesEachPoolUsingItsOwnGeneration() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.swapDirect = true;
        repository.hold.targetPoolGeneration = "pool-destination";
        repository.hold.sourceHoldStatus = "Held"; repository.hold.targetHoldStatus = "Held";
        Map<String, String> releases = new LinkedHashMap<>();
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                if ("migration-hold".equals(action)) { releases.put(host, (String) command.get("poolGeneration")); }
                super.call(host, action, command, callback);
            }
        };
        migrate(repository, manager);
        assertEquals("pool-1", releases.get("host-a"));
        assertEquals("pool-destination", releases.get("host-b"));
        assertEquals("Released", repository.hold.sourceHoldStatus);
        assertEquals("Released", repository.hold.targetHoldStatus);
        assertEquals("Released", repository.hold.status);
    }

    @Test public void lostDestinationReleaseCannotBeReportedAsReleased() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.swapDirect = true;
        repository.hold.targetPoolGeneration = "pool-destination";
        repository.hold.sourceHoldStatus = "Held"; repository.hold.targetHoldStatus = "Held";
        FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                if ("host-b".equals(host) && "migration-hold".equals(action)) { callback.accept(null); }
                else { super.call(host, action, command, callback); }
            }
        };
        migrate(repository, manager);
        assertEquals("Released", repository.hold.sourceHoldStatus);
        assertEquals("Unknown", repository.hold.targetHoldStatus);
        assertEquals("Unknown", repository.hold.status);
        // The same persisted intent is retried; its source release is already confirmed.
        List<String> retries = new ArrayList<>();
        migrate(repository, new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                if ("migration-hold".equals(action)) { retries.add(host); }
                super.call(host, action, command, callback);
            }
        });
        assertEquals(Collections.singletonList("host-b"), retries);
        assertEquals("Released", repository.hold.status);
    }

    @Test public void commandRequiresBothConfirmedHoldsAndMatchingPlacement() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.swapDirect = true; repository.hold.swapDirectState = "{\"operationUuid\":\"op-1\"}";
        repository.hold.sourceHoldStatus = "Held"; repository.hold.targetHoldStatus = "Unknown";
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
        try { hooks.directMigrationCommand("vm-1", "host-a", "host-b"); fail("unknown target must reject"); }
        catch (MemoryOperationException expected) { }
        repository.hold.targetHoldStatus = "Held";
        assertEquals("op-1", hooks.directMigrationCommand("vm-1", "host-a", "host-b").get("operationUuid"));
        try { hooks.directMigrationCommand("vm-1", "host-a", "another-host"); fail("target changed"); }
        catch (MemoryOperationException expected) { }
        repository.hold.status = "Released";
        assertNull(hooks.directMigrationCommand("vm-1", "host-a", "host-b"));
    }

    @Test public void anOrdinaryMigrationChosenByTheSourceAgentKeepsBothHoldsForTheirRelease() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.swapDirect = true; repository.hold.swapDirectState = "{}";
        repository.hold.sourceHoldStatus = "Held"; repository.hold.targetHoldStatus = "Held";
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
        hooks.recordDirectMigrationResult("vm-1", Collections.singletonMap("operationUuid", "op-1"),
                Collections.singletonMap("fallback", "SWAP_DIRECT_GUEST_MEMORY_UNSUPPORTED"), true);
        assertTrue(repository.hold.swapDirectState.contains("SWAP_DIRECT_GUEST_MEMORY_UNSUPPORTED"));
        assertTrue("the destination hold and its writeback quota are still released after the migration",
                repository.hold.swapDirect);
        assertEquals("Held", repository.hold.targetHoldStatus);
    }

    @Test public void failedCounterWriteDoesNotFailTheCompletedMigrationCallback() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.swapDirect = true; repository.hold.swapDirectState = "{}";
        repository.failedMerges = 1;
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
        hooks.recordDirectMigrationResult("vm-1", Collections.singletonMap("operationUuid", "op-1"),
                Collections.singletonMap("pages-received", 1025), false);
        assertEquals("Held", repository.hold.status);
        assertEquals(0, repository.failedMerges);
    }

    @Test public void staleHoldCannotEraseCountersOrReacquireAReleasedSide() throws Exception {
        FakeRepository repository = heldMigration();
        repository.hold.swapDirect = true;
        repository.hold.sourceHoldStatus = "Released"; repository.hold.targetHoldStatus = "Unknown";
        repository.hold.status = "Unknown";
        repository.hold.swapDirectState = "{\"sourceResult\":{\"pages-sent\":1024}}";
        MemoryMigrationVO stale = heldMigration().hold;
        stale.swapDirect = true; stale.sourceHoldStatus = "Held"; stale.targetHoldStatus = "Held";
        stale.swapDirectState = "{\"operationUuid\":\"op-1\"}";
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
        java.lang.reflect.Method record = MemoryLifecycleHooks.class.getDeclaredMethod("record", MemoryMigrationVO.class);
        record.setAccessible(true); record.invoke(hooks, stale);
        assertEquals("Released", repository.hold.sourceHoldStatus);
        assertEquals("Preparing", repository.hold.status);
        assertTrue(repository.hold.swapDirectState.contains("pages-sent"));
    }

    @Test public void directPreparationGrantsQuotaAfterBothPreflightsAndWaitsForItsResult() throws Exception {
        // A complete scan is used as counted, and so is a pool without a writeback disk;
        // a partial scan is replaced by all swapped-out pages.
        String[][] estimates = {{"native_get_pagemap", "625004"}, {"native_get_pagemap_partial", "1048576"},
                {"no_backing_device", "0"}};
        for (int round = 0; round < 6; round++) {
            boolean acceptBudget = round < 3;
            String[] estimate = estimates[round % 3];
            FakeRepository repository = heldMigration();
            repository.hold.swapDirect = true;
            repository.hold.sourceHoldStatus = "Held";
            List<String> calls = new ArrayList<>();
            FakeManager manager = new FakeManager() {
                @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                    MemoryAgentResponse answer = response(host, null);
                    if ("state".equals(action)) {
                        answer.state.put("poolGeneration", "pool-target");
                        if (Boolean.TRUE.equals(command.get("swapDirectPreflight"))) {
                            calls.add("preflight-" + host);
                            Map<String, Object> info = new HashMap<>();
                            info.put("supported", true); info.put("protocol", 1); info.put("hostUuid", host);
                            info.put("poolGeneration", command.get("poolGeneration")); info.put("compressor", "lzo-rle");
                            info.put("memoryBytes", "8589934592"); info.put("coldBytes", "4294967296");
                            if ("host-a".equals(host)) {
                                info.put("backingPages", "no_backing_device".equals(estimate[0]) ? "0" : "625004");
                                info.put("backingEstimate", estimate[0]);
                            } else {
                                // the destination check is told how many written-back pages are coming
                                assertEquals(estimate[1], command.get("swapDirectWritebackPages"));
                            }
                            answer.state.put("swapDirect", info);
                        }
                    } else if ("migration-hold".equals(action)) {
                        assertEquals("host-b", host);
                        if (command.containsKey("writebackPages")) {
                            calls.add("quota");
                            assertNotNull(repository.hold.swapDirectState);
                            assertEquals(estimate[1], command.get("writebackPages"));
                            if (!acceptBudget) { callback.accept(null); return; }
                        } else { calls.add("hold"); }
                    }
                    callback.accept(answer);
                }
            };
            MemoryLifecycleHooks hooks = new MemoryLifecycleHooks();
            inject(hooks, "repository", repository); inject(hooks, "manager", manager);
            boolean[] result = new boolean[2];
            org.zstack.header.core.Completion completion = new org.zstack.header.core.Completion(null) {
                @Override public void success() { result[0] = true; }
                @Override public void fail(org.zstack.header.errorcode.ErrorCode error) { result[1] = true; }
            };
            java.lang.reflect.Method prepare = MemoryLifecycleHooks.class.getDeclaredMethod("prepareDirectTarget", MemoryMigrationVO.class, org.zstack.header.core.Completion.class);
            prepare.setAccessible(true); prepare.invoke(hooks, repository.hold, completion);
            assertEquals(Arrays.asList("hold", "preflight-host-a", "preflight-host-b", "quota"), calls);
            assertEquals(acceptBudget, result[0]); assertEquals(!acceptBudget, result[1]);
            assertEquals(acceptBudget ? "Held" : "Unknown", repository.hold.targetHoldStatus);
        }
    }

    private static String directHostObservation(String boot, String pool, boolean supported) {
        Map<String, Object> proof = new HashMap<>();
        proof.put("bootId", boot); proof.put("poolGeneration", pool); proof.put("supported", supported);
        proof.put("reasonCode", "SWAP_DIRECT_LIBVIRT_CAPABILITY_UNCONFIRMED");
        Map<String, Object> raw = new HashMap<>();
        raw.put("bootId", "boot"); raw.put("zram", Collections.singletonMap("poolGeneration", "pool-1"));
        raw.put("capabilities", Collections.singletonMap("swapDirect", proof));
        return new com.google.gson.Gson().toJson(raw);
    }

    private static MigrateVmCmd migrationCommand(boolean xbzrle) {
        MigrateVmCmd cmd = new MigrateVmCmd(); cmd.setVmUuid("vm-1"); cmd.setXbzrle(xbzrle); return cmd;
    }

    private static boolean[] selectMigration(MemoryLifecycleHooks hooks, MigrateVmCmd cmd) throws Exception {
        return selectMigration(hooks, cmd, Collections.emptySet());
    }

    /** Hosts in {@code directOff} have cold-page direct migration turned off, through their cluster or themselves. */
    private static boolean[] selectMigration(MemoryLifecycleHooks hooks, MigrateVmCmd cmd, Set<String> directOff) throws Exception {
        inject(hooks, "rcf", Proxy.newProxyInstance(org.zstack.resourceconfig.ResourceConfigFacade.class.getClassLoader(),
                new Class<?>[]{org.zstack.resourceconfig.ResourceConfigFacade.class}, (proxy, method, args) -> {
                    assertEquals("getResourceConfigValue", method.getName());
                    assertSame(MemoryStandardGlobalConfig.SWAP_DIRECT_ENABLED, args[0]);
                    return !directOff.contains((String) args[1]);
                }));
        boolean[] result = new boolean[2];
        hooks.prepareMigrationCommand(cmd, "host-a", "host-b", new org.zstack.header.core.Completion(null) {
            @Override public void success() { result[0] = true; }
            @Override public void fail(org.zstack.header.errorcode.ErrorCode error) { result[1] = true; }
        });
        return result;
    }

    @Test public void unprovenOrStaleHostCacheAddsNoRpcOrHoldAndPreservesOrdinaryXbzrle() throws Exception {
        List<String> observations = Arrays.asList("{}", "not-json", directHostObservation("boot", "pool-1", false),
                directHostObservation("old-boot", "pool-1", true), directHostObservation("boot", "old-pool", true));
        for (String rejectedHost : Arrays.asList("host-a", "host-b")) {
            for (String observation : observations) {
                for (boolean xbzrle : new boolean[]{false, true}) {
                    FakeRepository repository = heldMigration(); repository.hold.sourceHoldStatus = "Held";
                    repository.hostObservations.put("host-a", directHostObservation("boot", "pool-1", true));
                    repository.hostObservations.put("host-b", directHostObservation("boot", "pool-1", true));
                    repository.hostObservations.put(rejectedHost, observation);
                    MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
                    inject(hooks, "manager", new FakeManager() {
                        @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                            fail("cache rejection must not issue an Agent RPC: " + action);
                        }
                    });
                    MigrateVmCmd cmd = migrationCommand(xbzrle);
                    assertArrayEquals(new boolean[]{true, false}, selectMigration(hooks, cmd));
                    assertNull(cmd.getSwapDirect()); assertEquals(xbzrle, cmd.isXbzrle());
                    assertFalse(repository.hold.swapDirect); assertEquals("Held", repository.hold.sourceHoldStatus);
                    assertNull(repository.hold.targetPoolGeneration); assertNull(repository.hold.targetHoldStatus);
                    assertTrue(repository.hold.reason, repository.hold.reason.startsWith("host-a".equals(rejectedHost) ? "SOURCE " : "TARGET "));
                }
            }
        }
    }

    @Test public void directMigrationTurnedOffOnEitherHostMigratesTheOrdinaryWayWithoutAgentCalls() throws Exception {
        for (String off : Arrays.asList("host-a", "host-b")) {
            FakeRepository repository = heldMigration(); repository.hold.sourceHoldStatus = "Held";
            for (String host : Arrays.asList("host-a", "host-b")) { repository.hostObservations.put(host, directHostObservation("boot", "pool-1", true)); }
            MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
            inject(hooks, "manager", new FakeManager() {
                @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                    fail("a migration with direct migration turned off must not issue an Agent RPC: " + action);
                }
            });
            MigrateVmCmd cmd = migrationCommand(true);
            assertArrayEquals(new boolean[]{true, false}, selectMigration(hooks, cmd, Collections.singleton(off)));
            assertNull(cmd.getSwapDirect()); assertTrue(cmd.isXbzrle());
            assertFalse(repository.hold.swapDirect); assertEquals("Held", repository.hold.sourceHoldStatus);
            assertNull(repository.hold.targetHoldStatus);
            assertEquals("SWAP_DIRECT_DISABLED", repository.hold.reason);
        }
    }

    @Test public void cachedSupportStillRejectsNonSharedOrDestinationDrivenCommandsBeforeExtraHolds() throws Exception {
        for (int mode = 0; mode < 4; mode++) {
            FakeRepository repository = heldMigration(); repository.hold.sourceHoldStatus = "Held";
            for (String host : Arrays.asList("host-a", "host-b")) { repository.hostObservations.put(host, directHostObservation("boot", "pool-1", true)); }
            MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
            inject(hooks, "manager", new FakeManager() {
                @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                    fail("incompatible command must not acquire extra holds");
                }
            });
            MigrateVmCmd cmd = migrationCommand(true);
            if (mode == 0) { cmd.setMigrateFromDestination(true); }
            if (mode == 1) { cmd.setDisks(Collections.singletonMap("disk", null)); }
            if (mode == 2) { cmd.setStorageMigrationPolicy("FullCopy"); }
            if (mode == 3) { cmd.setStorageMigrationPolicy("IncCopy"); }
            assertArrayEquals(new boolean[]{true, false}, selectMigration(hooks, cmd));
            assertNull(cmd.getSwapDirect()); assertTrue(cmd.isXbzrle());
            assertEquals("SOURCE_DRIVEN_SHARED_STORAGE_PRECOPY_REQUIRED", repository.hold.reason);
        }
    }

    @Test public void automaticDirectSelectionAndPreflightFallbackKeepTheOrdinarySourceHold() throws Exception {
        for (String failure : Arrays.asList("none", "source-protocol", "target-protocol", "compressor", "target-identity", "budget", "cleanup", "source-released")) {
            FakeRepository repository = heldMigration(); repository.hold.sourceHoldStatus = "Held";
            for (String host : Arrays.asList("host-a", "host-b")) { repository.hostObservations.put(host, directHostObservation("boot", "pool-1", true)); }
            List<String> calls = new ArrayList<>();
            MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, "repository", repository);
            inject(hooks, "manager", new FakeManager() {
                @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                    MemoryAgentResponse answer = response(host, null);
                    if ("state".equals(action)) {
                        answer.state.put("poolGeneration", "pool-target");
                        if ("target-identity".equals(failure)) { answer.state.put("inventoryComplete", false); }
                        if (Boolean.TRUE.equals(command.get("swapDirectPreflight"))) {
                            calls.add("preflight-" + host);
                            Map<String, Object> info = new HashMap<>();
                            boolean supported = !("source-protocol".equals(failure) && "host-a".equals(host)
                                    || ("target-protocol".equals(failure) || "cleanup".equals(failure) || "source-released".equals(failure)) && "host-b".equals(host));
                            info.put("supported", supported); info.put("reasonCode", "RUNNING_PROTOCOL_UNCONFIRMED");
                            info.put("protocol", 1); info.put("hostUuid", host); info.put("poolGeneration", command.get("poolGeneration"));
                            info.put("compressor", "compressor".equals(failure) && "host-b".equals(host) ? "zstd" : "lzo-rle");
                            info.put("memoryBytes", "8589934592"); info.put("coldBytes", "4096"); info.put("backingPages", "1");
                            answer.state.put("swapDirect", info);
                        }
                    } else if ("migration-hold".equals(action)) {
                        assertEquals("ordinary source hold must stay untouched", "host-b", host);
                        if ("source-released".equals(failure) && "release".equals(command.get("holdAction"))) {
                            repository.hold.sourceHoldStatus = "Released";
                        }
                        calls.add(command.containsKey("writebackPages") ? "budget" : (String) command.get("holdAction"));
                        if ("budget".equals(failure) && command.containsKey("writebackPages")
                                || "cleanup".equals(failure) && "release".equals(command.get("holdAction"))) {
                            callback.accept(null); return;
                        }
                    } else { fail("unexpected action " + action); }
                    callback.accept(answer);
                }
            });
            MigrateVmCmd cmd = migrationCommand(true);
            boolean[] result = selectMigration(hooks, cmd);
            boolean cleanupUnconfirmed = "cleanup".equals(failure) || "source-released".equals(failure);
            assertEquals(!cleanupUnconfirmed, result[0]); assertEquals(cleanupUnconfirmed, result[1]);
            assertEquals("source-released".equals(failure) ? "Released" : "Held", repository.hold.sourceHoldStatus);
            if ("none".equals(failure)) {
                assertNotNull(cmd.getSwapDirect()); assertFalse(cmd.isXbzrle());
                assertEquals(Arrays.asList("acquire", "preflight-host-a", "preflight-host-b", "budget"), calls);
                assertEquals("Held", repository.hold.targetHoldStatus); assertTrue(repository.hold.swapDirect);
            } else {
                assertNull(cmd.getSwapDirect()); assertTrue(cmd.isXbzrle());
                if (cleanupUnconfirmed) {
                    assertTrue(repository.hold.swapDirect);
                    assertEquals("cleanup".equals(failure) ? "Unknown" : "Released", repository.hold.targetHoldStatus);
                } else {
                    assertFalse(repository.hold.swapDirect); assertEquals("Held", repository.hold.status);
                    assertEquals("Released", repository.hold.targetHoldStatus); assertNull(repository.hold.swapDirectState);
                    assertNotNull(repository.hold.reason);
                    if (failure.endsWith("protocol")) { assertTrue(repository.hold.reason.contains("RUNNING_PROTOCOL_UNCONFIRMED")); }
                    if (!"target-identity".equals(failure)) { assertEquals("release", calls.get(calls.size() - 1)); }
                }
            }
        }
    }

    private static class FakeManager extends MemoryOptimizationManager {
        Map<String, Object> bindCommand;
        Map<String, Object> revokeCommand;
        String reconciledVm;
        @Override void reconcileVmConfiguration(String vm, Consumer<MemoryAgentResponse> callback) {
            reconciledVm = vm; callback.accept(response("host-a", null));
        }
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
        String reclaimMode = "auto";
        Boolean sharePages = true;
        @Override public String vmReclaimMode(String vm) { return reclaimMode; }
        @Override public Boolean vmSharePages(String vm) { return sharePages; }
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
        final Map<String, String> hostObservations = new HashMap<>();
        final Set<String> hostsWithoutPool = new HashSet<>();
        @Override public MemoryStateVO state(String host) {
            MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host-a");
            state.setStatus("Succeeded"); state.setDesiredRevision(1L); state.setAppliedRevision(1L);
            state.setState(hostObservations.getOrDefault(host, hostsWithoutPool.contains(host) ? "{}" : "{\"zram\":{\"poolGeneration\":\"pool-1\"}}"));
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

    @Test public void migrationRebindRetainsTheCurrentVmReclaimOptOut() throws Exception {
        FakeRepository repository = heldMigration(); repository.reclaimMode = "deny";
        FakeManager manager = new FakeManager();
        migrate(repository, manager);
        assertEquals("deny", manager.bindCommand.get("participation"));
        assertEquals("Released", repository.hold.status);
    }

    @Test public void vmConfigurationReadsLatestValueInsideTheVmLifecycleQueue() {
        VmConfigurationFixture f = new VmConfigurationFixture();
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.RECONCILE));
        assertEquals("Vm-vm-1", f.queued.getSyncSignature());
        assertNull(f.manager.bindCommand);
        f.repository.reclaimMode = "deny";
        f.runQueued();
        assertTrue(f.reply.isSuccess());
        assertEquals("deny", f.manager.bindCommand.get("participation"));
        assertEquals("current-instance", f.manager.bindCommand.get("expectedInstanceGeneration"));
        assertEquals(1, f.completed);
        f.repository.reclaimMode = "auto";
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.RECONCILE));
        f.runQueued();
        assertEquals("allow", f.manager.bindCommand.get("participation"));
    }

    @Test public void aBindingReadsOnlyTheVmIdentityAndPageSharingAppliesAValueNotSavedYet() {
        VmConfigurationFixture f = new VmConfigurationFixture();
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.RECONCILE)); f.runQueued();
        assertEquals(Boolean.TRUE, f.stateCommand.get("vmIdentity"));
        assertFalse("a binding does not observe the whole Host", f.stateCommand.containsKey("vmMemoryState"));
        f.repository.sharePages = true;
        MemoryVmConfigurationMsg apply = f.message(MemoryVmConfigurationMsg.Action.APPLY_KSM);
        apply.setKsmEnabled(false);
        f.coordinator.handle(apply); f.runQueued();
        assertEquals(Boolean.TRUE, f.stateCommand.get("vmMemoryState"));
        assertEquals("set-ksm", f.ksmCommand.get("action"));
        assertEquals("the value under validation, not the saved one", Boolean.FALSE, f.ksmCommand.get("ksmEnabled"));
        assertFalse(f.ksmCommand.containsKey("vmIdentity"));
    }

    @Test public void vmReadStateRequiresMatchingInstanceAndQmpEvidence() {
        VmConfigurationFixture f = new VmConfigurationFixture();
        f.repository.reclaimMode = "deny"; f.repository.sharePages = false;
        f.vm.put("binding_mode", "deny"); f.vm.put("binding_generation", "current-instance");
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.READ));
        assertNull("read-only query does not enqueue a write", f.queued);
        assertEquals("Applied", ((Map<?, ?>) f.reply.getState().get("ksm")).get("state"));
        assertEquals("Applied", ((Map<?, ?>) f.reply.getState().get("zram")).get("state"));
        f.vm.put("binding_generation", "old-instance"); f.proof.put("instanceGeneration", "old-instance");
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.READ));
        assertEquals("Unknown", ((Map<?, ?>) f.reply.getState().get("ksm")).get("state"));
        assertEquals("Pending", ((Map<?, ?>) f.reply.getState().get("zram")).get("state"));
        assertNull(f.manager.bindCommand);
        // On a Host that compresses no memory the VM's setting has nothing to act on.
        f.repository.hostsWithoutPool.add("host-a"); f.repository.hostsWithoutPool.add("host-b");
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.READ));
        assertEquals("NotApplicable", ((Map<?, ?>) f.reply.getState().get("zram")).get("state"));
        assertEquals(false, ((Map<?, ?>) f.reply.getState().get("zram")).get("configuredValue"));
    }

    @Test public void vmSharingIsNotApplicableOnAHostThatMergesNoPages() {
        VmConfigurationFixture f = new VmConfigurationFixture();
        f.repository.sharePages = true;
        f.proof.put("backends", Collections.singletonList(Collections.singletonMap("merge", true)));
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.READ));
        assertEquals("Applied", ((Map<?, ?>) f.reply.getState().get("ksm")).get("state"));
        f.repository.hostObservations.put("host-a", "{\"zram\":{\"poolGeneration\":\"pool-1\"},\"ksm\":{\"enabled\":false}}");
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.READ));
        Map<?, ?> ksm = (Map<?, ?>) f.reply.getState().get("ksm");
        assertEquals("NotApplicable", ksm.get("state")); assertEquals("HOST_KSM_NOT_ENABLED", ksm.get("reasonCode"));
        assertEquals(true, ksm.get("configuredValue"));
        // A VM kept out of merging is out of it on any Host.
        f.repository.sharePages = false;
        f.proof.put("backends", Collections.singletonList(Collections.singletonMap("merge", false)));
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.READ));
        assertEquals("Applied", ((Map<?, ?>) f.reply.getState().get("ksm")).get("state"));
    }

    @Test public void vmConfigurationDoesNotModifyAnUnknownOrReplacedVm() {
        VmConfigurationFixture f = new VmConfigurationFixture();
        f.repository.vmState = "Unknown";
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.RECONCILE)); f.runQueued();
        assertFalse(f.reply.isSuccess()); assertNull(f.manager.bindCommand);
        f.repository.vmState = "Running"; f.vm.remove("instanceGeneration");
        f.coordinator.handle(f.message(MemoryVmConfigurationMsg.Action.RECONCILE)); f.runQueued();
        assertFalse(f.reply.isSuccess()); assertNull(f.manager.bindCommand);
        assertEquals(2, f.completed);
    }

    @Test public void vmConfigurationSharesReadAcrossFeaturesButNotAfterChanges() throws Exception {
        VmConfigurationFixture f = new VmConfigurationFixture();
        f.coordinator.readState("vm-1", "ksm");
        f.coordinator.readState("vm-1", "zram");
        assertEquals("two config categories must share one READ", 1, f.readCalls);
        assertEquals(10000L, f.readTimeout);
        f.repository.reclaimMode = "deny";
        assertEquals(false, f.coordinator.readState("vm-1", "zram").get("configuredValue"));
        assertEquals("configuration changes bypass cached evidence", 2, f.readCalls);
        Thread.sleep(1100);
        f.coordinator.readState("vm-1", "ksm");
        assertEquals("readback cache must not outlive one second", 3, f.readCalls);
    }

    @Test public void vmConfigurationReadFailureIsUnknownAndShared() {
        VmConfigurationFixture f = new VmConfigurationFixture();
        f.readFailure = true;
        assertEquals("Unknown", f.coordinator.readState("vm-1", "ksm").get("state"));
        assertEquals("Unknown", f.coordinator.readState("vm-1", "zram").get("state"));
        assertEquals("timeout must not trigger a second wait for the other feature", 1, f.readCalls);
        assertEquals(10000L, f.readTimeout);
    }

    private static class VmConfigurationFixture {
        final FakeRepository repository = new FakeRepository();
        final Map<String, Object> vm = new HashMap<>(), proof = new HashMap<>();
        org.zstack.core.thread.ChainTask queued;
        MemoryVmConfigurationMsg.Reply reply;
        int completed;
        int readCalls;
        long readTimeout;
        boolean readFailure;
        Map<String, Object> stateCommand, ksmCommand;
        final FakeManager manager = new FakeManager() {
            @Override void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> callback) {
                if (!"state".equals(action)) {
                    if ("set-ksm".equals(command.get("action"))) { ksmCommand = new HashMap<>(command); }
                    super.call(host, action, command, answer -> {
                        answer.operationUuid = (String) command.get("operationUuid"); callback.accept(answer);
                    }); return;
                }
                stateCommand = new HashMap<>(command);
                MemoryAgentResponse observed = response(host, "current-instance");
                observed.state.put("vms", Collections.singletonMap("vm-1", vm));
                observed.state.put("vmMemoryState", proof);
                callback.accept(observed);
            }
        };
        MemoryVmConfiguration coordinator;
        VmConfigurationFixture() {
            vm.put("instanceGeneration", "current-instance");
            proof.put("instanceGeneration", "current-instance"); proof.put("supported", true);
            proof.put("backends", Collections.singletonList(Collections.singletonMap("merge", false)));
            org.zstack.core.cloudbus.CloudBus bus = (org.zstack.core.cloudbus.CloudBus) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{org.zstack.core.cloudbus.CloudBus.class}, (proxy, method, args) -> {
                    if ("reply".equals(method.getName())) { reply = (MemoryVmConfigurationMsg.Reply) args[1]; }
                    if ("call".equals(method.getName())) {
                        MemoryVmConfigurationMsg msg = (MemoryVmConfigurationMsg) args[0];
                        readCalls++; readTimeout = msg.getTimeout();
                        if (readFailure) { throw new IllegalStateException("READ_TIMEOUT"); }
                        coordinator.handle(msg);
                        return reply;
                    }
                    return null;
                });
            org.zstack.core.thread.ThreadFacade threads = (org.zstack.core.thread.ThreadFacade) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{org.zstack.core.thread.ThreadFacade.class}, (proxy, method, args) -> {
                    if ("chainSubmit".equals(method.getName())) { queued = (org.zstack.core.thread.ChainTask) args[0]; }
                    return null;
                });
            coordinator = new MemoryVmConfiguration(bus, threads, repository, manager);
        }
        MemoryVmConfigurationMsg message(MemoryVmConfigurationMsg.Action action) {
            MemoryVmConfigurationMsg msg = new MemoryVmConfigurationMsg(); msg.setVmUuid("vm-1"); msg.setAction(action); return msg;
        }
        void runQueued() {
            queued.run(() -> completed++);
        }
    }

}
