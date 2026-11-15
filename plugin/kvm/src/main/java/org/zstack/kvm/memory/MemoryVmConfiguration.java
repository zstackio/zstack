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
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import static org.zstack.kvm.memory.MemoryVmConfigurationMsg.Action.*;

/** VM ResourceConfig application and live evidence, using existing Agent and VM lifecycle mechanisms. */
final class MemoryVmConfiguration {
    private final CloudBus bus;
    private final ThreadFacade threads;
    private final MemoryRepository repository;
    private final MemoryOptimizationManager manager;
    // The two ResourceConfig categories are fetched concurrently by the UI.
    // Coalesce their combined READ, including Unknown, without caching writes.
    private final Cache<String, Map<String, Object>> readStates = CacheBuilder.newBuilder()
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
        zram.installLocalUpdateExtension((config, uuid, type, oldValue, newValue) -> request(uuid, RECONCILE, null));
        zram.installLocalDeleteExtension((config, uuid, type, oldValue) -> request(uuid, RECONCILE, null));
        zram.installRuntimeStateReader(uuid -> readState(uuid, "zram"));
        ResourceConfig ksm = configs.getResourceConfig("vmMemory.memory.backing.share.pages");
        if (ksm == null) { return; } // Optional commercial module; never invent a second KSM config.
        ksm.installValidatorExtension((uuid, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) { request(uuid, CHECK_KSM, Boolean.valueOf(newValue)); }
        });
        ksm.installLocalUpdateExtension((config, uuid, type, oldValue, newValue) -> request(uuid, APPLY_KSM, null));
        ksm.installLocalDeleteExtension((config, uuid, type, oldValue) -> request(uuid, APPLY_KSM, null));
        ksm.installRuntimeStateReader(uuid -> readState(uuid, "ksm"));
    }

    private MemoryVmConfigurationMsg message(String vm, MemoryVmConfigurationMsg.Action action, Boolean enabled) {
        MemoryVmConfigurationMsg msg = new MemoryVmConfigurationMsg();
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
            MemoryRepository.VmPlacement placement = repository.vmPlacement(vm);
            // Never reuse an Applied result after a configuration/placement change.
            String key = vm + ":" + (placement == null ? "absent" : placement.hostUuid + ":" + placement.state)
                    + ":" + repository.vmSharePages(vm) + ":" + repository.vmReclaimMode(vm);
            Map<String, Object> state = readStates.get(key, () -> {
                try {
                    Map<String, Object> result = request(vm, READ, null);
                    return result == null ? Collections.emptyMap() : result;
                } catch (RuntimeException e) {
                    Map<String, Object> unknown = new LinkedHashMap<>();
                    unknown.put("ksm", status("Unknown", e.getMessage()));
                    unknown.put("zram", status("Unknown", e.getMessage()));
                    return unknown;
                }
            });
            Object value = state == null ? null : state.get(feature);
            return value instanceof Map ? (Map<String, Object>) value : status("Unknown", "VM_MEMORY_STATE_UNKNOWN");
        } catch (RuntimeException | ExecutionException e) { return status("Unknown", e.getMessage()); }
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
        if (msg.getAction() == READ) { execute(msg, reply -> bus.reply(msg, reply)); return; }
        threads.chainSubmit(new ChainTask(msg) {
            @Override public String getSyncSignature() { return "Vm-" + msg.getVmUuid(); }
            @Override public String getName() { return "vm-memory-configuration-" + msg.getVmUuid(); }
            @Override public void run(SyncTaskChain chain) {
                execute(msg, reply -> { try { bus.reply(msg, reply); } finally { chain.next(); } });
            }
        });
    }

    private void execute(MemoryVmConfigurationMsg msg, Consumer<MemoryVmConfigurationMsg.Reply> done) {
        try {
            MemoryRepository.VmPlacement placement = repository.vmPlacement(msg.getVmUuid());
            if (placement == null) { finish(done, null, "VM_NOT_FOUND"); return; }
            if (placement.state != VmInstanceState.Running || placement.hostUuid == null) {
                if (placement.state == VmInstanceState.Unknown) { finish(done, null, "VM_STATE_UNKNOWN"); return; }
                Map<String, Object> pending = new LinkedHashMap<>();
                pending.put("ksm", status("Pending", "NEXT_VM_START"));
                pending.put("zram", status("Pending", "NEXT_VM_START"));
                finish(done, pending, null); return;
            }
            String host = placement.hostUuid;
            Map<String, Object> command = new LinkedHashMap<>();
            command.put("hostUuid", host); command.put("resourceUuid", msg.getVmUuid());
            command.put("operationUuid", Platform.getUuid());
            command.put("vmMemoryState", msg.getAction() != RECONCILE);
            manager.call(host, "state", command, observed -> {
                try { applyObserved(msg, host, command, observed, done); }
                catch (RuntimeException e) { finish(done, null, e.getMessage()); }
            });
        } catch (RuntimeException e) { finish(done, null, e.getMessage()); }
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
            finish(done, runtimeState(msg.getVmUuid(), generation, observed.state), null); return;
        }
        if (msg.getAction() != RECONCILE) {
            Object raw = observed.state.get("vmMemoryState");
            Map<?, ?> proof = raw instanceof Map ? (Map<?, ?>) raw : Collections.emptyMap();
            if (raw == null || Boolean.FALSE.equals(proof.get("supported"))) {
                finish(done, status("Unsupported", "KSM_RUNTIME_UNSUPPORTED_NEXT_START"), null); return;
            }
            if (!Boolean.TRUE.equals(proof.get("supported")) || !Objects.equals(generation, proof.get("instanceGeneration"))) {
                finish(done, null, "KSM_RUNTIME_STATE_UNKNOWN"); return;
            }
        }
        if (generation == null) { finish(done, null, "VM_IDENTITY_UNKNOWN"); return; }
        command.put("expectedInstanceGeneration", generation);
        command.remove("vmMemoryState");
        if (msg.getAction() == RECONCILE) {
            command.put("action", "bind");
            command.put("participation", "deny".equals(repository.vmReclaimMode(msg.getVmUuid())) ? "deny" : "allow");
        } else {
            Boolean enabled = msg.getAction() == CHECK_KSM ? msg.getKsmEnabled() : repository.vmSharePages(msg.getVmUuid());
            if (enabled == null) { finish(done, null, "KSM_CONFIGURATION_UNSUPPORTED"); return; }
            command.put("action", msg.getAction() == CHECK_KSM ? "check-ksm" : "set-ksm");
            command.put("ksmEnabled", enabled);
        }
        manager.call(host, "vm-efficiency", command, applied -> {
            if (MemoryResultRules.succeeded((String) command.get("operationUuid"), applied)) {
                if (msg.getAction() == APPLY_KSM && (applied.state == null || !Boolean.TRUE.equals(applied.state.get("applied")))) {
                    finish(done, null, "KSM_RUNTIME_READBACK_UNKNOWN"); return;
                }
                finish(done, applied.state, null);
            }
            else if (msg.getAction() != RECONCILE && applied != null &&
                    "KSM_RUNTIME_UNSUPPORTED_NEXT_START".equals(applied.reasonCode)) {
                // Preserve the existing static setting on QEMU without online merge control.
                finish(done, status("Unsupported", applied.reasonCode), null);
            } else { finish(done, null, MemoryOptimizationManager.failure(applied)); }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> runtimeState(String vm, String generation, Map<String, Object> observed) {
        Map<String, Object> result = new LinkedHashMap<>();
        String mode = repository.vmReclaimMode(vm);
        Boolean sharing = repository.vmSharePages(vm);
        Map<?, ?> vms = observed.get("vms") instanceof Map ? (Map<?, ?>) observed.get("vms") : Collections.emptyMap();
        Map<?, ?> actual = vms.get(vm) instanceof Map ? (Map<?, ?>) vms.get(vm) : Collections.emptyMap();
        Map<String, Object> zram = status("Unknown", "VM_IDENTITY_UNKNOWN");
        if (generation != null) {
            boolean applied = generation.equals(actual.get("binding_generation")) && mode.equals(actual.get("binding_mode"));
            zram = status(applied ? "Applied" : "Pending", applied ? null : "BINDING_NOT_CONFIRMED");
            zram.put("actualValue", !generation.equals(actual.get("binding_generation")) ? null :
                    "auto".equals(actual.get("binding_mode")) ? true : "deny".equals(actual.get("binding_mode")) ? false : null);
            zram.put("instanceGeneration", generation);
        }
        zram.put("configuredValue", "auto".equals(mode)); result.put("zram", zram);
        Map<?, ?> proof = observed.get("vmMemoryState") instanceof Map ? (Map<?, ?>) observed.get("vmMemoryState") : Collections.emptyMap();
        Map<String, Object> ksm = status("Unknown", "KSM_RUNTIME_STATE_UNKNOWN");
        if (Boolean.FALSE.equals(proof.get("supported"))) {
            ksm = status("Unsupported", String.valueOf(proof.get("reasonCode")));
        } else if (sharing != null && generation != null && generation.equals(proof.get("instanceGeneration")) && proof.get("backends") instanceof List) {
            List<?> backends = (List<?>) proof.get("backends");
            boolean allTrue = !backends.isEmpty(), allFalse = !backends.isEmpty();
            for (Object backend : backends) {
                Object merge = backend instanceof Map ? ((Map<?, ?>) backend).get("merge") : null;
                allTrue &= Boolean.TRUE.equals(merge); allFalse &= Boolean.FALSE.equals(merge);
            }
            boolean applied = sharing ? allTrue : allFalse;
            ksm = status(applied ? "Applied" : "Pending", applied ? null : "QMP_MERGE_NOT_CONFIRMED");
            ksm.put("actualValue", allTrue ? true : allFalse ? false : null);
            ksm.put("liveNoSharePages", proof.get("liveNoSharePages"));
            ksm.put("instanceGeneration", generation);
        }
        ksm.put("configuredValue", sharing); result.put("ksm", ksm);
        return result;
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
