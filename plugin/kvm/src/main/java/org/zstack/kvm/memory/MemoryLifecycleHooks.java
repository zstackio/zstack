package org.zstack.kvm.memory;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.persistence.LockModeType;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Never changes CPU controller, multipath, VM XML or the migration data path. */
public class MemoryLifecycleHooks implements VmInstanceMigrateExtensionPoint, HostDeleteExtensionPoint,
        VmInstanceStartExtensionPoint, VmInstanceStopExtensionPoint, VmInstanceDestroyExtensionPoint,
        VmJustAfterDeleteFromDbExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MemoryLifecycleHooks.class);
    @Autowired private MemoryRepository repository;
    @Autowired private MemoryOptimizationManager manager;
    private final Map<String, String> lifecycleGenerations = new ConcurrentHashMap<>();
    private final MemoryBatchCursor bindingCursor = new MemoryBatchCursor();

    private MemoryStateVO state(String host) { return repository.state(host); }

    private static Map<?, ?> capabilities(MemoryAgentResponse response) {
        Object value = response == null || response.state == null ? null : response.state.get("capabilities");
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

    /** Only a service-owned ZRAM pool creates a migration hold. KSM-only state does not. */
    private boolean managedZram(String host) {
        MemoryStateVO state = state(host);
        return state != null && hasPoolGeneration(state.getState());
    }

    private void record(MemoryMigrationVO hold) {
        repository.transaction(em -> {
            MemoryMigrationVO existing = em.find(MemoryMigrationVO.class, hold.vmUuid, LockModeType.PESSIMISTIC_WRITE);
            if (existing != null && !Objects.equals(existing.operationUuid, hold.operationUuid)) {
                return null;
            }
            em.merge(hold); em.flush(); return null;
        });
    }

    private boolean reserve(MemoryMigrationVO hold) {
        return repository.transaction(em -> {
            // Lock the host policy/state first. A row-level lock on a missing
            // migration record cannot serialize two first-time reservations.
            em.find(MemoryStateVO.class, hold.sourceHostUuid, LockModeType.PESSIMISTIC_WRITE);
            MemoryMigrationVO existing = em.find(MemoryMigrationVO.class, hold.vmUuid, LockModeType.PESSIMISTIC_WRITE);
            if (existing != null && !"Released".equals(existing.status)) { return false; }
            em.merge(hold); em.flush(); return true;
        });
    }

    static boolean hasPoolGeneration(String raw) {
        if (raw == null || raw.trim().isEmpty()) { return false; }
        com.google.gson.JsonElement element = MemoryObservationRules.readJson(raw, com.google.gson.JsonElement.class,
                "memory pool generation is unreadable");
        return element != null && hasPoolGeneration(element);
    }

    private static boolean hasPoolGeneration(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) { return false; }
        if (element.isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                if (("poolGeneration".equals(key) || "pool_generation".equals(key))
                        && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()
                        && !entry.getValue().getAsString().trim().isEmpty()) {
                    return true;
                }
                if (hasPoolGeneration(entry.getValue())) { return true; }
            }
        } else if (element.isJsonArray()) {
            for (com.google.gson.JsonElement child : element.getAsJsonArray()) {
                if (hasPoolGeneration(child)) { return true; }
            }
        }
        return false;
    }

    @Override public void preMigrateVm(VmInstanceInventory vm, String target, Completion completion) {
        if (!managedZram(vm.getHostUuid())) { completion.success(); return; }
        MemoryMigrationVO hold = new MemoryMigrationVO();
        hold.vmUuid = vm.getUuid(); hold.operationUuid = Platform.getUuid();
        hold.sourceHostUuid = vm.getHostUuid(); hold.targetHostUuid = target; hold.status = "Preparing";
        if (!reserve(hold)) {
            completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "Resolve the previous migration hold first")); return;
        }
        Map<String, Object> query = command(hold);
        manager.call(hold.sourceHostUuid, "state", query, response -> {
            // A VM entry in a partial inventory is not proof that the source
            // identity is current.  The Agent's inventoryComplete flag means
            // libvirt enumeration finished; it deliberately does not mean
            // every VM identity was available.  The per-VM generation check
            // below therefore remains mandatory as a second gate.
            String pool = inventoryComplete(response)
                    ? stateString(response, "poolGeneration", "pool_generation") : null;
            String instance = instanceGeneration(response, hold.vmUuid);
            if (pool == null || instance == null) {
                hold.status = "Unknown"; hold.reason = "Source pool identity cannot be confirmed"; record(hold);
                completion.fail(error("MEMORY_RESULT_UNKNOWN", hold.reason)); return;
            }
            hold.poolGeneration = pool; hold.sourceInstanceGeneration = instance; record(hold);
            hold(hold, "acquire", acquired -> {
                if (acquired) { completion.success(); }
                else { completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "Source active operations were not confirmed held")); }
            });
        });
    }

    private Map<String, Object> command(MemoryMigrationVO hold) {
        return command(hold, hold.sourceHostUuid);
    }

    private Map<String, Object> command(MemoryMigrationVO hold, String host) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("hostUuid", host); command.put("operationUuid", hold.operationUuid);
        command.put("resourceUuid", hold.vmUuid); return command;
    }

    /** Attempts at a hold while the Host answers that another change is still in progress. */
    private static final int HOLD_ATTEMPTS = 6;

    private void hold(MemoryMigrationVO hold, String action, Consumer<Boolean> completion) {
        hold(hold, action, 1, completion);
    }

    private void hold(MemoryMigrationVO hold, String action, int attempt, Consumer<Boolean> completion) {
        Map<String, Object> command = command(hold);
        command.put("holdAction", action); command.put("poolGeneration", hold.poolGeneration);
        manager.call(hold.sourceHostUuid, "migration-hold", command, response -> {
            if (response != null && response.isSuccess() && "Converging".equals(response.status) && attempt < HOLD_ATTEMPTS) {
                manager.later(() -> hold(hold, action, attempt + 1, completion),
                        response.retryAfterSeconds == null ? 5 : Math.max(1, response.retryAfterSeconds));
                return;
            }
            boolean confirmed = MemoryResultRules.succeeded(hold.operationUuid, response);
            hold.status = confirmed ? ("acquire".equals(action) ? "Held" : "Released") : "Unknown";
            hold.reason = confirmed ? null : "Migration hold result must be reconciled";
            record(hold); completion.accept(confirmed);
        });
    }

    @Override public void beforeMigrateVm(VmInstanceInventory vm, String target) { }

    @Override public void afterMigrateVm(VmInstanceInventory vm, String source, NoErrorCompletion completion) {
        releaseAfterIdentityCheck(vm.getUuid(), MigrationResult.MIGRATED, completion);
    }

    @Override public void failedToMigrateVm(VmInstanceInventory vm, String target, ErrorCode error, NoErrorCompletion completion) {
        releaseAfterIdentityCheck(vm.getUuid(), MigrationResult.FAILED, completion);
    }

    /** Where the platform's migration left the VM: on the target, or still on the source. */
    private enum MigrationResult { MIGRATED, FAILED }

    /** The source or target Host of a migration hold. */
    private enum Side { SOURCE, TARGET }

    private void releaseAfterIdentityCheck(String vm, MigrationResult result, NoErrorCompletion completion) {
        MemoryMigrationVO hold = repository.transaction(em ->
                em.find(MemoryMigrationVO.class, vm, LockModeType.PESSIMISTIC_WRITE));
        if (hold == null || "Released".equals(hold.status)) { completion.done(); return; }
        verifyMigrationIdentities(hold, result, safe -> {
            if (!safe) { completion.done(); return; }
            if (result == MigrationResult.MIGRATED) {
                rebind(hold, Side.TARGET, rebound -> {
                    if (!rebound) { completion.done(); return; }
                    revoke(hold, revoked -> { if (!revoked) { completion.done(); return; } hold(hold, "release", ignored -> completion.done()); });
                });
            } else {
                // The source VM is still the verified instance after a failed
                // migration. Restore its VM binding; revoking it here would
                // silently drop optimization for the still-running VM.
                rebind(hold, Side.SOURCE, rebound -> {
                    if (rebound) { hold(hold, "release", ignored -> completion.done()); }
                    else { completion.done(); }
                });
            }
        });
    }

    private void verifyMigrationIdentities(MemoryMigrationVO hold, MigrationResult result, Consumer<Boolean> completion) {
        inspectMigrationIdentities(hold, (source, target) -> {
            if (target.present) {
                hold.targetInstanceGeneration = target.generation;
                record(hold);
            }
            boolean sourceExpected = source.present && hold.sourceInstanceGeneration != null
                    && hold.sourceInstanceGeneration.equals(source.generation);
            boolean targetExpected = targetExpected(hold, target);
            boolean sourceAbsent = source.authoritative && !source.present;
            boolean targetAbsent = target.authoritative && !target.present;
            boolean safe = result == MigrationResult.MIGRATED ? targetExpected && sourceAbsent : sourceExpected && targetAbsent;
            if (!safe) { hold.status = "Unknown"; hold.reason = "Source/target VM instance identity was not conclusively verified"; record(hold); }
            completion.accept(safe);
        });
    }

    /**
     * The migrated instance is on the target. Its generation, given by the target's
     * ZRAM service, is needed only to bind it; a target without a service-owned pool
     * has nothing to bind, even when its service is not running.
     */
    private boolean targetExpected(MemoryMigrationVO hold, VmIdentity target) {
        return target.present && (target.generation != null || target.featureAbsent
                || !managedZram(hold.targetHostUuid));
    }

    /** What a Host's VM inventory proves about one VM: nothing, its absence or its presence. */
    private enum Presence { UNKNOWN, ABSENT, PRESENT }

    private static final class VmIdentity {
        final boolean authoritative;
        final boolean present;
        final String generation;
        /** The inventory says the Host has no ZRAM to bind the VM to. */
        final boolean featureAbsent;
        VmIdentity(Presence presence, String generation, MemoryAgentResponse inventory) {
            this.authoritative = presence != Presence.UNKNOWN;
            this.present = presence == Presence.PRESENT;
            this.generation = generation;
            this.featureAbsent = inventory != null && targetHasNoZram(inventory);
        }
    }

    private void inspectMigrationIdentities(MemoryMigrationVO hold, BiConsumer<VmIdentity, VmIdentity> completion) {
        manager.call(hold.sourceHostUuid, "state", command(hold, hold.sourceHostUuid), source -> {
            VmIdentity sourceIdentity = vmIdentity(source, hold.vmUuid);
            manager.call(hold.targetHostUuid, "state", command(hold, hold.targetHostUuid), target -> {
                completion.accept(sourceIdentity, vmIdentity(target, hold.vmUuid));
            });
        });
    }

    private void rebind(MemoryMigrationVO hold, Side side, Consumer<Boolean> completion) {
        boolean target = side == Side.TARGET;
        Map<String, Object> command = command(hold, target ? hold.targetHostUuid : hold.sourceHostUuid);
        command.put("action", "bind"); command.put("scope", "VM");
        command.put("expectedInstanceGeneration", target && hold.targetInstanceGeneration != null
                ? hold.targetInstanceGeneration : hold.sourceInstanceGeneration);
        String host = target ? hold.targetHostUuid : hold.sourceHostUuid;
        if (target && !managedZram(host)) { completion.accept(true); return; }
        if (target) {
            manager.call(host, "state", command(hold, host), response -> {
                if (capabilities(response) != null && Boolean.FALSE.equals(capabilities(response).get("service"))
                        && Boolean.FALSE.equals(response.state.get("managed"))) {
                    // A destination without this feature needs no optimizer
                    // binding. Native VM identity was already verified above.
                    completion.accept(true);
                } else if (targetHasNoZram(response)) {
                    // KSM-only/feature-absent targets have no native ZRAM
                    // binding. An explicit capability is required here;
                    // service failure must not be mistaken for no ZRAM.
                    completion.accept(true);
                } else {
                    manager.call(host, "vm-efficiency", command, result -> completion.accept(
                            MemoryResultRules.succeeded(hold.operationUuid, result)));
                }
            });
            return;
        }
        manager.call(host, "vm-efficiency", command,
                response -> completion.accept(MemoryResultRules.succeeded(hold.operationUuid, response)));
    }

    /** Bind existing/rerun VMs only after a confirmed Host policy and fresh inventory. */
    void reconcileObservedBindings(String host, MemoryAgentResponse response) {
        MemoryStateVO state = state(host);
        if (state == null || state.getAppliedRevision() == null
                || state.getAppliedRevision() != state.getDesiredRevision()
                || !"Succeeded".equals(state.getStatus())
                || response == null || !response.isSuccess() || response.state == null
                || !Boolean.TRUE.equals(response.state.get("inventoryComplete"))
                || !(response.state.get("vms") instanceof Map)) { return; }
        Map<?, ?> vms = (Map<?, ?>) response.state.get("vms");
        Map<String, Map<String, Object>> candidates = new LinkedHashMap<>();
        for (Map.Entry<?, ?> item : vms.entrySet()) {
            if (!(item.getKey() instanceof String) || !(item.getValue() instanceof Map)) { continue; }
            String uuid = (String) item.getKey();
            String generation = instanceGeneration(response, uuid);
            if (generation == null) { continue; }
            Map<?, ?> vm = (Map<?, ?>) item.getValue();
            // The service reports its binding as binding_generation and binding_mode.
            if (generation.equals(vm.get("binding_generation")) && "auto".equals(vm.get("binding_mode"))) { continue; }
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("hostUuid", host); command.put("operationUuid", UUID.randomUUID().toString().replace("-", ""));
            command.put("resourceUuid", uuid); command.put("scope", "VM"); command.put("action", "bind");
            command.put("expectedInstanceGeneration", generation);
            candidates.put(uuid, command);
        }
        List<String> batch = new ArrayList<>(bindingCursor.next(host, candidates.keySet(),
                MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.VM_BIND_BATCH_SIZE, 32)));
        Round round = new Round(String.format("Host[%s] VM memory optimization bindings", host), batch.size());
        for (String uuid : batch) {
            Map<String, Object> command = candidates.get(uuid);
            String operation = (String) command.get("operationUuid");
            // Advance even on a failure; the next authoritative sample binds it again.
            manager.call(host, "vm-efficiency", command, reply -> round.finished(uuid,
                    MemoryResultRules.succeeded(operation, reply) ? null : String.format("%s (operation %s)",
                            reply == null ? "AGENT_UNREACHABLE" : reply.reasonCode, operation)));
        }
    }

    /** One round of Agent calls; its failures are logged once, as an error, when the last call returns. */
    static final class Round {
        private final String what;
        private final AtomicInteger pending;
        private final Map<String, String> failures = new ConcurrentHashMap<>();
        private final Consumer<String> errors;

        Round(String what, int calls) { this(what, calls, logger::error); }

        Round(String what, int calls, Consumer<String> errors) {
            this.what = what; this.pending = new AtomicInteger(calls); this.errors = errors;
        }

        void finished(String resource, String failure) {
            if (failure != null) { failures.put(resource, failure); }
            if (pending.decrementAndGet() == 0 && !failures.isEmpty()) {
                errors.accept(String.format("%s: %d failed and are tried again: %s",
                        what, failures.size(), new TreeMap<>(failures)));
            }
        }
    }

    private void revoke(MemoryMigrationVO hold, Consumer<Boolean> completion) {
        Map<String, Object> command = command(hold);
        command.put("action", "revoke"); command.put("scope", "VM");
        command.put("expectedInstanceGeneration", hold.sourceInstanceGeneration);
        manager.call(hold.sourceHostUuid, "vm-efficiency", command,
                response -> completion.accept(MemoryResultRules.succeeded(hold.operationUuid, response)));
    }

    private static String stateString(MemoryAgentResponse response, String... keys) {
        if (response == null || response.state == null) { return null; }
        for (String key : keys) {
            String value = findString(response.state, key);
            if (value != null) { return value; }
        }
        return null;
    }

    private static String findString(Object value, String key) {
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (key.equals(entry.getKey()) && entry.getValue() instanceof String
                        && !((String) entry.getValue()).trim().isEmpty()) {
                    return (String) entry.getValue();
                }
                String nested = findString(entry.getValue(), key);
                if (nested != null) { return nested; }
            }
        } else if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                String nested = findString(item, key);
                if (nested != null) { return nested; }
            }
        }
        return null;
    }

    static String instanceGeneration(MemoryAgentResponse response, String vm) {
        if (response == null || response.state == null || !(response.state.get("vms") instanceof Map)) { return null; }
        Object value = ((Map<?, ?>) response.state.get("vms")).get(vm);
        if (!(value instanceof Map)) { return null; }
        Object generation = ((Map<?, ?>) value).get("instanceGeneration");
        if (generation == null) { generation = ((Map<?, ?>) value).get("instance_generation"); }
        return generation instanceof String && !((String) generation).trim().isEmpty() ? (String) generation : null;
    }

    static boolean inventoryComplete(MemoryAgentResponse response) {
        return response != null && response.state != null
                && Boolean.TRUE.equals(response.state.get("inventoryComplete"))
                && response.state.get("vms") instanceof Map;
    }

    private static VmIdentity vmIdentity(MemoryAgentResponse response, String vm) {
        if (response == null || response.state == null) { return new VmIdentity(Presence.UNKNOWN, null, null); }
        Object raw = response.state.get("vms");
        // An empty vms map is only proof of absence when the Agent explicitly
        // declares that the inventory is complete. A partial/failed snapshot
        // must remain Unknown.
        if (!(raw instanceof Map) || !Boolean.TRUE.equals(response.state.get("inventoryComplete"))) {
            return new VmIdentity(Presence.UNKNOWN, null, null);
        }
        Object value = ((Map<?, ?>) raw).get(vm);
        if (!(value instanceof Map)) { return new VmIdentity(Presence.ABSENT, null, response); }
        Object generation = ((Map<?, ?>) value).get("instanceGeneration");
        if (generation == null) { generation = ((Map<?, ?>) value).get("instance_generation"); }
        return new VmIdentity(Presence.PRESENT, generation instanceof String && !((String) generation).trim().isEmpty()
                ? (String) generation : null, response);
    }

    private static boolean targetHasNoZram(MemoryAgentResponse response) {
        Map<?, ?> capabilities = capabilities(response);
        if (capabilities == null || !Boolean.FALSE.equals(capabilities.get("zram"))) { return false; }
        // Agent discover_capabilities reports zram=false for both a known
        // unsupported platform and an unavailable/old service. Only the
        // former is an authoritative no-ZRAM result. In particular,
        // SERVICE_RECLAIM_CAPABILITY_UNKNOWN must remain Unknown when the
        // target still has a configured ZRAM service.
        Object rawReason = capabilities.get("zramReasonCode");
        if (!(rawReason instanceof String)) { return false; }
        String reason = (String) rawReason;
        return "UNSUPPORTED_ARCHITECTURE".equals(reason)
                || "UNSUPPORTED_OS_RELEASE".equals(reason)
                || "UNSUPPORTED_PAGE_SIZE".equals(reason)
                || "UNSUPPORTED_RECLAIM_KERNEL".equals(reason)
                || "ABI_INCOMPLETE".equals(reason);
    }

    /** Host removal is never blocked: an offline or old-Agent Host cannot prove a drain. */
    @Override public void preDeleteHost(HostInventory host) throws HostException {
        MemoryStateVO state = state(host.getUuid());
        if (state != null && hasPoolGeneration(state.getState())) {
            logger.warn(String.format("deleting Host[%s] with memory optimization state %s; its local ZRAM pool and"
                    + " service state remain on the Host until it is drained or reinstalled", host.getUuid(), state.getStatus()));
        }
    }
    @Override public void beforeDeleteHost(HostInventory host) { }
    // HostBase calls this before HostCascadeExtension removes HostVO. Cleanup
    // belongs to the cascade CLEANUP phase, not this misleadingly named hook.
    @Override public void afterDeleteHost(HostInventory host) { }

    @Override public void vmJustAfterDeleteFromDbExtensionPoint(VmInstanceInventory vm, String accountUuid) {
        lifecycleGenerations.remove(vm.getUuid());
        repository.cleanupDeletedResources();
    }

    /* VM lifecycle hooks are idempotent Agent binding/revocation notifications. */
    @Override public String preStartVm(VmInstanceInventory inv) { return null; }
    @Override public void beforeStartVm(VmInstanceInventory inv) { }
    @Override public void afterStartVm(VmInstanceInventory inv) { safely(inv, () -> lifecycleBinding(inv, "bind")); }
    @Override public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) { safely(inv, () -> lifecycleBinding(inv, "revoke")); }
    @Override public String preStopVm(VmInstanceInventory inv) { return null; }
    @Override public void beforeStopVm(VmInstanceInventory inv) { safely(inv, () -> snapshotLifecycleGeneration(inv)); }
    @Override public void afterStopVm(VmInstanceInventory inv) { safely(inv, () -> lifecycleBinding(inv, "revoke")); }
    @Override public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) { }
    @Override public void afterFailedToStopVm(VmInstanceInventory inv, ErrorCode reason) { }
    @Override public boolean needStopBeforeDestroy(VmInstanceInventory inv) { return false; }
    @Override public String preDestroyVm(VmInstanceInventory inv) { return null; }
    @Override public void beforeDestroyVm(VmInstanceInventory inv) { }
    @Override public void afterDestroyVm(VmInstanceInventory inv) { safely(inv, () -> lifecycleBinding(inv, "revoke")); }
    @Override public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) { }

    /** The VM operation does not depend on its memory binding; the next observation binds it again. */
    @ExceptionSafe @Deferred
    static void safely(VmInstanceInventory inv, Runnable binding) {
        Defer.guard(() -> logger.error(String.format(
                "VM[%s] memory optimization binding was not updated; the next observation binds it again", inv.getUuid())));
        binding.run();
    }

    private void lifecycleBinding(VmInstanceInventory inv, String action) {
        String host = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();
        if (host == null) { return; }
        String operation = "vm-lifecycle-" + action + "-" + inv.getUuid();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("hostUuid", host); query.put("operationUuid", operation);
        query.put("resourceUuid", inv.getUuid());
        // A failed notification is logged with its VM, Host and operation; the next observation binds again.
        Round round = new Round(String.format("VM memory optimization %s on Host[%s] (operation %s)",
                action, host, operation), 1);
        manager.call(host, "state", query, response -> {
            // VM-scoped Agent mutations are generation guarded. If the VM is no
            // longer visible, retain the uncertainty instead of revoking a
            // possible replacement VM with the same UUID.
            VmIdentity identity = vmIdentity(response, inv.getUuid());
            String generation = identity.present ? identity.generation : null;
            if (generation != null) {
                lifecycleGenerations.put(inv.getUuid(), generation);
            } else if ("revoke".equals(action) && identity.authoritative) {
                generation = lifecycleGenerations.get(inv.getUuid());
            }
            if (generation == null) {
                // A VM proven absent with no generation left has nothing to revoke.
                boolean nothingToRevoke = "revoke".equals(action) && identity.authoritative;
                round.finished(inv.getUuid(), nothingToRevoke ? null : identity.authoritative
                        ? "VM_INSTANCE_NOT_FOUND" : "VM_IDENTITY_UNKNOWN: " + MemoryOptimizationManager.failure(response));
                return;
            }
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("hostUuid", host); command.put("operationUuid", operation);
            command.put("resourceUuid", inv.getUuid()); command.put("scope", "VM");
            command.put("action", action); command.put("expectedInstanceGeneration", generation);
            String expected = generation;
            manager.call(host, "vm-efficiency", command, reply -> {
                boolean succeeded = MemoryResultRules.succeeded(operation, reply);
                // A failed revoke keeps the generation, so the next stop or destroy revokes it again.
                if ("revoke".equals(action) && succeeded) {
                    lifecycleGenerations.remove(inv.getUuid(), expected);
                }
                round.finished(inv.getUuid(), succeeded ? null : MemoryOptimizationManager.failure(reply));
            });
        });
    }

    private void snapshotLifecycleGeneration(VmInstanceInventory inv) {
        String host = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();
        if (host == null) { return; }
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("hostUuid", host); query.put("operationUuid", "vm-lifecycle-snapshot-" + inv.getUuid());
        query.put("resourceUuid", inv.getUuid());
        Round round = new Round(String.format("VM memory optimization generation before stop on Host[%s]", host), 1);
        manager.call(host, "state", query, response -> {
            VmIdentity identity = vmIdentity(response, inv.getUuid());
            if (identity.present && identity.generation != null) {
                lifecycleGenerations.put(inv.getUuid(), identity.generation);
            }
            // Without it the revoke after the stop has no generation to use.
            round.finished(inv.getUuid(), identity.present && identity.generation != null ? null
                    : identity.authoritative ? "VM_INSTANCE_NOT_FOUND"
                    : "VM_IDENTITY_UNKNOWN: " + MemoryOptimizationManager.failure(response));
        });
    }

    /** Unknown holds being reconciled on this management node, by hold operation. */
    private final Set<String> reconcilingHolds = ConcurrentHashMap.newKeySet();
    /** Preparing or Held holds whose VM is not migrating: when this node first saw each so, by source and operation. */
    private final Map<String, Long> settledHolds = new ConcurrentHashMap<>();
    /** The migration callback releases a hold seconds after its VM settles; one still held later was left behind. */
    private long settledHoldGraceMillis = TimeUnit.MINUTES.toMillis(5);

    /**
     * After a fresh observation of a migration source, reconcile each of its
     * Unknown holds; an unresolved hold otherwise blocks the VM's next migration.
     * A hold that cannot be proven yet is tried again at the next observation.
     */
    void reconcileUnknownMigrationHolds(String sourceHost) {
        // Also the hold of a deleted VM: no migration callback will ever release it.
        List<MemoryMigrationVO> holds = new ArrayList<>(repository.unknownOrOrphanedHolds(sourceHost));
        holds.addAll(settledHoldsLeftBehind(sourceHost));
        List<MemoryMigrationVO> started = new ArrayList<>();
        for (MemoryMigrationVO hold : holds) {
            if (reconcilingHolds.add(hold.operationUuid)) { started.add(hold); }
        }
        Round round = new Round(String.format("Host[%s] migration holds", sourceHost), started.size());
        for (MemoryMigrationVO hold : started) {
            reconcileMigrationHold(hold.vmUuid, new Completion(null) {
                @Override public void success() {
                    reconcilingHolds.remove(hold.operationUuid);
                    settledHolds.remove(sourceHost + "/" + hold.operationUuid);
                    logger.info(String.format("VM[%s] migration hold reconciled", hold.vmUuid));
                    round.finished(hold.vmUuid, null);
                }
                @Override public void fail(ErrorCode errorCode) {
                    reconcilingHolds.remove(hold.operationUuid);
                    round.finished(hold.vmUuid, String.format("hold %s unresolved: %s", hold.operationUuid, errorCode.getDetails()));
                }
            });
        }
    }

    /**
     * A management node that restarts during a migration leaves its Preparing or Held hold with no
     * callback to release it. The VM is Migrating for as long as any node migrates it, so a hold whose
     * VM has been out of Migrating for the grace period is reconciled like an Unknown one.
     */
    private List<MemoryMigrationVO> settledHoldsLeftBehind(String sourceHost) {
        List<MemoryMigrationVO> settled = repository.settledHolds(sourceHost);
        long now = System.currentTimeMillis();
        Set<String> current = new HashSet<>();
        List<MemoryMigrationVO> leftBehind = new ArrayList<>();
        for (MemoryMigrationVO hold : settled) {
            String key = sourceHost + "/" + hold.operationUuid;
            current.add(key);
            if (now - settledHolds.computeIfAbsent(key, ignored -> now) >= settledHoldGraceMillis) { leftBehind.add(hold); }
        }
        settledHolds.keySet().removeIf(key -> key.startsWith(sourceHost + "/") && !current.contains(key));
        return leftBehind;
    }

    private boolean vmStoppedOrDeleted(String vmUuid) {
        MemoryRepository.VmPlacement vm = repository.vmPlacement(vmUuid);
        return vm == null || vm.state == VmInstanceState.Stopped || vm.state == VmInstanceState.Destroyed;
    }

    /**
     * The operator's exit for a hold the automatic reconcile cannot prove: the VM
     * runs on confirmedHost (source or target), or is stopped when it is null.
     * The platform and both Hosts must agree before the hold is released.
     */
    void recoverMigrationHold(String vmUuid, String confirmedHost, Completion completion) {
        MemoryMigrationVO hold = repository.transaction(em -> em.find(MemoryMigrationVO.class, vmUuid));
        if (hold == null || "Released".equals(hold.status)) {
            completion.fail(error("MEMORY_INVALID_REQUEST", "VM has no unresolved memory migration hold")); return;
        }
        MemoryRepository.VmPlacement vm = repository.vmPlacement(vmUuid);
        VmInstanceState vmState = vm == null ? null : vm.state;
        String vmHost = vm == null ? null : vm.hostUuid;
        boolean running = confirmedHost != null;
        if (running && (!Arrays.asList(hold.sourceHostUuid, hold.targetHostUuid).contains(confirmedHost)
                || vmState != VmInstanceState.Running || !confirmedHost.equals(vmHost))
                || !running && vmState != VmInstanceState.Stopped) {
            completion.fail(error("MEMORY_INVALID_REQUEST", String.format("VM is %s on Host[%s]; confirm the migration"
                    + " source or target it runs on, or none for a stopped VM", vmState, vmHost))); return;
        }
        if (!reconcilingHolds.add(hold.operationUuid)) {
            completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "The hold is being reconciled; try again")); return;
        }
        Completion done = new Completion(completion) {
            @Override public void success() { reconcilingHolds.remove(hold.operationUuid); completion.success(); }
            @Override public void fail(ErrorCode errorCode) { reconcilingHolds.remove(hold.operationUuid); completion.fail(errorCode); }
        };
        startRecovery(hold, confirmedHost, done);
    }

    /*
     * Each recovery step takes the recovery's Completion: an exception in a step fails it
     * (AsyncSafeAspect), which frees the hold for a retry.
     */
    private void startRecovery(MemoryMigrationVO hold, String confirmedHost, Completion done) {
        locateVm(hold, (source, target) -> recoverLocated(hold, confirmedHost, source, target, done));
    }

    /** No confirmed Host means the operator confirms a stopped VM. */
    private void recoverLocated(MemoryMigrationVO hold, String confirmedHost,
                                VmIdentity source, VmIdentity target, Completion done) {
        boolean running = confirmedHost != null;
        // The side that is bound needs its instance generation; a target without a pool needs none.
        boolean onSource = source.present && source.generation != null
                && target.authoritative && !target.present;
        boolean onTarget = target.present && (target.generation != null || !managedZram(hold.targetHostUuid))
                && source.authoritative && !source.present;
        boolean nowhere = source.authoritative && !source.present && target.authoritative && !target.present;
        boolean confirmed = !running ? nowhere : confirmedHost.equals(hold.sourceHostUuid) ? onSource : onTarget;
        if (!confirmed) {
            done.fail(error("MEMORY_INVALID_REQUEST", String.format("Hosts do not show the VM only %s: source %s,"
                    + " target %s", running ? "on Host[" + confirmedHost + "]" : "stopped",
                    describe(source), describe(target))));
            return;
        }
        if (!running) { releaseRecoveredHold(hold, done); return; }
        if (confirmedHost.equals(hold.sourceHostUuid)) {
            hold.sourceInstanceGeneration = source.generation; record(hold);
            rebind(recoveryOperation(hold), Side.SOURCE, rebound -> {
                if (rebound) { releaseRecoveredHold(hold, done); } else { done.fail(unknown("Source VM rebinding")); }
            });
            return;
        }
        if (target.generation != null) { hold.targetInstanceGeneration = target.generation; record(hold); }
        MemoryMigrationVO operation = recoveryOperation(hold);
        // A target without a service-owned pool has nothing to bind.
        if (managedZram(hold.targetHostUuid)) {
            rebind(operation, Side.TARGET, rebound -> {
                if (rebound) { targetRebound(hold, operation, done); } else { done.fail(unknown("Target VM binding")); }
            });
        } else {
            targetRebound(hold, operation, done);
        }
    }

    private void targetRebound(MemoryMigrationVO hold, MemoryMigrationVO operation, Completion done) {
        if (!hostExists(hold.sourceHostUuid)) { releaseRecoveredHold(hold, done); return; }
        revoke(operation, revoked -> {
            if (revoked) { releaseRecoveredHold(hold, done); } else { done.fail(unknown("Source VM revocation")); }
        });
    }

    private ErrorCode unknown(String step) {
        return error("MEMORY_RESULT_UNKNOWN", step + " remains unknown");
    }

    /**
     * The bind and revoke of a recovery or reconcile are new Agent operations: the hold's own
     * UUID may already be journaled for the migration's bind. Only the release uses it.
     */
    private static MemoryMigrationVO recoveryOperation(MemoryMigrationVO hold) {
        MemoryMigrationVO operation = new MemoryMigrationVO();
        operation.vmUuid = hold.vmUuid; operation.operationUuid = Platform.getUuid();
        operation.sourceHostUuid = hold.sourceHostUuid; operation.targetHostUuid = hold.targetHostUuid;
        operation.poolGeneration = hold.poolGeneration;
        operation.sourceInstanceGeneration = hold.sourceInstanceGeneration;
        operation.targetInstanceGeneration = hold.targetInstanceGeneration;
        operation.status = hold.status;
        return operation;
    }

    /** A deleted Host has no VM; any other Host answers from its Agent inventory. */
    private void locateVm(MemoryMigrationVO hold, BiConsumer<VmIdentity, VmIdentity> completion) {
        identityOn(hold, hold.sourceHostUuid, source -> identityOn(hold, hold.targetHostUuid,
                target -> completion.accept(source, target)));
    }

    private void identityOn(MemoryMigrationVO hold, String host, Consumer<VmIdentity> completion) {
        if (!hostExists(host)) { completion.accept(new VmIdentity(Presence.ABSENT, null, null)); return; }
        manager.call(host, "state", command(hold, host), response -> completion.accept(vmIdentity(response, hold.vmUuid)));
    }

    private boolean hostExists(String host) {
        return repository.hostExists(host);
    }

    private static String describe(VmIdentity identity) {
        return !identity.authoritative ? "unknown" : identity.present ? "has it" : "does not have it";
    }

    /** No pool generation means the source acquire was never sent; otherwise the source service releases it. */
    private void releaseRecoveredHold(MemoryMigrationVO hold, Completion completion) {
        if (hold.poolGeneration == null || !hostExists(hold.sourceHostUuid)) {
            hold.status = "Released"; hold.reason = "Released by operator recovery"; record(hold);
            completion.success();
            return;
        }
        hold(hold, "release", released -> {
            if (released) { completion.success(); }
            else { completion.fail(error("MEMORY_RESULT_UNKNOWN", "Source migration hold release remains unknown")); }
        });
    }

    /** Never retries a hold without identity evidence. */
    void reconcileMigrationHold(String vmUuid, Completion completion) {
        MemoryMigrationVO hold = repository.transaction(em -> em.find(MemoryMigrationVO.class, vmUuid, LockModeType.PESSIMISTIC_WRITE));
        if (hold == null || "Released".equals(hold.status)) { completion.success(); return; }
        // No pool/instance identity means preflight failed before native
        // acquire was sent. It is safe to close only when authoritative
        // inventories and the platform still place the VM on the source.
        final boolean acquireNotSent = hold.poolGeneration == null && hold.sourceInstanceGeneration == null;
        inspectMigrationIdentities(hold, (source, target) -> {
            boolean sourceExpected = source.present && hold.sourceInstanceGeneration != null
                    && hold.sourceInstanceGeneration.equals(source.generation);
            boolean targetExpected = targetExpected(hold, target);
            boolean sourceAbsent = source.authoritative && !source.present;
            boolean targetAbsent = target.authoritative && !target.present;
            if (acquireNotSent && source.present && targetAbsent) {
                List<String> currentHosts = repository.targets("VM", vmUuid);
                if (currentHosts.size() == 1 && hold.sourceHostUuid.equals(currentHosts.get(0))) {
                    hold.status = "Released";
                    hold.reason = "Migration preflight failed before native hold acquire";
                    record(hold);
                    completion.success();
                    return;
                }
            }
            if (target.present) {
                hold.targetInstanceGeneration = target.generation;
                record(hold);
            }
            if (sourceExpected && targetAbsent) {
                // Reconcile found the original source instance still running;
                // restore its binding rather than revoking the VM that must
                // continue to be optimized.
                rebind(recoveryOperation(hold), Side.SOURCE, rebound -> {
                    if (!rebound) {
                        completion.fail(error("MEMORY_RESULT_UNKNOWN", "Source VM rebinding remains unknown"));
                        return;
                    }
                    hold(hold, "release", released -> {
                        if (released) completion.success();
                        else completion.fail(error("MEMORY_RESULT_UNKNOWN", "Migration hold release remains unknown"));
                    });
                });
            } else if (targetExpected && sourceAbsent) {
                // The target is the only VM instance. Rebind it before removing
                // the source hold; an unknown/both-present state stays held.
                MemoryMigrationVO operation = recoveryOperation(hold);
                rebind(operation, Side.TARGET, rebound -> {
                    if (!rebound) { completion.fail(error("MEMORY_RESULT_UNKNOWN", "Target VM binding remains unknown")); return; }
                    revoke(operation, revoked -> {
                        if (!revoked) { completion.fail(error("MEMORY_RESULT_UNKNOWN", "Source VM revocation remains unknown")); return; }
                        hold(hold, "release", released -> {
                            if (released) completion.success();
                            else completion.fail(error("MEMORY_RESULT_UNKNOWN", "Migration hold release remains unknown"));
                        });
                    });
                });
            } else if (sourceAbsent && targetAbsent && vmStoppedOrDeleted(vmUuid)) {
                // No instance is left to protect; the source only needs its hold released.
                hold(hold, "release", released -> {
                    if (released) completion.success();
                    else completion.fail(error("MEMORY_RESULT_UNKNOWN", "Migration hold release remains unknown"));
                });
            } else {
                hold.status = "Unknown";
                hold.reason = "Migration reconcile found zero or multiple VM identities";
                record(hold);
                completion.fail(error("MEMORY_RESULT_UNKNOWN", hold.reason));
            }
        });
    }

    private ErrorCode error(String code, String message) { return new MemoryOperationException(code, message).toErrorCode(); }
}
