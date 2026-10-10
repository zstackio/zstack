package org.zstack.kvm.memory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.vm.*;
import javax.persistence.LockModeType;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;

/** Never changes CPU controller, multipath, VM XML or the migration data path. */
public class MemoryLifecycleHooks implements VmInstanceMigrateExtensionPoint, HostDeleteExtensionPoint,
        VmInstanceStartExtensionPoint, VmInstanceStopExtensionPoint, VmInstanceDestroyExtensionPoint,
        VmJustAfterDeleteFromDbExtensionPoint {
    @Autowired private MemoryRepository repository;
    @Autowired private MemoryOptimizationManager manager;
    private final Map<String, String> lifecycleGenerations = new ConcurrentHashMap<>();
    private final MemoryBatchCursor bindingCursor = new MemoryBatchCursor();

    private MemoryStateVO state(String host) {
        List<MemoryStateVO> states = repository.states(Collections.singletonList(host), 0, 1);
        return states.isEmpty() ? null : states.get(0);
    }

    /** Only a service-owned ZRAM pool creates a migration hold. KSM-only state does not. */
    private boolean managedZram(String host) {
        MemoryStateVO state = state(host);
        return state != null && hasPoolGeneration(state.getState());
    }

    /** Host deletion must drain any owned service, including KSM-only mode. */
    private boolean managedForDelete(MemoryStateVO state) {
        if (state == null) { return false; }
        if (state.getControlOperationUuid() != null || state.getDesiredRevision() > 0
                || hasPoolGeneration(state.getState())) { return true; }
        if (state.getState() == null) { return false; }
        try {
            com.google.gson.JsonObject json = new JsonParser().parse(state.getState()).getAsJsonObject();
            return json.has("managed") && json.get("managed").getAsBoolean();
        } catch (RuntimeException ignored) { return false; }
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
        try { return hasPoolGeneration(new JsonParser().parse(raw)); }
        catch (RuntimeException ignored) { return false; }
    }

    private static boolean hasPoolGeneration(com.google.gson.JsonElement element) {
        if (element == null || element.isJsonNull()) { return false; }
        if (element.isJsonObject()) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : element.getAsJsonObject().entrySet()) {
                String key = entry.getKey();
                if (("poolGeneration".equals(key) || "pool_generation".equals(key))
                        && entry.getValue().isJsonPrimitive() && !entry.getValue().getAsString().trim().isEmpty()) {
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
        try { repository.captureMigrationParticipation(vm.getUuid(), vm.getHostUuid(), target); }
        catch (RuntimeException e) {
            completion.fail(error("MEMORY_PARTICIPATION_UNCONFIRMED", "Cannot preserve VM migration participation")); return;
        }
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

    private void hold(MemoryMigrationVO hold, String action, Consumer<Boolean> completion) {
        Map<String, Object> command = command(hold);
        command.put("holdAction", action); command.put("poolGeneration", hold.poolGeneration);
        manager.call(hold.sourceHostUuid, "migration-hold", command, response -> {
            boolean confirmed = "Succeeded".equals(MemoryResultRules.status(hold.operationUuid, -1, response));
            hold.status = confirmed ? ("acquire".equals(action) ? "Held" : "Released") : "Unknown";
            hold.reason = confirmed ? null : "Migration hold result must be reconciled";
            record(hold); completion.accept(confirmed);
        });
    }

    @Override public void beforeMigrateVm(VmInstanceInventory vm, String target) { }

    @Override public void afterMigrateVm(VmInstanceInventory vm, String source, NoErrorCompletion completion) {
        releaseAfterIdentityCheck(vm.getUuid(), true, completion);
    }

    @Override public void failedToMigrateVm(VmInstanceInventory vm, String target, ErrorCode error, NoErrorCompletion completion) {
        repository.abortMigrationParticipation(vm.getUuid());
        releaseAfterIdentityCheck(vm.getUuid(), false, completion);
    }

    private void releaseAfterIdentityCheck(String vm, boolean migrated, NoErrorCompletion completion) {
        MemoryMigrationVO hold = repository.transaction(em ->
                em.find(MemoryMigrationVO.class, vm, LockModeType.PESSIMISTIC_WRITE));
        if (hold == null || "Released".equals(hold.status)) { completion.done(); return; }
        verifyMigrationIdentities(hold, migrated, safe -> {
            if (!safe) { completion.done(); return; }
            if (migrated) {
                rebind(hold, true, rebound -> {
                    if (!rebound) { completion.done(); return; }
                    revoke(hold, revoked -> { if (!revoked) { completion.done(); return; } hold(hold, "release", ignored -> completion.done()); });
                });
            } else {
                // The source VM is still the verified instance after a failed
                // migration. Restore its VM binding; revoking it here would
                // silently drop optimization for the still-running VM.
                rebind(hold, false, rebound -> {
                    if (rebound) { hold(hold, "release", ignored -> completion.done()); }
                    else { completion.done(); }
                });
            }
        });
    }

    private void verifyMigrationIdentities(MemoryMigrationVO hold, boolean migrated, Consumer<Boolean> completion) {
        inspectMigrationIdentities(hold, (source, target) -> {
            if (target.present) {
                hold.targetInstanceGeneration = target.generation;
                record(hold);
            }
            boolean sourceExpected = source.present && hold.sourceInstanceGeneration != null
                    && hold.sourceInstanceGeneration.equals(source.generation);
            boolean targetExpected = target.present
                    && (target.generation != null || target.featureAbsent);
            boolean sourceAbsent = source.authoritative && !source.present;
            boolean targetAbsent = target.authoritative && !target.present;
            boolean safe = migrated ? targetExpected && sourceAbsent : sourceExpected && targetAbsent;
            if (!safe) { hold.status = "Unknown"; hold.reason = "Source/target VM instance identity was not conclusively verified"; record(hold); }
            completion.accept(safe);
        });
    }

    private static final class VmIdentity {
        final boolean authoritative;
        final boolean present;
        final String generation;
        final boolean featureAbsent;
        VmIdentity(boolean authoritative, boolean present, String generation) {
            this.authoritative = authoritative;
            this.present = present;
            this.generation = generation;
            this.featureAbsent = false;
        }
        VmIdentity(boolean authoritative, boolean present, String generation, boolean featureAbsent) {
            this.authoritative = authoritative;
            this.present = present;
            this.generation = generation;
            this.featureAbsent = featureAbsent;
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

    private void rebind(MemoryMigrationVO hold, boolean target, Consumer<Boolean> completion) {
        Map<String, Object> command = command(hold, target ? hold.targetHostUuid : hold.sourceHostUuid);
        command.put("action", "bind"); command.put("scope", "VM");
        command.put("expectedInstanceGeneration", target && hold.targetInstanceGeneration != null
                ? hold.targetInstanceGeneration : hold.sourceInstanceGeneration);
        String participation = vmParticipation(hold.vmUuid);
        command.put("participation", participation);
        String host = target ? hold.targetHostUuid : hold.sourceHostUuid;
        if (target) {
            manager.call(host, "state", command(hold, host), response -> {
                if (response != null && response.capabilities != null
                        && Boolean.FALSE.equals(response.capabilities.get("service"))
                        && response.state != null && Boolean.FALSE.equals(response.state.get("managed"))) {
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
                            "Succeeded".equals(MemoryResultRules.status(hold.operationUuid, -1, result))));
                }
            });
            return;
        }
        manager.call(host, "vm-efficiency", command,
                response -> completion.accept("Succeeded".equals(MemoryResultRules.status(hold.operationUuid, -1, response))));
    }

    String vmParticipation(String vmUuid) {
        try {
            return repository.vmParticipation(vmUuid);
        } catch (RuntimeException ignored) {
            // A failed DB lookup is not permission to discard a stored VM
            // exclusion and resume reclamation on that instance.
            return "deny";
        }
    }

    /** Bind existing/rerun VMs only after a confirmed Host policy and fresh inventory. */
    void reconcileObservedBindings(String host, MemoryAgentResponse response) {
        MemoryStateVO state = state(host);
        if (state == null || state.getActiveTaskUuid() != null
                || state.getAppliedRevision() == null
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
            String participation = vmParticipation(uuid);
            String mode = "deny".equals(participation) ? "deny" : "auto";
            Map<?, ?> vm = (Map<?, ?>) item.getValue();
            if (generation.equals(vm.get("bindingGeneration")) && mode.equals(vm.get("bindingMode"))) { continue; }
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("hostUuid", host); command.put("operationUuid", UUID.randomUUID().toString().replace("-", ""));
            command.put("resourceUuid", uuid); command.put("scope", "VM"); command.put("action", "bind");
            command.put("participation", participation); command.put("expectedInstanceGeneration", generation);
            candidates.put(uuid, command);
        }
        for (String uuid : bindingCursor.next(host, candidates.keySet(),
                MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.VM_BIND_BATCH_SIZE, 32))) {
            manager.call(host, "vm-efficiency", candidates.get(uuid), ignored -> { });
            // Advance even on a failure; the next authoritative sample confirms the result.
        }
    }

    private void revoke(MemoryMigrationVO hold, Consumer<Boolean> completion) {
        Map<String, Object> command = command(hold);
        command.put("action", "revoke"); command.put("scope", "VM");
        command.put("expectedInstanceGeneration", hold.sourceInstanceGeneration);
        manager.call(hold.sourceHostUuid, "vm-efficiency", command,
                response -> completion.accept("Succeeded".equals(MemoryResultRules.status(hold.operationUuid, -1, response))));
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
        if (response == null || response.state == null) { return new VmIdentity(false, false, null); }
        Object raw = response.state.get("vms");
        // An empty vms map is only proof of absence when the Agent explicitly
        // declares that the inventory is complete. A partial/failed snapshot
        // must remain Unknown.
        if (!(raw instanceof Map) || !Boolean.TRUE.equals(response.state.get("inventoryComplete"))) {
            return new VmIdentity(false, false, null);
        }
        Object value = ((Map<?, ?>) raw).get(vm);
        if (!(value instanceof Map)) { return new VmIdentity(true, false, null, targetHasNoZram(response)); }
        Object generation = ((Map<?, ?>) value).get("instanceGeneration");
        if (generation == null) { generation = ((Map<?, ?>) value).get("instance_generation"); }
        return new VmIdentity(true, true, generation instanceof String && !((String) generation).trim().isEmpty()
                ? (String) generation : null, targetHasNoZram(response));
    }

    private static boolean targetHasNoZram(MemoryAgentResponse response) {
        if (response == null || response.capabilities == null) { return false; }
        if (!Boolean.FALSE.equals(response.capabilities.get("zram"))) { return false; }
        // Agent discover_capabilities reports zram=false for both a known
        // unsupported platform and an unavailable/old service. Only the
        // former is an authoritative no-ZRAM result. In particular,
        // SERVICE_RECLAIM_CAPABILITY_UNKNOWN must remain Unknown when the
        // target still has a configured ZRAM service.
        Object rawReason = response.capabilities.get("zramReasonCode");
        if (!(rawReason instanceof String)) { return false; }
        String reason = (String) rawReason;
        return "UNSUPPORTED_ARCHITECTURE".equals(reason)
                || "UNSUPPORTED_OS_RELEASE".equals(reason)
                || "UNSUPPORTED_PAGE_SIZE".equals(reason)
                || "UNSUPPORTED_RECLAIM_KERNEL".equals(reason)
                || "ABI_INCOMPLETE".equals(reason);
    }

    @Override public void preDeleteHost(HostInventory host) throws HostException {
        MemoryStateVO state = state(host.getUuid());
        if (state != null && state.getActiveTaskUuid() != null) {
            throw new HostException("MEMORY_DRAIN_REQUIRED: resolve the outstanding memory operation before deleting this Host");
        }
        if (!managedForDelete(state)) { return; }
        try {
            JsonObject json = new JsonParser().parse(state.getState()).getAsJsonObject();
            long now = System.currentTimeMillis();
            if (state.getActiveTaskUuid() == null && state.getLastSampleTime() != null
                    && !MemoryTaskRules.blocksHost(state.getStatus())
                    && state.getLastSampleTime() <= now + 5000
                    && now - state.getLastSampleTime() < MemoryOptimizationGlobalConfig.displayTtlMillis()
                    && json.has("safeToRemove") && json.get("safeToRemove").getAsBoolean()) { return; }
        } catch (RuntimeException ignored) { }
        throw new HostException("MEMORY_DRAIN_REQUIRED: drain and verify owned memory resources before deleting this Host");
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
    @Override public void afterStartVm(VmInstanceInventory inv) { lifecycleBinding(inv, "bind"); }
    @Override public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) { lifecycleBinding(inv, "revoke"); }
    @Override public String preStopVm(VmInstanceInventory inv) { return null; }
    @Override public void beforeStopVm(VmInstanceInventory inv) { snapshotLifecycleGeneration(inv); }
    @Override public void afterStopVm(VmInstanceInventory inv) { lifecycleBinding(inv, "revoke"); }
    @Override public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) { }
    @Override public void afterFailedToStopVm(VmInstanceInventory inv, ErrorCode reason) { }
    @Override public boolean needStopBeforeDestroy(VmInstanceInventory inv) { return false; }
    @Override public String preDestroyVm(VmInstanceInventory inv) { return null; }
    @Override public void beforeDestroyVm(VmInstanceInventory inv) { }
    @Override public void afterDestroyVm(VmInstanceInventory inv) { lifecycleBinding(inv, "revoke"); }
    @Override public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) { }

    private void lifecycleBinding(VmInstanceInventory inv, String action) {
        String host = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();
        if (host == null) { return; }
        String operation = "vm-lifecycle-" + action + "-" + inv.getUuid();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("hostUuid", host); query.put("operationUuid", operation);
        query.put("resourceUuid", inv.getUuid());
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
            if (generation == null) { return; }
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("hostUuid", host); command.put("operationUuid", operation);
            command.put("resourceUuid", inv.getUuid()); command.put("scope", "VM");
            command.put("action", action); command.put("expectedInstanceGeneration", generation);
            String participation = vmParticipation(inv.getUuid());
            command.put("participation", participation);
            String expected = generation;
            manager.call(host, "vm-efficiency", command, ignored -> {
                if ("revoke".equals(action)) { lifecycleGenerations.remove(inv.getUuid(), expected); }
            });
        });
    }

    private void snapshotLifecycleGeneration(VmInstanceInventory inv) {
        String host = inv.getHostUuid() == null ? inv.getLastHostUuid() : inv.getHostUuid();
        if (host == null) { return; }
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("hostUuid", host); query.put("operationUuid", "vm-lifecycle-snapshot-" + inv.getUuid());
        query.put("resourceUuid", inv.getUuid());
        manager.call(host, "state", query, response -> {
            VmIdentity identity = vmIdentity(response, inv.getUuid());
            if (identity.present && identity.generation != null) {
                lifecycleGenerations.put(inv.getUuid(), identity.generation);
            }
        });
    }

    /** Explicit MN reconcile entry; never retries a hold without identity evidence. */
    public void reconcileMigrationHold(String vmUuid, Completion completion) {
        MemoryMigrationVO hold = repository.transaction(em -> em.find(MemoryMigrationVO.class, vmUuid, LockModeType.PESSIMISTIC_WRITE));
        if (hold == null || "Released".equals(hold.status)) { completion.success(); return; }
        // No pool/instance identity means preflight failed before native
        // acquire was sent. It is safe to close only when authoritative
        // inventories and the platform still place the VM on the source.
        final boolean acquireNotSent = hold.poolGeneration == null && hold.sourceInstanceGeneration == null;
        inspectMigrationIdentities(hold, (source, target) -> {
            boolean sourceExpected = source.present && hold.sourceInstanceGeneration != null
                    && hold.sourceInstanceGeneration.equals(source.generation);
            boolean targetExpected = target.present && target.generation != null;
            boolean sourceAbsent = source.authoritative && !source.present;
            boolean targetAbsent = target.authoritative && !target.present;
            if (acquireNotSent && source.present && targetAbsent) {
                List<String> currentHosts = repository.targets("VM", vmUuid);
                if (currentHosts.size() == 1 && hold.sourceHostUuid.equals(currentHosts.get(0))) {
                    repository.abortMigrationParticipation(vmUuid);
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
                rebind(hold, false, rebound -> {
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
                rebind(hold, true, rebound -> {
                    if (!rebound) { completion.fail(error("MEMORY_RESULT_UNKNOWN", "Target VM binding remains unknown")); return; }
                    revoke(hold, revoked -> {
                        if (!revoked) { completion.fail(error("MEMORY_RESULT_UNKNOWN", "Source VM revocation remains unknown")); return; }
                        hold(hold, "release", released -> {
                            if (released) completion.success();
                            else completion.fail(error("MEMORY_RESULT_UNKNOWN", "Migration hold release remains unknown"));
                        });
                    });
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
