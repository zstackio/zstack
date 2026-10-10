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
            @Override public MemoryPolicyInventory getPolicy(String scope, String resource) {
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
                MemoryStateVO state = new MemoryStateVO(); state.setPermitAuthorized(true);
                state.setStatus("Succeeded"); state.setDesiredRevision(1L); state.setAppliedRevision(1L);
                return Collections.singletonList(state);
            }
            @Override public MemoryPolicyInventory getPolicy(String scope, String resource) {
                MemoryPolicyInventory result = new MemoryPolicyInventory();
                result.setPolicy("{\"participation\":\"deny\"}"); return result;
            }
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
            String value = MemoryPolicyRules.decode(getPolicy("VM", uuid).getPolicy()).participation;
            return value == null ? "inherit" : value;
        }
        MemoryMigrationVO hold;
        private final EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(), new Class[]{EntityManager.class}, (proxy, method, args) -> {
                    if ("find".equals(method.getName()) && args != null && args.length >= 2
                            && args[0] == MemoryMigrationVO.class) { return hold; }
                    if ("merge".equals(method.getName())) { hold = (MemoryMigrationVO) args[0]; return hold; }
                    if ("flush".equals(method.getName())) { return null; }
                    if (method.getReturnType().isPrimitive()) {
                        if (method.getReturnType() == boolean.class) return false;
                        if (method.getReturnType() == int.class) return 0;
                    }
                    return null;
                });
        @Override protected <T> T transaction(java.util.function.Function<EntityManager, T> function) {
            return function.apply(entityManager);
        }
        @Override public MemoryPolicyInventory getPolicy(String scope, String resource) {
            MemoryPolicyInventory policy = new MemoryPolicyInventory();
            policy.setScope(scope); policy.setResourceUuid(resource);
            policy.setPolicy("{\"schemaVersion\":1,\"participation\":\"inherit\"}");
            return policy;
        }
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
