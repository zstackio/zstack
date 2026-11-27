package org.zstack.kvm.memory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import static org.zstack.kvm.memory.MemoryVmConfigurationMsg.Action.*;

/** VM ResourceConfig application and live evidence, using existing Agent and VM lifecycle mechanisms. */
final class MemoryVmConfiguration {
    private static final CLogger logger = Utils.getLogger(MemoryVmConfiguration.class);
    private final CloudBus bus;
    private final ThreadFacade threads;
    private final MemoryRepository repository;
    private final MemoryOptimizationManager manager;
    // READ messages are routed to the VM owner. Keep the one-second coalescing
    // cache here, on that owner, so reads sent through different MNs share it.
    private final Cache<String, CachedRead> readStates = CacheBuilder.newBuilder()
            .maximumSize(1024).expireAfterWrite(1, TimeUnit.SECONDS).build();

    MemoryVmConfiguration(CloudBus bus, ThreadFacade threads, MemoryRepository repository, MemoryOptimizationManager manager) {
        this.bus = bus; this.threads = threads; this.repository = repository; this.manager = manager;
    }

    void install(ResourceConfigFacade configs) {
        GlobalConfig reclaim = MemoryStandardGlobalConfig.VM_ZRAM_RECLAIM_ENABLED;
        reclaim.installValidateExtension((category, name, oldValue, newValue) -> {
            if (!"true".equals(newValue) && !"false".equals(newValue)) {
                throw new GlobalConfigException("VM ZRAM reclaim must be true or false");
            }
        });
        ResourceConfig zram = configs.getResourceConfig(reclaim.getIdentity());
        zram.installValidatorExtension((uuid, oldValue, newValue) -> manager.checkConfigurationLicense());
        zram.installLocalUpdateExtension((config, uuid, type, oldValue, newValue) -> reconcileAfterReclaimChange(uuid));
        zram.installLocalDeleteExtension((config, uuid, type, oldValue) -> reconcileAfterReclaimChange(uuid));
        zram.installRuntimeStateReader(uuid -> readState(uuid, "zram"));
        // A tenant must not take its VM out of the Host's reclaim, or split its merged pages.
        ResourceConfig ksm = repository.vmPageSharingConfig();
        if (ksm == null) { return; } // Optional commercial module; never invent a second KSM config.
        ksm.installLocalValueWriter((uuid, value) ->
                request(uuid, APPLY_KSM, value == null ? null : Boolean.valueOf(value)));
        ksm.installRuntimeStateReader(uuid -> readState(uuid, "ksm"));
    }

    private MemoryVmConfigurationMsg message(String vm, MemoryVmConfigurationMsg.Action action, Boolean enabled) {
        MemoryVmConfigurationMsg msg = action == APPLY_KSM ? new MemoryVmConfigurationMsg.SaveKsm() : new MemoryVmConfigurationMsg();
        msg.setVmUuid(vm); msg.setAction(action); msg.setKsmEnabled(enabled);
        if (action == READ) { msg.setTimeout(10000L); }
        bus.makeTargetServiceIdByResourceUuid(msg, MemoryOptimizationManager.SERVICE_ID, vm);
        return msg;
    }

