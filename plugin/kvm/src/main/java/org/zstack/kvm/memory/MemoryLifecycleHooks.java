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
import org.zstack.kvm.KVMAgentCommands.MigrateVmCmd;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.persistence.LockModeType;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Protects service-owned pools across the platform's existing migration lifecycle. */
public class MemoryLifecycleHooks implements VmInstanceMigrateExtensionPoint, HostDeleteExtensionPoint,
        VmInstanceStartExtensionPoint, VmInstanceStopExtensionPoint, VmInstanceDestroyExtensionPoint,
        VmJustAfterDeleteFromDbExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MemoryLifecycleHooks.class);
    @Autowired private MemoryRepository repository;
    @Autowired private MemoryOptimizationManager manager;
    private final Map<String, String> lifecycleGenerations = new ConcurrentHashMap<>();
    private final MemoryBatchCursor bindingCursor = new MemoryBatchCursor();
    /** VMs whose binding request is still waiting in the VM's queue or at the Agent. */
    private final Set<String> reconcilingBindings = ConcurrentHashMap.newKeySet();

    private MemoryStateVO state(String host) { return repository.state(host); }

    /**
     * Runs one step of a migration callback. A failure inside it (the database refusing a
     * record, say) is reported to the caller, which must still answer the migration.
     */
    private static boolean attempt(String what, Runnable step) {
        try { step.run(); return true; }
        catch (RuntimeException e) { logger.warn("Migration memory protection: " + what + " failed", e); return false; }
    }

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
            if (existing != null && hold.swapDirect) {
                // Result collection and either Host's reconciliation can finish
                // independently. Never overwrite newer counters or released holds.
                if (existing.swapDirectState != null) {
                    Map<String, Object> current = new Gson().fromJson(existing.swapDirectState, Map.class);
                    if (hold.swapDirectState != null) {
                        Map<String, Object> candidate = new Gson().fromJson(hold.swapDirectState, Map.class);
                        candidate.putAll(current); current = candidate;
                    }
                    hold.swapDirectState = new Gson().toJson(current);
                }
                if ("Released".equals(existing.sourceHoldStatus)) { hold.sourceHoldStatus = "Released"; }
                if ("Released".equals(existing.targetHoldStatus)) { hold.targetHoldStatus = "Released"; }
                if ("Released".equals(existing.status)
                        || "Released".equals(hold.sourceHoldStatus) && "Released".equals(hold.targetHoldStatus)) {
                    hold.status = "Released"; hold.reason = null;
                } else if ("Held".equals(hold.status) && !Objects.equals(hold.sourceHoldStatus, hold.targetHoldStatus)) {
                    hold.status = "Preparing";
                }
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

    /**
     * The source's written-back pages. Only a complete scan, or a pool without a writeback
     * disk, counts them exactly; a partial or refused scan is replaced by its upper bound:
     * every page the source has swapped out.
     */
    static Object writebackPages(Map<?, ?> sourceState) {
        if (sourceState == null) { return null; }
        Object pages = sourceState.get("backingPages");
        Object quality = sourceState.get("backingEstimate");
        if (!"native_get_pagemap".equals(quality) && !"no_backing_device".equals(quality)) {
            try { pages = String.valueOf(Long.parseLong(String.valueOf(sourceState.get("coldBytes"))) / 4096); }
            catch (NumberFormatException unknown) { /* keep the estimate */ }
        }
        return pages;
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
            boolean known = pool != null && instance != null;
            boolean recorded = attempt("recording the source identity", () -> {
                if (known) { hold.poolGeneration = pool; hold.sourceInstanceGeneration = instance; }
                else { hold.status = "Unknown"; hold.reason = "Source pool identity cannot be confirmed"; }
                record(hold);
            });
            if (!known || !recorded) {
                completion.fail(error("MEMORY_RESULT_UNKNOWN", known ? "Source migration protection could not be recorded" : hold.reason));
                return;
            }
            holdSide(hold, Side.SOURCE, "acquire", 1, acquired -> {
                if (acquired) { completion.success(); }
                else { completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "Source active operations were not confirmed held")); }
            });
        });
    }

    /** Read only the latest Host observation; an absent or mismatched cache cannot grant support. */
    private String directCapabilityRejection(String host) {
        MemoryStateVO observed = state(host);
        if (observed == null || observed.getState() == null) { return "HOST_CAPABILITY_UNKNOWN"; }
        try {
            Map<?, ?> raw = new Gson().fromJson(observed.getState(), Map.class);
            Map<?, ?> caps = raw != null && raw.get("capabilities") instanceof Map ? (Map<?, ?>) raw.get("capabilities") : null;
            Map<?, ?> direct = caps != null && caps.get("swapDirect") instanceof Map ? (Map<?, ?>) caps.get("swapDirect") : null;
            Map<?, ?> zram = raw != null && raw.get("zram") instanceof Map ? (Map<?, ?>) raw.get("zram") : null;
            Object boot = raw == null ? null : raw.get("bootId");
            Object pool = zram == null ? null : zram.get("poolGeneration");
            if (direct == null || !(boot instanceof String) || ((String) boot).isEmpty()
                    || !Objects.equals(boot, direct.get("bootId")) || !Objects.equals(pool, direct.get("poolGeneration"))) {
                return "HOST_CAPABILITY_IDENTITY_UNKNOWN";
            }
            if (Boolean.TRUE.equals(direct.get("supported")) && pool instanceof String && !((String) pool).isEmpty()) { return null; }
            return direct.get("reasonCode") instanceof String ? (String) direct.get("reasonCode") : "HOST_CAPABILITY_UNCONFIRMED";
        } catch (RuntimeException unreadable) {
            return "HOST_CAPABILITY_UNREADABLE";
        }
    }

    /** Select only after the actual storage/direction parameters are known, before sending migrate. */
    public void prepareMigrationCommand(MigrateVmCmd cmd, String source, String target, Completion completion) {
        final long started = System.nanoTime();
        String rejection = directCapabilityRejection(source);
        if (rejection != null) { rejection = "SOURCE " + rejection; }
        else {
            rejection = directCapabilityRejection(target);
            if (rejection != null) { rejection = "TARGET " + rejection; }
        }
        if (rejection == null && (cmd.isMigrateFromDestination()
                || cmd.getDisks() != null && !cmd.getDisks().isEmpty()
                || "FullCopy".equals(cmd.getStorageMigrationPolicy()) || "IncCopy".equals(cmd.getStorageMigrationPolicy()))) {
            rejection = "SOURCE_DRIVEN_SHARED_STORAGE_PRECOPY_REQUIRED";
        }
        MemoryMigrationVO hold = repository.transaction(em -> em.find(MemoryMigrationVO.class, cmd.getVmUuid()));
        if (rejection != null) {
            if (hold != null && !hold.swapDirect && "Held".equals(hold.status)
                    && source.equals(hold.sourceHostUuid) && target.equals(hold.targetHostUuid)) {
                hold.reason = rejection; record(hold);
            }
            migrationSelection(cmd.getVmUuid(), false, rejection, started); completion.success(); return;
        }
        if (hold == null || !"Held".equals(hold.status) || !"Held".equals(hold.sourceHoldStatus)
                || !source.equals(hold.sourceHostUuid) || !target.equals(hold.targetHostUuid)) {
            migrationSelection(cmd.getVmUuid(), false, "SOURCE_MIGRATION_HOLD_UNCONFIRMED", started);
            completion.success(); return;
        }
        hold.swapDirect = true; record(hold);
        prepareDirectTarget(hold, new Completion(completion) {
            @Override public void success() {
                cmd.setSwapDirect(directMigrationCommand(cmd.getVmUuid(), source, target));
                // Preserve the global XBZRLE setting; only this command changes.
                cmd.setXbzrle(false);
                migrationSelection(cmd.getVmUuid(), true, null, started);
                completion.success();
            }
            @Override public void fail(ErrorCode errorCode) {
                final String reason = hold.reason == null ? errorCode.getDetails() : hold.reason;
                // No migrate command has been sent. Release only the extra destination
                // hold/budget; ordinary migration still needs its original source hold.
                holdSide(hold, Side.TARGET, "release", 1, released -> {
                    if (!released) {
                        completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "Cannot confirm destination preflight hold cleanup: " + reason));
                        return;
                    }
                    boolean[] ordinaryReady = {false};
                    attempt("returning to ordinary migration", () -> ordinaryReady[0] = repository.transaction(em -> {
                        MemoryMigrationVO current = em.find(MemoryMigrationVO.class, hold.vmUuid, LockModeType.PESSIMISTIC_WRITE);
                        if (current == null || !hold.operationUuid.equals(current.operationUuid)
                                || !"Held".equals(current.sourceHoldStatus) || !"Released".equals(current.targetHoldStatus)) { return false; }
                        current.swapDirect = false; current.swapDirectState = null;
                        current.status = "Held"; current.reason = reason;
                        em.merge(current); em.flush(); return true;
                    }));
                    if (!ordinaryReady[0]) {
                        completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "Source migration protection changed during preflight cleanup"));
                        return;
                    }
                    migrationSelection(cmd.getVmUuid(), false, reason, started);
                    completion.success();
                });
            }
        });
    }

    private void migrationSelection(String vm, boolean direct, String reason, long started) {
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        String detail = String.format("VM[%s] migration method=%s, direct eligibility elapsed=%d ms%s", vm,
                direct ? "cold-page-direct" : "ordinary", elapsed, reason == null ? "" : ", reason=" + reason);
        logger.info(detail);
        try { org.zstack.core.progress.ProgressReportService.taskProgress("%s", detail); }
        catch (RuntimeException unavailable) { logger.debug("Migration selection remains in the management-node log", unavailable); }
    }

    private void prepareDirectTarget(MemoryMigrationVO hold, Completion completion) {
        manager.call(hold.targetHostUuid, "state", command(hold, hold.targetHostUuid), response -> {
            String pool = inventoryComplete(response) ? stateString(response, "poolGeneration", "pool_generation") : null;
            VmIdentity target = vmIdentity(response, hold.vmUuid);
            boolean usable = pool != null && target.authoritative && !target.present;
            // Persist the destination intent before sending acquire: a lost response
            // must still be recoverable after a management-node restart.
            boolean recorded = attempt("recording the destination pool", () -> {
                if (usable) { hold.targetPoolGeneration = pool; }
                else { hold.status = "Unknown"; hold.reason = "Destination pool or VM absence cannot be confirmed"; }
                record(hold);
            });
            if (!usable || !recorded) {
                if (usable) { hold.reason = "Destination migration protection could not be recorded"; }
                completion.fail(error("MEMORY_RESULT_UNKNOWN", hold.reason)); return;
            }
            holdSide(hold, Side.TARGET, "acquire", 1, acquired -> {
                if (!acquired) {
                    completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "Destination active operations were not confirmed held")); return;
                }
                directPreflight(hold, Side.SOURCE, null, source -> directPreflight(hold, Side.TARGET, source, targetState -> {
                    boolean matched = source != null && targetState != null
                            && Objects.equals(source.get("compressor"), targetState.get("compressor"));
                    boolean saved = attempt("recording the prepared identities", () -> {
                        if (matched) {
                            Map<String, Object> prepared = new LinkedHashMap<>();
                            prepared.put("operationUuid", hold.operationUuid);
                            prepared.put("source", source); prepared.put("destination", targetState);
                            hold.swapDirectState = new Gson().toJson(prepared);
                        } else {
                            hold.status = "Unknown";
                            if (hold.reason == null) { hold.reason = "Cold-page direct compressors differ"; }
                        }
                        record(hold);
                    });
                    if (!matched || !saved) {
                        if (matched) { hold.reason = "Cold-page direct preparation could not be recorded"; }
                        completion.fail(error("MEMORY_INVALID_REQUEST", hold.reason)); return;
                    }
                    // The first target hold quiesces native writeback. Grant its
                    // temporary quota only after the source estimate is durable.
                    holdSide(hold, Side.TARGET, "acquire", 1, budgetReady -> {
                        if (budgetReady) { completion.success(); }
                        else { completion.fail(error("MEMORY_MIGRATION_HOLD_PENDING", "Destination writeback budget was not confirmed")); }
                    });
                }));
            });
        });
    }

    private void directPreflight(MemoryMigrationVO hold, Side side, Map<?, ?> sourceInfo, Consumer<Map<?, ?>> completion) {
        boolean source = side == Side.SOURCE;
        if (!source && sourceInfo == null) { completion.accept(null); return; }
        String host = source ? hold.sourceHostUuid : hold.targetHostUuid;
        Map<String, Object> query = command(hold, host);
        if (!source) {
            query.put("swapDirectMemoryBytes", sourceInfo.get("memoryBytes"));
            query.put("swapDirectColdBytes", sourceInfo.get("coldBytes"));
            Object pages = writebackPages(sourceInfo);
            if (pages != null) { query.put("swapDirectWritebackPages", pages); }
        }
        query.put("swapDirectPreflight", true); query.put("swapDirectSource", source);
        query.put("poolGeneration", source ? hold.poolGeneration : hold.targetPoolGeneration);
        query.put("expectedInstanceGeneration", source ? hold.sourceInstanceGeneration : null);
        manager.call(host, "state", query, response -> {
            Object raw = response != null && response.isSuccess() && response.state != null
                    ? response.state.get("swapDirect") : null;
            Map<?, ?> info = raw instanceof Map ? (Map<?, ?>) raw : null;
            boolean ready = info != null && Boolean.TRUE.equals(info.get("supported"))
                    && info.get("protocol") instanceof Number && ((Number) info.get("protocol")).intValue() == 1
                    && host.equals(info.get("hostUuid"))
                    && Objects.equals(query.get("poolGeneration"), info.get("poolGeneration"));
            if (!ready) {
                hold.reason = side + " cold-page preflight: " + (info != null && info.get("reasonCode") != null
                        ? info.get("reasonCode") : MemoryOptimizationManager.failure(response));
            }
            completion.accept(ready ? info : null);
        });
    }

    /** Only a persisted, fully held operation may reach the hypervisor command. */
    public Map<String, Object> directMigrationCommand(String vm, String source, String target) {
        MemoryMigrationVO hold = repository.transaction(em -> em.find(MemoryMigrationVO.class, vm));
        if (hold == null || !hold.swapDirect || "Released".equals(hold.status)) { return null; }
        if (!"Held".equals(hold.status) || !"Held".equals(hold.sourceHoldStatus) || !"Held".equals(hold.targetHoldStatus)
                || !source.equals(hold.sourceHostUuid) || !target.equals(hold.targetHostUuid) || hold.swapDirectState == null) {
            throw new MemoryOperationException("MEMORY_MIGRATION_HOLD_PENDING", "Cold-page direct migration is not prepared on both Hosts");
        }
        return new Gson().fromJson(hold.swapDirectState, Map.class);
    }

    public void recordDirectMigrationResult(String vm, Map<String, Object> request, Map<String, Object> result, boolean source) {
        if (request == null || result == null) { return; }
        try {
            repository.transaction(em -> {
                MemoryMigrationVO hold = em.find(MemoryMigrationVO.class, vm, LockModeType.PESSIMISTIC_WRITE);
                if (hold == null || !hold.swapDirect || !hold.operationUuid.equals(request.get("operationUuid"))) { return null; }
                Map<String, Object> recorded = new Gson().fromJson(hold.swapDirectState, Map.class);
                if (recorded == null) { recorded = new LinkedHashMap<>(); }
                recorded.put(source ? "sourceResult" : "destinationResult", result);
                hold.swapDirectState = new Gson().toJson(recorded); em.merge(hold); em.flush(); return null;
            });
            if (source && result.get("fallback") != null) {
                // The source Agent's last check, or QEMU before it sent any memory, refused
                // the direct path and the Agent went on with an ordinary migration.
                String detail = String.format("VM[%s] migration method=ordinary, reason=%s", vm, result.get("fallback"));
                logger.info(detail);
                org.zstack.core.progress.ProgressReportService.taskProgress("%s", detail);
            }
            // Both ends report the counters under QEMU's names: the Agent drops libvirt's prefix.
            if (source && Boolean.TRUE.equals(result.get("managed"))) {
                org.zstack.core.progress.ProgressReportService.taskProgress(
                        "Cold-page migration: compressed objects %s, compressed payload bytes %s, backing pages sent %s, cached pages sent %s",
                        result.get("compressed-objects-sent"), result.get("compressed-bytes-sent"),
                        result.get("backing-pages-sent"), result.get("cached-pages-sent"));
            }
        } catch (RuntimeException failure) {
            // Accounting must not turn a completed hypervisor migration into a
            // failed VM placement callback or prevent its hold reconciliation.
            logger.warn(String.format("Cannot record cold-page migration counters for VM[%s]", vm), failure);
        }
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
        holdSide(hold, Side.SOURCE, action, 1, source -> {
            if (!hold.swapDirect) { completion.accept(source); return; }
            holdSide(hold, Side.TARGET, action, 1, target -> completion.accept(source && target));
        });
    }

    private void holdSide(MemoryMigrationVO hold, Side side, String action, int attempt, Consumer<Boolean> completion) {
        boolean source = side == Side.SOURCE;
        String host = source ? hold.sourceHostUuid : hold.targetHostUuid;
        String pool = source ? hold.poolGeneration : hold.targetPoolGeneration;
        String status = source ? hold.sourceHoldStatus : hold.targetHoldStatus;
        if ("release".equals(action) && ("Released".equals(status) || pool == null || !hostExists(host))) {
            heldResult(hold, side, action, true); completion.accept(true); return;
        }
        Map<String, Object> command = command(hold, host);
        command.put("holdAction", action); command.put("poolGeneration", pool);
        if (!source && "acquire".equals(action) && hold.swapDirectState != null) {
            Map<?, ?> prepared = new Gson().fromJson(hold.swapDirectState, Map.class);
            Object pages = writebackPages((Map<?, ?>) prepared.get("source"));
            if (pages != null) { command.put("writebackPages", pages); }
        }
        manager.call(host, "migration-hold", command, response -> {
            if (response != null && response.isSuccess() && "Converging".equals(response.status) && attempt < HOLD_ATTEMPTS) {
                manager.later(() -> holdSide(hold, side, action, attempt + 1, completion),
                        response.retryAfterSeconds == null ? 5 : Math.max(1, response.retryAfterSeconds));
                return;
            }
            boolean confirmed = MemoryResultRules.succeeded(hold.operationUuid, response);
            boolean recorded = attempt("recording the " + side + " hold", () -> heldResult(hold, side, action, confirmed));
            completion.accept(confirmed && recorded);
        });
    }

    private void heldResult(MemoryMigrationVO hold, Side side, String action, boolean confirmed) {
        String status = confirmed ? ("acquire".equals(action) ? "Held" : "Released") : "Unknown";
        if (side == Side.SOURCE) { hold.sourceHoldStatus = status; } else { hold.targetHoldStatus = status; }
        hold.status = status;
        if (hold.swapDirect) {
            if ("Unknown".equals(hold.sourceHoldStatus) || "Unknown".equals(hold.targetHoldStatus)) { hold.status = "Unknown"; }
            else if (!Objects.equals(hold.sourceHoldStatus, hold.targetHoldStatus)) { hold.status = "Preparing"; }
        }
        hold.reason = "Unknown".equals(hold.status) ? "Migration hold result must be reconciled on both Hosts" : null;
        record(hold);
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
            if (target.present && !attempt("recording the target instance", () -> {
                hold.targetInstanceGeneration = target.generation; record(hold);
            })) { completion.accept(false); return; }
            boolean sourceExpected = source.present && hold.sourceInstanceGeneration != null
                    && hold.sourceInstanceGeneration.equals(source.generation);
            boolean targetExpected = targetExpected(hold, target);
            boolean sourceAbsent = source.authoritative && !source.present;
            boolean targetAbsent = target.authoritative && !target.present;
            boolean safe = result == MigrationResult.MIGRATED ? targetExpected && sourceAbsent : sourceExpected && targetAbsent;
            boolean recorded = safe || attempt("recording the unverified identities", () -> {
                hold.status = "Unknown"; hold.reason = "Source/target VM instance identity was not conclusively verified"; record(hold);
            });
            completion.accept(safe && recorded);
        });
    }

    /**
     * The migrated instance is on the target. Its generation, given by the target's
     * ZRAM service, is needed only to bind it; a target without a service-owned pool
     * has nothing to bind, even when its service is not running. A running service
     * whose pool could not be read may still own one, so it proves nothing.
     * Swap-direct always requires the target instance generation.
     */
    private boolean targetExpected(MemoryMigrationVO hold, VmIdentity target) {
        return target.present && (target.generation != null || !hold.swapDirect
                && (target.featureAbsent || !target.poolUnread && !managedZram(hold.targetHostUuid)));
    }

    /** What a Host's VM inventory proves about one VM: nothing, its absence or its presence. */
    private enum Presence { UNKNOWN, ABSENT, PRESENT }

    /**
     * Why a lifecycle binding found no generation to send, or null when there is nothing to do:
     * only a VM proven absent, with no generation left, has nothing to revoke.
     */
    static String lifecycleFailure(String action, VmIdentity identity, MemoryAgentResponse response) {
        boolean absent = identity.authoritative && !identity.present;
        if (absent && "revoke".equals(action)) { return null; }
        return absent ? "VM_INSTANCE_NOT_FOUND" : "VM_IDENTITY_UNKNOWN: " + MemoryOptimizationManager.failure(response);
    }

    static final class VmIdentity {
        final boolean authoritative;
        final boolean present;
        final String generation;
        /** The inventory says the Host has no ZRAM to bind the VM to. */
        final boolean featureAbsent;
        /** The Host's ZRAM service is running but the Agent could not read its pool. */
        final boolean poolUnread;
        VmIdentity(Presence presence, String generation, MemoryAgentResponse inventory) {
            this.authoritative = presence != Presence.UNKNOWN;
            this.present = presence == Presence.PRESENT;
            this.generation = generation;
            this.featureAbsent = inventory != null && targetHasNoZram(inventory);
            Object zram = inventory == null ? null : inventory.state.get("zram");
            this.poolUnread = zram instanceof Map && "active".equals(((Map<?, ?>) zram).get("serviceState"))
                    && !(((Map<?, ?>) zram).get("pool") instanceof Map);
        }
    }

    private void inspectMigrationIdentities(MemoryMigrationVO hold, BiConsumer<VmIdentity, VmIdentity> completion) {
        manager.call(hold.sourceHostUuid, "state", command(hold, hold.sourceHostUuid), source -> {
            VmIdentity sourceIdentity = vmIdentity(source, hold.vmUuid);
            Map<String, Object> query = command(hold, hold.targetHostUuid);
            if (hold.swapDirect) { query.put("swapDirectResult", true); }
            manager.call(hold.targetHostUuid, "state", query, target -> {
                if (hold.swapDirectState != null && target != null && target.state != null
                        && target.state.get("swapDirectResult") instanceof Map) {
                    recordDirectMigrationResult(hold.vmUuid, command(hold),
                            (Map<String, Object>) target.state.get("swapDirectResult"), false);
                }
                completion.accept(sourceIdentity, vmIdentity(target, hold.vmUuid));
            });
        });
    }

    private void rebind(MemoryMigrationVO hold, Side side, Consumer<Boolean> completion) {
        boolean target = side == Side.TARGET;
        Map<String, Object> command = command(hold, target ? hold.targetHostUuid : hold.sourceHostUuid);
        command.put("action", "bind"); command.put("scope", "VM");
        try { command.put("participation", "deny".equals(repository.vmReclaimMode(hold.vmUuid)) ? "deny" : "allow"); }
        catch (RuntimeException e) { completion.accept(false); return; }
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
        Set<String> candidates = new LinkedHashSet<>();
        for (Map.Entry<?, ?> item : vms.entrySet()) {
            if (!(item.getKey() instanceof String) || !(item.getValue() instanceof Map)) { continue; }
            String uuid = (String) item.getKey();
            String generation = instanceGeneration(response, uuid);
            if (generation == null) { continue; }
            Map<?, ?> vm = (Map<?, ?>) item.getValue();
            // The service reports its binding as binding_generation and binding_mode.
            String mode;
            try { mode = repository.vmReclaimMode(uuid); }
            catch (RuntimeException e) { logger.warn("VM memory configuration is unknown: " + uuid, e); continue; }
            if (generation.equals(vm.get("binding_generation")) && mode.equals(vm.get("binding_mode"))) { continue; }
            candidates.add(uuid);
        }
        List<String> batch = new ArrayList<>(bindingCursor.next(host, candidates,
                MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.VM_BIND_BATCH_SIZE, 32)));
        Round round = new Round(String.format("Host[%s] VM memory optimization bindings", host), batch.size());
        for (String uuid : batch) {
            // One binding per VM at a time: a VM busy with a long operation would otherwise
            // collect the same request again at every observation.
            if (!reconcilingBindings.add(uuid)) { round.finished(uuid, null); continue; }
            // Route to the VM owner and reread placement/generation/config inside its lifecycle queue.
            manager.reconcileVmConfiguration(uuid, reply -> {
                reconcilingBindings.remove(uuid);
                round.finished(uuid, reply != null && reply.isSuccess() ? null : MemoryOptimizationManager.failure(reply));
            });
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

    static VmIdentity vmIdentity(MemoryAgentResponse response, String vm) {
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
                round.finished(inv.getUuid(), lifecycleFailure(action, identity, response));
                return;
            }
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("hostUuid", host); command.put("operationUuid", operation);
            command.put("resourceUuid", inv.getUuid()); command.put("scope", "VM");
            command.put("action", action); command.put("expectedInstanceGeneration", generation);
            if ("bind".equals(action)) {
                try { command.put("participation", "deny".equals(repository.vmReclaimMode(inv.getUuid())) ? "deny" : "allow"); }
                catch (RuntimeException e) { round.finished(inv.getUuid(), "VM_CONFIGURATION_UNKNOWN"); return; }
            }
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
        if (!hold.swapDirect && (hold.poolGeneration == null || !hostExists(hold.sourceHostUuid))) {
            hold.status = "Released"; hold.reason = "Released by operator recovery"; record(hold);
            completion.success();
            return;
        }
        hold(hold, "release", released -> {
            if (released) { completion.success(); }
            else { completion.fail(error("MEMORY_RESULT_UNKNOWN", "Migration hold release remains unknown")); }
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