    private Map<String, Object> request(String vm, MemoryVmConfigurationMsg.Action action, Boolean enabled) {
        MessageReply reply = bus.call(message(vm, action, enabled));
        if (!reply.isSuccess()) { throw new GlobalConfigException(reply.getError().getDetails()); }
        if (!(reply instanceof MemoryVmConfigurationMsg.Reply)) { throw new GlobalConfigException("VM_MEMORY_REPLY_UNKNOWN"); }
        return ((MemoryVmConfigurationMsg.Reply) reply).getState();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> readState(String vm, String feature) {
        try {
            Map<String, Object> state = request(vm, READ, null);
            Object value = state == null ? null : state.get(feature);
            return value instanceof Map ? (Map<String, Object>) value : status("Unknown", "VM_MEMORY_STATE_UNKNOWN");
        } catch (RuntimeException e) { return status("Unknown", e.getMessage()); }
    }

    private String readFingerprint(String vm) {
        MemoryRepository.VmPlacement placement = repository.vmPlacement(vm);
        MemoryStateVO hostState = placement == null || placement.hostUuid == null ? null : repository.state(placement.hostUuid);
        Long hostSample = hostState == null ? null : hostState.getLastSampleTime();
        // Never reuse an Applied result after a configuration/placement change.
        return (placement == null ? "absent" : placement.hostUuid + ":" + placement.state)
                + ":" + repository.vmSharePages(vm) + ":" + repository.vmReclaimMode(vm) + ":" + hostSample;
    }

    private static final class CachedRead {
        final String fingerprint;
        final Map<String, Object> state;
        final String error;
        CachedRead(String fingerprint, Map<String, Object> state, String error) {
            this.fingerprint = fingerprint; this.state = state; this.error = error;
        }
    }

    private void replyRead(MemoryVmConfigurationMsg msg, Map<String, Object> state, SyncTaskChain chain) {
        MemoryVmConfigurationMsg.Reply reply = new MemoryVmConfigurationMsg.Reply();
        reply.setState(state);
        try { bus.reply(msg, reply); } finally { chain.next(); }
    }

    private void replyReadFailure(MemoryVmConfigurationMsg msg, String error, SyncTaskChain chain) {
        MemoryVmConfigurationMsg.Reply reply = new MemoryVmConfigurationMsg.Reply();
        reply.setError(Platform.operr("%s", error));
        try { bus.reply(msg, reply); } finally { chain.next(); }
    }

    private Map<String, Object> unknownRead(String reason) {
        Map<String, Object> unknown = new LinkedHashMap<>();
        unknown.put("ksm", status("Unknown", reason));
        unknown.put("zram", status("Unknown", reason));
        return unknown;
    }

    private void reconcileAfterReclaimChange(String vm) {
        try {
            reconcile(vm, result -> {
                if (result == null || !result.isSuccess()) {
                    logger.warn(String.format("VM[%s] ZRAM reclaim configuration was saved, but runtime reconciliation failed: %s",
                            vm, result == null ? "NO_REPLY" : MemoryOptimizationManager.failure(result)));
                }
            });
        } catch (RuntimeException e) {
            logger.warn(String.format("VM[%s] ZRAM reclaim configuration was saved, but runtime reconciliation could not be scheduled",
                    vm), e);
        }
    }

    void reconcile(String vm, Consumer<MemoryAgentResponse> completion) {
        bus.send(message(vm, RECONCILE, null), new CloudBusCallBack(null) {
            @Override public void run(MessageReply reply) {
                MemoryAgentResponse result = new MemoryAgentResponse();
                result.setSuccess(reply.isSuccess());
                result.reasonCode = reply.isSuccess() ? null : reply.getError().getDetails();
                completion.accept(result);
            }
        });
    }

    void handle(MemoryVmConfigurationMsg msg) {
        threads.chainSubmit(new ChainTask(msg) {
            @Override public String getSyncSignature() { return "Vm-" + msg.getVmUuid(); }
            @Override public String getName() { return "vm-memory-configuration-" + msg.getVmUuid(); }
            @Override public void run(SyncTaskChain chain) {
                if (msg.getAction() == READ) {
                    final String fingerprint;
                    try { fingerprint = readFingerprint(msg.getVmUuid()); }
                    catch (RuntimeException e) {
                        replyRead(msg, unknownRead(e.getMessage()), chain); return;
                    }
                    CachedRead cached = readStates.getIfPresent(msg.getVmUuid());
                    if (cached != null && cached.fingerprint.equals(fingerprint)) {
                        if (cached.error == null) { replyRead(msg, cached.state, chain); }
                        else { replyReadFailure(msg, cached.error, chain); }
                        return;
                    }
                    execute(msg, reply -> {
                        if (!reply.isSuccess()) {
                            String error = reply.getError() == null ? "VM_MEMORY_STATE_UNKNOWN" : reply.getError().getDetails();
                            readStates.put(msg.getVmUuid(), new CachedRead(fingerprint, null, error));
                            try { bus.reply(msg, reply); } finally { chain.next(); }
                            return;
                        }
                        Map<String, Object> state = reply.getState() == null ? unknownRead("VM_MEMORY_STATE_UNKNOWN")
                                : reply.getState();
                        readStates.put(msg.getVmUuid(), new CachedRead(fingerprint, state, null));
                        replyRead(msg, state, chain);
                    });
                    return;
                }
                execute(msg, reply -> {
                    try {
                        if ((msg.getAction() == APPLY_KSM || msg.getAction() == RECONCILE) && reply.isSuccess()) {
                            readStates.invalidate(msg.getVmUuid());
                        }
                        bus.reply(msg, reply);
                    } finally { chain.next(); }
                });
            }
        });
    }

    private void execute(MemoryVmConfigurationMsg msg, Consumer<MemoryVmConfigurationMsg.Reply> done) {
        try {
            MemoryRepository.VmPlacement placement = repository.vmPlacement(msg.getVmUuid());
            if (placement == null) { finish(done, null, "VM_NOT_FOUND"); return; }
            if (msg instanceof MemoryVmConfigurationMsg.SaveKsm && msg.getKsmEnabled() != null
                    && placement.state != VmInstanceState.Running
                    && Objects.equals(repository.vmSharePages(msg.getVmUuid()), msg.getKsmEnabled())) {
                saveKsm(msg, null, done, null);
                return;
            }
            if (placement.state != VmInstanceState.Running || placement.hostUuid == null) {
                if (placement.state == VmInstanceState.Unknown) { finish(done, null, "VM_STATE_UNKNOWN"); return; }
                Map<String, Object> pending = new LinkedHashMap<>();
                pending.put("ksm", status("Pending", "NEXT_VM_START"));
                pending.put("zram", status("Pending", "NEXT_VM_START"));
                if (msg instanceof MemoryVmConfigurationMsg.SaveKsm) { saveKsm(msg, pending, done, null); }
                else { finish(done, pending, null); }
                return;
            }
            String host = placement.hostUuid;
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("hostUuid", host); command.put("resourceUuid", msg.getVmUuid());
            command.put("operationUuid", Platform.getUuid());
            // A binding needs only this VM's identity, not an observation of the whole Host.
            command.put(msg.getAction() == RECONCILE ? "vmIdentity" : "vmMemoryState", true);
            manager.call(host, "state", command, observed -> {
                try { applyObserved(msg, host, command, observed, done); }
                catch (RuntimeException e) {
                    String reason = e.getMessage();
                    finish(done, null, reason == null || reason.trim().isEmpty() ? "VM_MEMORY_OBSERVATION_UNAVAILABLE" : reason);
                }
            });
        } catch (RuntimeException e) {
            String reason = e.getMessage();
            finish(done, null, reason == null || reason.trim().isEmpty() ? "VM_MEMORY_OBSERVATION_UNAVAILABLE" : reason);
        }
    }

    private void applyObserved(MemoryVmConfigurationMsg msg, String host, Map<String, Object> command,
                               MemoryAgentResponse observed, Consumer<MemoryVmConfigurationMsg.Reply> done) {
        if (observed == null || !observed.isSuccess() || observed.state == null) {
            finish(done, null, "VM_MEMORY_OBSERVATION_UNAVAILABLE"); return;
        }
        MemoryRepository.VmPlacement placement = repository.vmPlacement(msg.getVmUuid());
        if (placement == null || placement.state != VmInstanceState.Running || !host.equals(placement.hostUuid)) {
            finish(done, null, "VM_PLACEMENT_CHANGED"); return;
        }
        String generation = MemoryLifecycleHooks.instanceGeneration(observed, msg.getVmUuid());
        if (msg.getAction() == READ) {
            finish(done, runtimeState(msg.getVmUuid(), host, generation, observed.state), null); return;
        }
        if (msg.getAction() != RECONCILE) {
            Object raw = observed.state.get("vmMemoryState");
            Map<?, ?> proof = raw instanceof Map ? (Map<?, ?>) raw : Collections.emptyMap();
            if (Boolean.FALSE.equals(proof.get("supported"))) {
                saveKsm(msg, status("Unsupported", "KSM_RUNTIME_UNSUPPORTED_NEXT_START"), done, null); return;
            }
            if (!Boolean.TRUE.equals(proof.get("supported")) || !Objects.equals(generation, proof.get("instanceGeneration"))) {
                finish(done, null, "KSM_RUNTIME_STATE_UNKNOWN"); return;
            }
        }
        if (generation == null) { finish(done, null, "VM_IDENTITY_UNKNOWN"); return; }
        command.put("expectedInstanceGeneration", generation);
        command.remove("vmMemoryState"); command.remove("vmIdentity");
        if (msg.getAction() == RECONCILE) {
            command.put("action", "bind");
            command.put("scope", "VM");
            command.put("participation", "deny".equals(repository.vmReclaimMode(msg.getVmUuid())) ? "deny" : "allow");
        } else {
            Boolean enabled = desiredSharing(msg);
            if (enabled == null) { finish(done, null, "KSM_CONFIGURATION_UNSUPPORTED"); return; }
            command.put("action", "set-ksm");
            command.put("ksmEnabled", enabled);
        }
        final Boolean previous = msg.getAction() == APPLY_KSM ? observedSharing(observed.state, generation) : null;
        manager.call(host, "vm-efficiency", command, applied -> {
            if (msg.getAction() != APPLY_KSM) {
                if (MemoryResultRules.succeeded((String) command.get("operationUuid"), applied)) { finish(done, applied.state, null); }
                else { finish(done, null, MemoryOptimizationManager.failure(applied)); }
                return;
            }
            boolean terminal = applied != null && command.get("operationUuid").equals(applied.operationUuid)
                    && ("Succeeded".equals(applied.status) || "Failed".equals(applied.status));
            Consumer<String> recover = reason -> recoverKsm(msg, host, command, previous, terminal, reason, done);
            if (applied != null && "KSM_RUNTIME_UNSUPPORTED_NEXT_START".equals(applied.reasonCode)) {
                saveKsm(msg, status("Unsupported", applied.reasonCode), done, null);
            } else if (MemoryResultRules.succeeded((String) command.get("operationUuid"), applied)
                    && applied.state != null && Boolean.TRUE.equals(applied.state.get("applied"))) {
                saveKsm(msg, applied.state, done, recover);
            } else {
                String error = applied != null && applied.isSuccess() ? "KSM_RUNTIME_READBACK_UNKNOWN" : MemoryOptimizationManager.failure(applied);
                if (msg instanceof MemoryVmConfigurationMsg.SaveKsm) { recover.accept(error); }
                else { finish(done, null, error); }
            }
        });
    }

    private Boolean desiredSharing(MemoryVmConfigurationMsg msg) {
        if (msg.getKsmEnabled() != null) { return msg.getKsmEnabled(); }
        return msg instanceof MemoryVmConfigurationMsg.SaveKsm ? repository.vmPageSharingConfig().defaultValue(Boolean.class)
                : repository.vmSharePages(msg.getVmUuid());
    }

    private void saveKsm(MemoryVmConfigurationMsg msg, Map<String, Object> state,
                         Consumer<MemoryVmConfigurationMsg.Reply> done, Consumer<String> recover) {
        if (!(msg instanceof MemoryVmConfigurationMsg.SaveKsm)) { finish(done, state, null); return; }
        String vm = msg.getVmUuid();
        Boolean requested = msg.getKsmEnabled();
        ResourceConfig ksm = repository.vmPageSharingConfig();
        try {
            ksm.persistLocalValue(vm, requested == null ? null : requested.toString());
        } catch (RuntimeException failure) {
            // A lost commit acknowledgement does not prove that the write failed.
            try {
                boolean saved = ksm.resourceConfigCreated(vm);
                if (saved == (requested != null) && (!saved || requested.equals(repository.vmSharePages(vm)))) {
                    finish(done, state, null);
                    return;
                }
            } catch (RuntimeException unreadable) {
                logger.warn("Cannot verify the saved page sharing setting for VM " + vm, unreadable);
                finish(done, null, Platform.i18n("memory.ksm.savedValueUnknown"));
                return;
            }
            logger.warn("Cannot save the page sharing setting for VM " + vm, failure);
            if (recover != null) { recover.accept("KSM_CONFIGURATION_SAVE_FAILED"); }
            else { finish(done, null, "KSM_CONFIGURATION_SAVE_FAILED"); }
            return;
        }
        finish(done, state, null);
    }

    private void recoverKsm(MemoryVmConfigurationMsg msg, String host, Map<String, Object> appliedCommand,
                            Boolean previous, boolean terminal, String reason, Consumer<MemoryVmConfigurationMsg.Reply> done) {
        String generation = (String) appliedCommand.get("expectedInstanceGeneration");
        String unknown = reason + ": " + Platform.i18n("memory.ksm.runtimeUnknown");
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("hostUuid", host); query.put("resourceUuid", msg.getVmUuid());
        query.put("operationUuid", Platform.getUuid()); query.put("vmMemoryState", true);
        manager.call(host, "state", query, observation -> {
            try {
                MemoryRepository.VmPlacement placement = repository.vmPlacement(msg.getVmUuid());
                if (!terminal || placement == null || !host.equals(placement.hostUuid) || placement.state != VmInstanceState.Running
                        || previous == null || observation == null || !observation.isSuccess() || observation.state == null
                        || !generation.equals(MemoryLifecycleHooks.instanceGeneration(observation, msg.getVmUuid()))) {
                    finish(done, null, unknown); return;
                }
                Boolean actual = observedSharing(observation.state, generation);
                String unchanged = "KSM_CONFIGURATION_SAVE_FAILED".equals(reason) ? reason + ": " + Platform.i18n("memory.ksm.runtimeRestored") : reason;
                if (previous.equals(actual)) { finish(done, null, unchanged); return; }
                if (!appliedCommand.get("ksmEnabled").equals(actual)) { finish(done, null, unknown); return; }
                Map<String, Object> restore = new LinkedHashMap<>(appliedCommand);
                restore.put("operationUuid", Platform.getUuid()); restore.put("ksmEnabled", previous);
                manager.call(host, "vm-efficiency", restore, response -> {
                    boolean restored = MemoryResultRules.succeeded((String) restore.get("operationUuid"), response)
                            && response.state != null && Boolean.TRUE.equals(response.state.get("applied"));
                    finish(done, null, restored ? unchanged : unknown);
                });
            } catch (RuntimeException unreadable) { finish(done, null, unknown); }
        });
    }

    private static Boolean observedSharing(Map<String, Object> state, String generation) {
        Object raw = state.get("vmMemoryState");
        if (!(raw instanceof Map)) { return null; }
        Map<?, ?> proof = (Map<?, ?>) raw;
        if (generation == null || !generation.equals(proof.get("instanceGeneration")) || !(proof.get("backends") instanceof List)) { return null; }
        Boolean result = null;
        for (Object backend : (List<?>) proof.get("backends")) {
            Object merge = backend instanceof Map ? ((Map<?, ?>) backend).get("merge") : null;
            if (!(merge instanceof Boolean) || result != null && !result.equals(merge)) { return null; }
            result = (Boolean) merge;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> runtimeState(String vm, String host, String generation, Map<String, Object> observed) {
        Map<String, Object> result = new LinkedHashMap<>();
        String mode = repository.vmReclaimMode(vm);
        Boolean sharing = repository.vmSharePages(vm);
        Map<?, ?> vms = observed.get("vms") instanceof Map ? (Map<?, ?>) observed.get("vms") : Collections.emptyMap();
        Map<?, ?> actual = vms.get(vm) instanceof Map ? (Map<?, ?>) vms.get(vm) : Collections.emptyMap();
        Map<String, Object> zram = status("Unknown", "VM_IDENTITY_UNKNOWN");
        MemoryStateVO hostState = repository.state(host);
        if (hostState == null || hostState.getState() == null || hostState.getState().trim().isEmpty()) {
            zram = status("Unknown", "HOST_MEMORY_OBSERVATION_UNAVAILABLE");
        } else if (!MemoryLifecycleHooks.hasPoolGeneration(hostState.getState())) {
            zram = status("Unknown", "HOST_MEMORY_POOL_STATE_UNAVAILABLE");
            if (freshHostSample(hostState)) {
                Map<?, ?> projection = new com.google.gson.Gson().fromJson(hostState.getState(), Map.class);
                Map<?, ?> hostZram = projection != null && projection.get("zram") instanceof Map
                        ? (Map<?, ?>) projection.get("zram") : Collections.emptyMap();
                if ("absent".equals(hostZram.get("serviceState"))) {
                    zram = status("NotApplicable", "HOST_MEMORY_COMPRESSION_NOT_ENABLED");
                }
            }
        } else if (generation != null) {
            boolean applied = generation.equals(actual.get("binding_generation")) && mode.equals(actual.get("binding_mode"));
            zram = status(applied ? "Applied" : "Pending", applied ? null : "BINDING_NOT_CONFIRMED");
            zram.put("actualValue", !generation.equals(actual.get("binding_generation")) ? null :
                    "auto".equals(actual.get("binding_mode")) ? Boolean.TRUE :
                            "deny".equals(actual.get("binding_mode")) ? Boolean.FALSE : null);
            zram.put("instanceGeneration", generation);
        }
        zram.put("configuredValue", "auto".equals(mode)); result.put("zram", zram);
        Map<?, ?> proof = observed.get("vmMemoryState") instanceof Map ? (Map<?, ?>) observed.get("vmMemoryState") : Collections.emptyMap();
        Map<String, Object> ksm = status("Unknown", "KSM_RUNTIME_STATE_UNKNOWN");
        if (Boolean.FALSE.equals(proof.get("supported"))) {
            ksm = status("Unsupported", String.valueOf(proof.get("reasonCode")));
        } else if (sharing != null && generation != null && generation.equals(proof.get("instanceGeneration")) && proof.get("backends") instanceof List) {
            Boolean actualSharing = observedSharing(observed, generation);
            boolean applied = sharing.equals(actualSharing);
            ksm = status(applied ? "Applied" : "Pending", applied ? null : "QMP_MERGE_NOT_CONFIRMED");
            ksm.put("actualValue", actualSharing);
            ksm.put("liveNoSharePages", proof.get("liveNoSharePages"));
            ksm.put("instanceGeneration", generation);
        }
        if (Boolean.TRUE.equals(sharing) && "Applied".equals(ksm.get("state")) && Boolean.FALSE.equals(hostKsmEnabled(hostState))) {
            // The Host merges no pages: what the VM allows has nothing to act on.
            Object actualValue = ksm.get("actualValue");
            ksm = status("NotApplicable", "HOST_KSM_NOT_ENABLED");
            ksm.put("actualValue", actualValue); ksm.put("instanceGeneration", generation);
        }
        ksm.put("configuredValue", sharing); result.put("ksm", ksm);
        return result;
    }

    /** The Host's own page-merging switch as it was last observed; null when it was not observed. */
    private static Boolean hostKsmEnabled(MemoryStateVO state) {
        if (!freshHostSample(state) || state.getState() == null || state.getState().trim().isEmpty()) { return null; }
        com.google.gson.JsonObject observed = MemoryObservationRules.readJson(state.getState(), com.google.gson.JsonObject.class,
                "memory state is unreadable");
        return MemorySummaryRules.boolObject(MemorySummaryRules.object(observed, "ksm"), "enabled");
    }

    private static boolean freshHostSample(MemoryStateVO state) {
        Long observedAt = state == null ? null : state.getLastSampleTime();
        long now = System.currentTimeMillis();
        if (observedAt == null || observedAt > now + 5000
                || now - observedAt >= MemoryOptimizationGlobalConfig.displayTtlMillis()) { return false; }
        return true;
    }

    static Map<String, Object> status(String value, String reason) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state", value);
        if (reason != null) { result.put("reasonCode", reason); }
        return result;
    }

    private static void finish(Consumer<MemoryVmConfigurationMsg.Reply> done, Map<String, Object> state, String error) {
        MemoryVmConfigurationMsg.Reply reply = new MemoryVmConfigurationMsg.Reply();
        reply.setState(state);
        if (error != null) { reply.setError(Platform.operr("%s", error)); }
        done.accept(reply);
    }
}
