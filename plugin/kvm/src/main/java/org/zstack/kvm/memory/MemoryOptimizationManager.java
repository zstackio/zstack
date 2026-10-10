package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.AccountManager;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.core.thread.Task;
import org.zstack.header.message.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MemoryOptimizationManager extends AbstractService implements ManagementNodeReadyExtensionPoint,
        ManagementNodeChangeListener, HostAfterConnectedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MemoryOptimizationManager.class);
    @Autowired private CloudBus bus;
    @Autowired private ThreadFacade thdf;
    @Autowired private PluginRegistry pluginRgty;
    @Autowired private MemoryRepository repository;
    @Autowired private AccountManager accountManager;
    @Autowired private MemoryLifecycleHooks lifecycleHooks;
    private final AtomicBoolean ticking = new AtomicBoolean();
    /** Coalesce event and periodic observations on this MN; DB admission remains the cross-MN fence. */
    private final Set<String> observingHosts = ConcurrentHashMap.newKeySet();
    private Future<?> poller;
    private long lastObservation;

    @Override public String getId() { return bus.makeLocalServiceId("memoryOptimization"); }
    @Override public boolean start() { return true; }
    @Override public boolean stop() { if (poller != null) { poller.cancel(false); } return true; }
    @Override public void nodeJoin(ManagementNodeInventory node) { }
    @Override public void nodeLeft(ManagementNodeInventory node) {
        repository.expireUnconfirmed(node.getUuid());
        repository.releaseFailureNotifications(node.getUuid());
    }
    @Override public void iAmDead(ManagementNodeInventory node) { stop(); }
    @Override public void iJoin(ManagementNodeInventory node) { }

    @Override public void afterHostConnected(HostInventory host) {
        if (host == null || host.getUuid() == null || host.getUuid().trim().isEmpty()
                || !"Connected".equals(host.getStatus())
                || !KVMConstant.KVM_HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            return;
        }
        final String hostUuid = host.getUuid();
        try {
            // Host connection callbacks run in the connection flow. Only enqueue work here;
            // Agent observation and policy admission happen after the flow has returned.
            thdf.submit(new Task<Void>() {
                @Override public String getName() { return "memory-host-connected-observation-" + hostUuid; }
                @Override public Void call() {
                    observeHostAfterConnect(hostUuid);
                    return null;
                }
            });
        } catch (RuntimeException e) {
            logger.warn(String.format("Unable to enqueue memory observation after Host[%s] connected; periodic scan will retry",
                    hostUuid), e);
        }
    }

    @Override public void managementNodeReady() {
        repository.initialize();
        repository.expireUnconfirmed(Platform.getManagementServerId());
        poller = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override public TimeUnit getTimeUnit() { return TimeUnit.SECONDS; }
            @Override public long getInterval() { return 5; }
            @Override public String getName() { return "memory-optimization-tasks"; }
            @Override public void run() { tick(); }
        });
    }

    @Override public void handleMessage(Message message) {
        try {
            if (message instanceof APIMessage) {
                checkReadAuthorization((APIMessage) message);
            }
            if (message instanceof APIUpdateMemoryPolicyMsg) {
                MemoryApiRequestBudget.validateUpdate((APIUpdateMemoryPolicyMsg) message);
            } else if (message instanceof APIPreviewMemoryPolicyMsg) {
                MemoryApiRequestBudget.validatePreview((APIPreviewMemoryPolicyMsg) message);
            }
            if (message instanceof APIUpdateMemoryPolicyMsg) { update((APIUpdateMemoryPolicyMsg) message); }
            else if (message instanceof APIGetMemoryPolicyMsg) { policy((APIGetMemoryPolicyMsg) message); }
            else if (message instanceof APIPreviewMemoryPolicyMsg) { preview((APIPreviewMemoryPolicyMsg) message); }
            else if (message instanceof APIQueryMemoryStateMsg) { states((APIQueryMemoryStateMsg) message); }
            else if (message instanceof APIQueryMemoryTaskMsg) { tasks((APIQueryMemoryTaskMsg) message); }
            else if (message instanceof APIGetVmMemoryOptimizationMsg) { vmAccounting((APIGetVmMemoryOptimizationMsg) message); }
            else if (message instanceof APIGetVmMemoryOptimizationsMsg) { vmAccountings((APIGetVmMemoryOptimizationsMsg) message); }
            else if (message instanceof APIQueryHostMemoryOperationsMsg) { operations((APIQueryHostMemoryOperationsMsg) message); }
            else if (message instanceof APIGetHostMemoryWritebackBackendsMsg) { writebackBackends((APIGetHostMemoryWritebackBackendsMsg) message); }
            else if (message instanceof APICancelMemoryTaskMsg) { cancel((APICancelMemoryTaskMsg) message); }
            else if (message instanceof APIDeleteMemoryTaskMsg) { deleteTask((APIDeleteMemoryTaskMsg) message); }
            else if (message instanceof APIGetMemorySummaryMsg) { summary((APIGetMemorySummaryMsg) message); }
            else { bus.dealWithUnknownMessage(message); }
        } catch (MemoryOperationException e) {
            bus.replyErrorByMessageType(message, e.toErrorCode());
        } catch (IllegalArgumentException e) {
            bus.replyErrorByMessageType(message, new MemoryOperationException("MEMORY_INVALID_REQUEST", e.getMessage()).toErrorCode());
        }
    }

    /**
     * API permission/session evaluation belongs to the platform authorization
     * backend before service dispatch (including IAM2). This service retains
     * the object-level read boundary: infrastructure data remain admin-only;
     * VM statistics are scoped to resources visible to the session's account.
     * Internal callers must use the normal API entry, not forge a session here.
     */
    void checkReadAuthorization(APIMessage api) {
        if (api.getSession() == null || api.getSession().getAccountUuid() == null) {
            throw new MemoryOperationException("MEMORY_PERMISSION_DENIED", "An authenticated account session is required");
        }

        if (api instanceof APIGetVmMemoryOptimizationMsg || api instanceof APIGetVmMemoryOptimizationsMsg) {
            List<String> accessible;
            try {
                // Reuse owner/public/private sharing semantics without running
                // legacy UserVO policy evaluation on an IAM2 VirtualID session.
                accessible = accountManager.getResourceUuidsCanAccessByAccount(
                        api.getSession().getAccountUuid(), VmInstanceVO.class);
            } catch (RuntimeException e) {
                throw new MemoryOperationException("MEMORY_PERMISSION_DENIED", "The account cannot access the requested VM memory statistics");
            }
            List<String> requested = api instanceof APIGetVmMemoryOptimizationMsg
                    ? Collections.singletonList(((APIGetVmMemoryOptimizationMsg) api).getVmUuid())
                    : ((APIGetVmMemoryOptimizationsMsg) api).getVmUuids();
            if (accessible != null && (requested == null || requested.isEmpty()
                    || !new HashSet<>(accessible).containsAll(requested))) {
                throw new MemoryOperationException("MEMORY_PERMISSION_DENIED", "The account cannot access the requested VM memory statistics");
            }
            return;
        }

        // The remaining memory APIs expose host-wide state, tasks, operations,
        // policy, or controls. Preserve the existing admin-only boundary.
        if (accountManager.getResourceUuidsCanAccessByAccount(api.getSession().getAccountUuid(), HostVO.class) != null) {
            throw new MemoryOperationException("MEMORY_PERMISSION_DENIED", "Infrastructure administrator access is required");
        }
    }

    private long licenseDeadline() {
        List<MemoryLicenseExtensionPoint> providers = pluginRgty.getExtensionList(MemoryLicenseExtensionPoint.class);
        if (providers.size() != 1) { return 0; }
        return providers.get(0).licenseDeadlineMillis();
    }

    private void requireLicense() {
        if (licenseDeadline() <= System.currentTimeMillis()) {
            throw new MemoryOperationException("MEMORY_LICENSE_UNAVAILABLE", "A valid Cloud License is required for memory configuration changes");
        }
    }

    private void update(APIUpdateMemoryPolicyMsg msg) {
        if (MemoryTaskRules.requiresLicenseForConfiguration(msg.getAction(), msg.getPolicy())) { requireLicense(); }
        APIUpdateMemoryPolicyEvent event = new APIUpdateMemoryPolicyEvent(msg.getId());
        event.setInventory(repository.submit(msg));
        bus.publish(event);
    }

    private void policy(APIGetMemoryPolicyMsg msg) {
        APIGetMemoryPolicyReply reply = new APIGetMemoryPolicyReply();
        MemoryPolicyInventory inventory = repository.getPolicy(msg.getScope(), msg.getResourceUuid());
        describe(inventory);
        if ("Global".equals(inventory.getScope())) {
            MemoryCloudBootstrapVO bootstrap = repository.freshCloudBootstrap();
            if (bootstrap != null) {
                inventory.setBootstrapStatus(bootstrap.getStatus());
                inventory.setBootstrapReason(bootstrap.getReason());
            }
        }
        reply.setInventory(inventory);
        bus.reply(msg, reply);
    }

    private void preview(APIPreviewMemoryPolicyMsg msg) {
        APIPreviewMemoryPolicyReply reply = new APIPreviewMemoryPolicyReply();
        reply.setInventory(repository.preview(msg.getScope(), msg.getResourceUuid(), msg.getPolicy(), msg.getAction(), msg.getClearOverrideFields()));
        msg.setScope(reply.getInventory().getScope());
        describe(reply.getInventory());
        List<String> targets = repository.targets(msg.getScope(), msg.getResourceUuid());
        List<String> hostUuids = MemoryTargetRules.select(msg.getScope(), targets, msg.getTargetHostUuids());
        reply.setHostUuids(hostUuids);
        List<MemoryHostPolicyPreviewInventory> hostResults = new ArrayList<>();
        if (!"VM".equals(msg.getScope())) {
            Map<String, MemoryHostPolicyPreviewSnapshot> hostPolicies = repository.previewHostPolicies(
                    msg.getScope(), msg.getResourceUuid(), msg.getPolicy(), msg.getAction(),
                    msg.getClearOverrideFields(), hostUuids);
            for (String host : hostUuids) {
                MemoryHostPolicyPreviewSnapshot snapshot = hostPolicies.get(host);
                Map<String, String> changed = MemoryPolicyAdmissionRules.changedFields(
                        snapshot == null ? null : snapshot.before, snapshot == null ? null : snapshot.after);
                Map<String, String> blocked = MemoryPolicyAdmissionRules.blockedFields(
                        changed, snapshot == null ? null : snapshot.state, System.currentTimeMillis(),
                        snapshot == null ? null : snapshot.after);
                MemoryHostPolicyPreviewInventory hostResult = new MemoryHostPolicyPreviewInventory();
                hostResult.setHostUuid(host);
                List<MemoryBlockedPolicyFieldInventory> blockedFields = new ArrayList<>();
                blocked.forEach((field, reason) -> blockedFields.add(new MemoryBlockedPolicyFieldInventory(field, reason)));
                hostResult.setBlockedFields(blockedFields);
                hostResult.setEligible(blockedFields.isEmpty());
                hostResults.add(hostResult);
            }
        }
        reply.setHostResults(hostResults);
        List<String> warnings = new ArrayList<>();
        List<MemoryPreviewWarningInventory> warningDetails = new ArrayList<>();
        addPreviewWarning(warnings, warningDetails, "VM_EXCLUSION_GLOBAL_SWAP");
        addPreviewWarning(warnings, warningDetails, "HOST_REVALIDATED_BEFORE_APPLY");
        if (licenseDeadline() <= System.currentTimeMillis()) {
            addPreviewWarning(warnings, warningDetails, "CLOUD_LICENSE_REQUIRED");
        }
        reply.setWarnings(warnings);
        reply.setWarningDetails(warningDetails);
        if (MemoryPreviewRules.needsCapacity(reply.getInventory())) {
            String operation = Platform.getUuid();
            call(msg.getResourceUuid(), "preview", command(msg.getResourceUuid(), operation), response -> {
                try {
                    if (response == null || !operation.equals(response.operationUuid)) {
                        throw new IllegalArgumentException("CAPACITY_DEFAULT_UNKNOWN");
                    }
                    MemoryPreviewRules.resolveCapacity(reply.getInventory(), response);
                } catch (IllegalArgumentException error) {
                    addPreviewWarning(warnings, warningDetails, "ZRAM_CAPACITY_UNVERIFIED");
                }
                bus.reply(msg, reply);
            });
        } else {
            bus.reply(msg, reply);
        }
    }

    private void addPreviewWarning(List<String> legacyWarnings,
                                   List<MemoryPreviewWarningInventory> warningDetails,
                                   String code) {
        legacyWarnings.add(MemoryPreviewWarnings.legacyMessage(code));
        warningDetails.add(MemoryPreviewWarnings.create(code));
    }

    private void describe(MemoryPolicyInventory inventory) {
        boolean licensed = licenseDeadline() > System.currentTimeMillis();
        inventory.setSource(policySource(inventory));
        Map<String, MemoryFieldCapability> fields = new LinkedHashMap<>();
        if ("VM".equals(inventory.getScope())) {
            fields.put("participation", new MemoryFieldCapability(licensed, true, "enum", null, "InstanceFenced"));
        } else {
            fields.put("ksm.enabled", new MemoryFieldCapability(licensed, true, "boolean", null, "NextPolicyCycle"));
            fields.put("ksm.zeroPagesEnabled", new MemoryFieldCapability(licensed, true, "boolean", null, "NextPolicyCycle"));
            fields.put("ksm.pagesToScan", new MemoryFieldCapability(licensed, false, "int64", "pages", "NextPolicyCycle"));
            fields.put("ksm.sleepMillis", new MemoryFieldCapability(licensed, false, "int64", "milliseconds", "NextPolicyCycle"));
            addPolicyCapabilities(fields, licensed);
        }
        MemoryStateVO hostState = null;
        MemoryStateInventory hostStateView = null;
        if ("Host".equals(inventory.getScope())) {
            List<MemoryStateVO> states = repository.states(Collections.singletonList(inventory.getResourceUuid()), 0, 1);
            hostState = states.isEmpty() ? null : states.get(0);
            if (hostState != null) {
                long now = System.currentTimeMillis();
                hostStateView = MemoryMetricsPresentation.inventory(hostState, null, now,
                        repository.canResumeControl(hostState.getHostUuid(), hostState.getControlOperationUuid()),
                        repository.canReconcileRejectedResume(hostState.getHostUuid(), hostState.getControlOperationUuid()));
            }
        }
        MemoryAllowedActionRules.Decision actions = MemoryAllowedActionRules.evaluate(
                licensed, inventory.getScope(), fields, hostState, hostStateView, System.currentTimeMillis());
        inventory.setAllowedActions(actions.getAllowedActions());
        inventory.setPreflightRequiredActions(actions.getPreflightRequiredActions());
        inventory.setFieldCapabilities(fields);
    }

    /**
     * A policy may inherit different fields from different scopes. Keep the
     * fieldSources map authoritative and expose a single source only when all
     * configured fields agree; schema defaults are not an override source.
     */
    private static String policySource(MemoryPolicyInventory inventory) {
        Set<String> sources = new LinkedHashSet<>();
        Map<String, String> fieldSources = inventory.getFieldSources();
        if (fieldSources != null) {
            for (String source : fieldSources.values()) {
                if (source == null || source.isEmpty() || "Default".equals(source)) { continue; }
                int separator = source.indexOf(':');
                sources.add(separator < 0 ? source : source.substring(0, separator));
            }
        }
        if (sources.size() == 1) { return sources.iterator().next(); }
        if (sources.size() > 1) { return "Mixed"; }
        return "Default";
    }

    /** Keep the public policy schema and the discovery contract in lockstep. */
    private static void addPolicyCapabilities(Map<String, MemoryFieldCapability> fields, boolean writable) {
        fields.put("schemaVersion", capability(writable, false, "int32", "version", "PersistedSchema"));
        fields.put("zram.enabled", capability(writable, true, "boolean", null, "EnableOrDrain"));
        // VM selection is a CLI/advanced policy control; the basic UI only
        // exposes enablement, capacity, inheritance and status per FS 14.2.
        fields.put("zram.selectionMode", capability(writable, false, "enum", null, "NextPolicyCycle"));
        fields.put("zram.selectedVmUuids", capability(writable, false, "uuid[]", null, "InstanceFenced"));
        for (String field : Arrays.asList("discoveryIntervalSeconds", "discoveryTimeoutSeconds", "discoveryTtlSeconds",
                "hostSampleIntervalSeconds", "hostSampleTtlSeconds", "vmSampleIntervalSeconds", "vmSampleTtlSeconds",
                "startupObservationSeconds")) {
            fields.put("zram." + field, capability(writable, false, "int64", "seconds", "ObservationWindow"));
        }
        fields.put("zram.hostCpuGuardEnabled", capability(writable, false, "boolean", null, "NextPolicyCycle"));
        fields.put("zram.hostMemoryPsiGuardEnabled", capability(writable, false, "boolean", null, "NextPolicyCycle"));
        fields.put("zram.hostIoPsiGuardEnabled", capability(writable, false, "boolean", null, "NextPolicyCycle"));
        for (String field : Arrays.asList("hostCpuThresholdPercent", "hostMemoryPsiThresholdPercent", "hostIoPsiThresholdPercent")) {
            fields.put("zram." + field, capability(writable, false, "float64", "percent", "NextPolicyCycle"));
        }
        fields.put("zram.reclaimBatchBytes", capability(writable, false, "int64", "bytes", "NextPolicyCycle"));
        fields.put("zram.concurrency", capability(writable, false, "int64", "count", "NextPolicyCycle"));
        fields.put("zram.timeoutIsolationSlots", capability(writable, false, "int64", "count", "NextPolicyCycle"));
        fields.put("zram.reclaimSlowOperationSeconds", capability(writable, false, "int64", "seconds", "NextPolicyCycle"));
        fields.put("zram.operationRecordBudgetBytes", capability(writable, false, "int64", "bytes", "RecordRetention"));
        fields.put("zram.algorithm", capability(writable, false, "enum", null, "NextPolicyCycle"));
        for (String field : Arrays.asList("logicalCapacityBytes", "ramLimitBytes", "requiredAvailableBytes", "hostFloorBytes",
                "metadataReserveBytes", "transientReserveBytes")) {
            fields.put("zram." + field, capability(writable, "logicalCapacityBytes".equals(field) || "ramLimitBytes".equals(field),
                    "int64", "bytes", "CapacityChecked"));
        }
        for (String field : Arrays.asList("cpuThresholdPercent")) {
            fields.put("zram." + field, capability(writable, false, "int64", "percent", "NextPolicyCycle"));
        }
        for (String field : Arrays.asList("cpuHoldSeconds", "operationIntervalSeconds")) {
            fields.put("zram." + field, capability(writable, false, "int64", "seconds", "NextPolicyCycle"));
        }
        fields.put("writeback.enabled", capability(writable, false, "boolean", null, "OfflinePreparationMayBeRequired"));
        fields.put("writeback.backendResourceUuid", capability(writable, false, "uuid", null, "DrainAndOfflinePreparation"));
        for (String field : Arrays.asList("backendCapacityBytes", "batchBytes", "metadataBudgetBytes", "filesystemReserveBytes")) {
            fields.put("writeback." + field, capability(writable, false, "int64", "bytes", "DrainAndOfflinePreparation"));
        }
        for (String field : Arrays.asList("idleObservationSeconds", "operationIntervalSeconds", "slowOperationSeconds",
                "backoffSeconds", "ioErrorBackoffSeconds")) {
            fields.put("writeback." + field, capability(writable, false, "int64", "seconds", "DrainAndOfflinePreparation"));
        }
        fields.put("writeback.noProgressLimit", capability(writable, false, "int64", "count", "DrainAndOfflinePreparation"));
    }

    private static MemoryFieldCapability capability(boolean writable, boolean basicUi, String type, String unit, String effect) {
        return new MemoryFieldCapability(writable, basicUi, type, unit, effect);
    }

    private void states(APIQueryMemoryStateMsg msg) {
        APIQueryMemoryStateReply reply = new APIQueryMemoryStateReply();
        MemoryQueryPage<MemoryStateVO> page = repository.statePage(msg.getHostUuids(), msg.getStart(), msg.getLimit(), msg.getSnapshotId());
        long now = System.currentTimeMillis();
        Map<String, Map<String, Object>> samples = hostMetricSamples(page.items.stream()
                .map(MemoryStateVO::getHostUuid).collect(Collectors.toList()), now);
        List<MemoryStateInventory> inventories = page.items
                .stream().map(state -> MemoryMetricsPresentation.inventory(state,
                        samples.get(state.getHostUuid()), now,
                        repository.canResumeControl(state.getHostUuid(), state.getControlOperationUuid()),
                        repository.canReconcileRejectedResume(state.getHostUuid(), state.getControlOperationUuid())))
                .collect(Collectors.toList());
        reply.setInventories(inventories); reply.setTotal(page.total); reply.setSnapshotId(page.snapshotId);
        reply.setNextPage(nextPage(msg.getStart(), inventories.size(), reply.getTotal())); bus.reply(msg, reply);
    }

    private void tasks(APIQueryMemoryTaskMsg msg) {
        APIQueryMemoryTaskReply reply = new APIQueryMemoryTaskReply();
        MemoryQueryPage<MemoryTaskInventory> page = repository.taskPage(msg);
        reply.setInventories(page.items); reply.setTotal(page.total); reply.setSnapshotId(page.snapshotId);
        reply.setNextPage(nextPage(msg.getStart(), reply.getInventories().size(), reply.getTotal())); bus.reply(msg, reply);
    }

    static Integer nextPage(int start, int size, long total) {
        long next = (long) start + size;
        return size > 0 && next < total && next <= Integer.MAX_VALUE ? (int) next : null;
    }

    private void vmAccounting(APIGetVmMemoryOptimizationMsg msg) {
        List<String> hosts = repository.targets("VM", msg.getVmUuid());
        APIGetVmMemoryOptimizationReply reply = new APIGetVmMemoryOptimizationReply();
        if (hosts.isEmpty()) {
            MemoryVmAccountingInventory unavailable = new MemoryVmAccountingInventory();
            unavailable.setVmUuid(msg.getVmUuid()); unavailable.setQuality("unavailable");
            unavailable.setReason("VM_HAS_NO_CURRENT_HOST");
            reply.setInventory(unavailable); bus.reply(msg, reply); return;
        }
        String host = hosts.get(0);
        callVmMetrics(host, Collections.singletonList(msg.getVmUuid()), response -> {
            if (!hosts.equals(repository.targets("VM", msg.getVmUuid()))) {
                bus.replyErrorByMessageType(msg, new MemoryOperationException("MEMORY_INSTANCE_CHANGED", "VM migrated; refresh the observation").toErrorCode());
                return;
            }
            if (response == null || !response.isSuccess() || response.state == null) {
                bus.replyErrorByMessageType(msg, new MemoryOperationException("MEMORY_OBSERVATION_UNAVAILABLE", "Host accounting observation unavailable").toErrorCode());
                return;
            }
            reply.setInventory(vmObservation(host, msg.getVmUuid(), response)); bus.reply(msg, reply);
        });
    }

    /** One read-only Prometheus snapshot per Host, then expose only requested VMs. */
    private void vmAccountings(APIGetVmMemoryOptimizationsMsg msg) {
        List<String> requested = new ArrayList<>(new LinkedHashSet<>(msg.getVmUuids()));
        Map<String, String> hosts = repository.currentVmHosts(requested);
        Map<String, MemoryVmAccountingInventory> result = new LinkedHashMap<>();
        Map<String, List<String>> byHost = new LinkedHashMap<>();
        for (String vm : requested) {
            String host = hosts.get(vm);
            if (host == null) {
                result.put(vm, unavailableVm(vm, "VM_HAS_NO_CURRENT_HOST"));
            } else {
                byHost.computeIfAbsent(host, ignored -> new ArrayList<>()).add(vm);
            }
        }
        new VmAccountingBatch(msg, requested, hosts, new ArrayList<>(byHost.keySet()), byHost, result).start();
    }

    /** Fixed-width Prometheus fan-out on the shared thread facade. */
    private final class VmAccountingBatch {
        private static final int WIDTH = MemoryVmAccountingBatchRules.MAX_IN_FLIGHT_HOSTS;
        private static final long DEADLINE_SECONDS = MemoryVmAccountingBatchRules.DEADLINE_SECONDS;
        private final APIGetVmMemoryOptimizationsMsg msg;
        private final List<String> requested;
        private final Map<String, String> initialHosts;
        private final List<String> hostList;
        private final Map<String, List<String>> byHost;
        private final Map<String, MemoryVmAccountingInventory> result;
        private int next;
        private int active;
        private boolean finished;

        private VmAccountingBatch(APIGetVmMemoryOptimizationsMsg msg, List<String> requested, Map<String, String> initialHosts,
                                  List<String> hostList, Map<String, List<String>> byHost,
                                  Map<String, MemoryVmAccountingInventory> result) {
            this.msg = msg; this.initialHosts = initialHosts; this.hostList = hostList;
            this.requested = new ArrayList<>(requested);
            this.byHost = byHost; this.result = result;
        }

        private void start() {
            thdf.submitTimeoutTask(this::deadline, TimeUnit.SECONDS, DEADLINE_SECONDS);
            synchronized (this) { launchLocked(); }
        }

        private void launchLocked() {
            while (!finished && active < WIDTH && next < hostList.size()) {
                String host = hostList.get(next++);
                active++;
                List<String> vmUuids = byHost.get(host);
                callVmMetrics(host, vmUuids, response -> completed(host, vmUuids, response));
            }
            if (!finished && active == 0 && next >= hostList.size()) { finishLocked(); }
        }

        private void completed(String host, List<String> vmUuids, MemoryAgentResponse response) {
            synchronized (this) {
                if (finished) { return; }
                Map<String, String> currentHosts = repository.currentVmHosts(vmUuids);
                boolean hostFailed = response == null || !response.isSuccess() || response.state == null;
                for (String vm : vmUuids) {
                    boolean placementChanged = !host.equals(initialHosts.get(vm)) || !host.equals(currentHosts.get(vm));
                    String unavailableReason = MemoryVmAccountingBatchRules.unavailableReason(!hostFailed, placementChanged);
                    if (unavailableReason != null) {
                        result.put(vm, unavailableVm(vm, unavailableReason));
                    } else {
                        result.put(vm, vmObservation(host, vm, response));
                    }
                }
                active--; launchLocked();
            }
        }

        private void deadline() {
            synchronized (this) {
                if (finished) { return; }
                // next counts dispatched hosts, not completed hosts. Include both
                // queued and in-flight VMs, without replacing completed results.
                for (String vm : requested) {
                    if (!result.containsKey(vm)) {
                        result.put(vm, unavailableVm(vm, "BATCH_DEADLINE_EXCEEDED"));
                    }
                }
                finishLocked();
            }
        }

        private void finishLocked() {
            if (finished) { return; }
            finished = true;
            APIGetVmMemoryOptimizationsReply reply = new APIGetVmMemoryOptimizationsReply();
            reply.setInventories(new LinkedHashMap<>(result)); bus.reply(msg, reply);
        }
    }

    private MemoryVmAccountingInventory unavailableVm(String vm, String reason) {
        MemoryVmAccountingInventory unavailable = new MemoryVmAccountingInventory();
        unavailable.setVmUuid(vm); unavailable.setQuality("unavailable"); unavailable.setReason(reason);
        return unavailable;
    }

    /** Prometheus callback response is scoped by callVmMetrics(host), not an Agent hostUuid envelope. */
    static MemoryVmAccountingInventory vmObservation(String host, String vm, MemoryAgentResponse response) {
        return MemoryObservationRules.vm(host, vm, response.state);
    }

    private void operations(APIQueryHostMemoryOperationsMsg msg) {
        repository.targets("Host", msg.getHostUuid());
        Map<String, Object> request = command(msg.getHostUuid(), Platform.getUuid());
        request.put("operationId", msg.getOperationId()); request.put("vmUuid", msg.getVmUuid());
        request.put("status", msg.getStatus()); request.put("start", msg.getStart()); request.put("limit", msg.getLimit());
        call(msg.getHostUuid(), "operations", request, response -> {
            if (response == null || !response.isSuccess() || response.state == null || !msg.getHostUuid().equals(response.hostUuid)) {
                bus.replyErrorByMessageType(msg, new MemoryOperationException("MEMORY_OPERATION_QUERY_UNAVAILABLE", "Host execution records unavailable; no operation was replayed").toErrorCode());
                return;
            }
            try {
                APIQueryHostMemoryOperationsReply reply = new APIQueryHostMemoryOperationsReply();
                reply.setInventory(MemoryHostOperationsInventory.fromAgentState(msg.getHostUuid(), response.hostUuid, response.state)); bus.reply(msg, reply);
            } catch (MemoryOperationException malformed) {
                bus.replyErrorByMessageType(msg, malformed.toErrorCode());
            }
        });
    }

    private void writebackBackends(APIGetHostMemoryWritebackBackendsMsg msg) {
        repository.targets("Host", msg.getHostUuid());
        call(msg.getHostUuid(), "writeback-backends", command(msg.getHostUuid(), Platform.getUuid()), response -> {
            if (response == null || !response.isSuccess() || response.state == null
                    || !msg.getHostUuid().equals(response.hostUuid)
                    || !msg.getHostUuid().equals(response.state.get("hostUuid"))) {
                bus.replyErrorByMessageType(msg, new MemoryOperationException("MEMORY_BACKEND_QUERY_UNAVAILABLE",
                        "Writeback device observation unavailable; no storage was modified").toErrorCode());
                return;
            }
            try {
                APIGetHostMemoryWritebackBackendsReply reply = new APIGetHostMemoryWritebackBackendsReply();
                reply.setInventory(MemoryWritebackBackendInventory.fromAgentState(
                        msg.getHostUuid(), response.hostUuid, response.state));
                bus.reply(msg, reply);
            } catch (IllegalArgumentException malformed) {
                bus.replyErrorByMessageType(msg, new MemoryOperationException("MEMORY_BACKEND_QUERY_UNAVAILABLE",
                        "Writeback device observation is malformed; no storage was modified").toErrorCode());
            }
        });
    }

    private void cancel(APICancelMemoryTaskMsg msg) {
        APICancelMemoryTaskEvent event = new APICancelMemoryTaskEvent(msg.getId());
        event.setInventory(repository.cancel(msg.getUuid())); bus.publish(event);
    }

    private void deleteTask(APIDeleteMemoryTaskMsg msg) {
        APIDeleteMemoryTaskEvent event = new APIDeleteMemoryTaskEvent(msg.getId());
        MemoryTaskDeleteResult result = repository.deleteTaskTree(msg.getUuid());
        event.setTaskUuid(result.getTaskUuid()); event.setDeleted(result.isDeleted());
        event.setScope(result.getScope()); event.setResourceUuid(result.getResourceUuid());
        event.setHostUuids(result.getHostUuids());
        bus.publish(event);
    }

    private void summary(APIGetMemorySummaryMsg msg) {
        APIGetMemorySummaryReply reply = new APIGetMemorySummaryReply();
        List<String> hosts = repository.summaryTargets(msg.getHostUuids(), msg.getZoneUuid());
        long now = System.currentTimeMillis();
        Map<String, Map<String, Object>> samples = hostMetricSamples(hosts, now);
        List<MemoryStateVO> states = repository.allStates(hosts).stream()
                .map(state -> MemoryMetricsPresentation.withSavings(state, samples.get(state.getHostUuid())))
                .collect(Collectors.toList());
        reply.setSummary(MemorySummaryRules.summarize(hosts, states, now));
        bus.reply(msg, reply);
    }

    private MemoryMetricsExtensionPoint metricProvider() {
        List<MemoryMetricsExtensionPoint> providers = pluginRgty.getExtensionList(MemoryMetricsExtensionPoint.class);
        return providers.size() == 1 ? providers.get(0) : null;
    }

    private Map<String, Map<String, Object>> hostMetricSamples(List<String> hosts, long now) {
        if (hosts.isEmpty()) { return Collections.emptyMap(); }
        try {
            MemoryMetricsExtensionPoint provider = metricProvider();
            Map<String, Map<String, Object>> samples = provider == null ? null
                    : provider.hostSavings(hosts, now, MemoryOptimizationGlobalConfig.displayTtlMillis());
            return samples == null ? Collections.emptyMap() : samples;
        } catch (RuntimeException e) {
            logger.warn("Memory monitoring query unavailable; cached Agent metrics are not a fallback", e);
            return Collections.emptyMap();
        }
    }

    private void callVmMetrics(String host, List<String> vms, Consumer<MemoryAgentResponse> callback) {
        thdf.submit(new org.zstack.core.thread.Task<Void>() {
            @Override public String getName() { return "memory-prometheus-vm-observation"; }
            @Override public Void call() {
                MemoryAgentResponse response = new MemoryAgentResponse();
                try {
                    MemoryMetricsExtensionPoint provider = metricProvider();
                    response.state = provider == null ? null : provider.vmAccounting(host, vms,
                            System.currentTimeMillis(), MemoryOptimizationGlobalConfig.displayTtlMillis());
                    response.setSuccess(response.state != null);
                } catch (RuntimeException e) {
                    response.setSuccess(false);
                    logger.warn("VM memory Prometheus query unavailable", e);
                }
                callback.accept(response);
                return null;
            }
        });
    }

    private void tick() {
        if (!ticking.compareAndSet(false, true)) { return; }
        try {
            processFreshCloudBootstrap();
            for (MemoryTaskVO task : repository.claim(Platform.getManagementServerId())) { dispatch(task, false); }
            for (MemoryTaskVO task : repository.outstanding(Platform.getManagementServerId())) { dispatch(task, true); }
            dispatchFailureNotifications(Platform.getManagementServerId());
            if (System.currentTimeMillis() - lastObservation >= 30000) {
                lastObservation = System.currentTimeMillis(); observeRuntime();
            }
        } catch (Exception e) {
            logger.warn("Memory optimization task polling failed; no task is assumed successful", e);
        } finally { ticking.set(false); }
    }

    private void processFreshCloudBootstrap() {
        MemoryCloudBootstrapVO bootstrap = repository.freshCloudBootstrap();
        if (bootstrap == null || !MemoryCloudBootstrapVO.PENDING.equals(bootstrap.getStatus())) { return; }
        String blocked = repository.freshCloudBootstrapBlockReason();
        if (blocked != null) {
            repository.updateFreshCloudBootstrapReason(blocked);
            return;
        }
        if (licenseDeadline() <= System.currentTimeMillis()) {
            repository.updateFreshCloudBootstrapReason("MEMORY_LICENSE_UNAVAILABLE");
            return;
        }
        try {
            repository.submitFreshCloudBootstrap(bootstrap.getRequestUuid());
        } catch (MemoryOperationException e) {
            repository.updateFreshCloudBootstrapReason(e.getCode());
        } catch (RuntimeException e) {
            logger.warn("Fresh-cloud memory defaults remain pending; no task is assumed accepted", e);
            repository.updateFreshCloudBootstrapReason("BOOTSTRAP_ADMISSION_RETRY");
        }
    }

    private void dispatchFailureNotifications(String owner) {
        for (MemoryTaskFailureOutboxVO outbox : repository.claimFailureNotifications(owner)) {
            MemoryTaskFailureNotificationMsg msg = new MemoryTaskFailureNotificationMsg();
            msg.setEvent(outbox.toEvent());
            bus.makeTargetServiceIdByResourceUuid(msg, "zwatch.alarm", outbox.getHostUuid());
            bus.send(msg, new CloudBusCallBack(null) {
                @Override public void run(MessageReply reply) {
                    boolean success = reply != null && reply.isSuccess();
                    String error = success || reply == null || reply.getError() == null
                            ? null : reply.getError().getDetails();
                    try {
                        repository.completeFailureNotification(outbox.getTaskUuid(), owner, success, error);
                    } catch (RuntimeException e) {
                        logger.warn(String.format("Unable to update memory failure outbox task[%s] delivery state",
                                outbox.getTaskUuid()), e);
                    }
                }
            });
        }
    }

    private void dispatch(MemoryTaskVO task, boolean poll) {
        // VM reconcile has two durable concerns: the host-side operation and
        // the migration hold. Resolve the latter through the lifecycle hook so
        // a management-node restart can recover a persisted hold without
        // replaying an apply or relying on an in-memory callback.
        if ("reconcile".equals(task.getAction()) && "VM".equals(task.getScope())
                && task.getReconcileOperationUuid() == null) {
            lifecycleHooks.reconcileMigrationHold(task.getResourceUuid(), new Completion(null) {
                @Override public void success() {
                    repository.result(task.getUuid(), "Succeeded", "VM migration hold reconciled", null);
                }
                @Override public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                    repository.result(task.getUuid(), "Unknown", errorCode == null ? "VM migration hold remains unknown" : errorCode.getDetails(), null);
                }
            });
            return;
        }
        boolean active = Arrays.asList("apply", "clearOverride").contains(task.getAction());
        // Repository tasks may contain the materialized effective policy.
        // Reconcile's empty user payload was already validated at admission;
        // dispatch sends only a result query, never that materialized policy.
        boolean configuration = !"reconcile".equals(task.getAction());
        if (configuration && !poll && licenseDeadline() <= System.currentTimeMillis()) {
            if (MemoryUncertainRecoveryRules.ACTION.equals(task.getAction())) {
                repository.recoveryLicenseExpiredBeforeDispatch(task.getUuid()); return;
            }
            repository.result(task.getUuid(), "Failed", "Cloud License expired before dispatch", null); return;
        }
        Map<String, Object> command = command(task.getHostUuid(), task.getUuid());
        command.put("scope", task.getScope()); command.put("resourceUuid", task.getResourceUuid());
        command.put("action", "clearOverride".equals(task.getAction()) ? "apply" : task.getAction());
        if (active) {
            String policyPlanHash = task.getPolicyPlanHash();
            command.put("policyMode", policyPlanHash != null && !policyPlanHash.trim().isEmpty()
                    ? "delta" : "effective");
        }
        command.put("desiredRevision", task.getDesiredRevision());
        // The Agent's VM-list shard protocol needs one stable generation for
        // every chunk. The policy revision is the MN snapshot generation for
        // a configuration task; it is never refreshed between chunks.
        command.put("targetSnapshotGeneration", "policy-" + task.getDesiredRevision());
        command.put("expectedInstanceGeneration", task.getExpectedInstanceGeneration());
        command.put("expectedControlOperationUuid", task.getExpectedControlOperationUuid());
        command.put("policy", active ? MemoryPolicyRules.decode(task.getPolicy()) : Collections.emptyMap());
        if (MemoryBackendPreparationRules.ACTION.equals(task.getAction())) {
            command.put("backendPreparation", JSONObjectUtil.toObject(task.getPolicy(), Map.class));
        }
        if (MemoryZramPoolPreparationRules.ACTION.equals(task.getAction())) {
            command.put("poolPreparation", JSONObjectUtil.toObject(task.getPolicy(), Map.class));
        }
        if (MemoryUncertainRecoveryRules.ACTION.equals(task.getAction())) {
            command.put("recovery", JSONObjectUtil.toObject(task.getPolicy(), Map.class));
        }
        if (active && !"VM".equals(task.getScope())) {
            List<MemoryStateVO> observed = repository.states(Collections.singletonList(task.getHostUuid()), 0, 1);
            if (!observed.isEmpty()) {
                Object proof = maintenanceProofForCommand(observed.get(0).getState());
                if (proof != null) { command.put("maintenanceProof", proof); }
            }
        }
        String operation = task.getUuid();
        if ("reconcile".equals(task.getAction()) && task.getReconcileOperationUuid() != null) {
            operation = task.getReconcileOperationUuid(); command.put("operationUuid", operation);
            MemoryTaskVO original = repository.findTask(operation);
            if (reconcilableUnknownTask(original)) {
                if (rejectedResumeRecoveryTask(original)) {
                    command.put("reconcileAction", "verify-rejected-resume");
                } else {
                    command.put("reconcileAction", "seal-if-never-issued");
                }
                // Do not manufacture a historical NOT_SENT classification.
                // The Agent owns the same-boot journal/lock proof. Only a
                // task whose persisted reason already proves Host admission
                // failure may carry the stronger legacy hint.
                if (knownNotSentTask(original) && !rejectedResumeRecoveryTask(original)) {
                    command.put("classification", "NOT_SENT");
                    command.put("admissionConnected", false);
                }
                if (original.getCreateDate() != null) {
                    command.put("taskSubmittedAt", original.getCreateDate().getTime());
                }
                String bootId = observedBootId(task.getHostUuid());
                if (bootId != null) { command.put("bootId", bootId); }
            }
        }
        final String queriedOperation = operation;
        String endpoint = poll || "reconcile".equals(task.getAction()) ? "reconcile" : "apply";
        try {
            call(task.getHostUuid(), endpoint, command, response -> {
                String status = MemoryResultRules.status(queriedOperation, active ? task.getDesiredRevision() : -1, response);
                if ("reconcile".equals(task.getAction()) && "VM".equals(task.getScope())
                        && "Succeeded".equals(status)) {
                    // A resolved migration hold is not proof that the original
                    // VM configuration operation succeeded. Verify both journals.
                    lifecycleHooks.reconcileMigrationHold(task.getResourceUuid(), new Completion(null) {
                        @Override public void success() {
                            repository.result(task.getUuid(), status, response.reason, response);
                        }
                        @Override public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                            repository.result(task.getUuid(), "Unknown", errorCode == null
                                    ? "VM migration hold remains unknown" : errorCode.getDetails(), response);
                        }
                    });
                    return;
                }
                String reason = response == null ? "No confirmed Agent result"
                        : (response.reason != null ? response.reason : response.reasonCode);
                if ("resume".equals(task.getAction()) && response != null
                        && "CONTROL_OPERATION_FENCED".equals(response.reasonCode)) {
                    // Persist the exact structured precondition rejection. The
                    // narrowly scoped reconcile path depends on this code and
                    // never trusts free-form Agent stderr/reason text.
                    reason = "CONTROL_OPERATION_FENCED";
                }
                repository.result(task.getUuid(), status,
                        ("Succeeded".equals(status) && reason == null) ? null
                                : (reason == null ? "Agent returned no reason" : reason), response);
            });
        } catch (RuntimeException error) {
            // A synchronous transport/serialization exception does not prove
            // whether the Agent received the command. Keep the operation
            // conservative and recoverable instead of leaving Applying or
            // incorrectly declaring a failed configuration.
            repository.result(task.getUuid(), "Unknown", "Memory Agent dispatch outcome is unknown: " + error.getMessage(), null);
        }
    }

    /** Preserve the exact trusted maintenance proof on the apply wire. The
     * repository has already fenced admission against this same snapshot;
     * the Agent revalidates it immediately before changing native state. */
    static Object maintenanceProofForCommand(String raw) {
        if (raw == null || raw.trim().isEmpty()) { return null; }
        try {
            Map<?, ?> root = JSONObjectUtil.toObject(raw, Map.class);
            Object proof = root.get("maintenanceProof");
            return proof instanceof Map ? proof : null;
        } catch (RuntimeException ignored) { return null; }
    }

    private boolean knownNotSentTask(MemoryTaskVO task) {
        if (rejectedResumeRecoveryTask(task)) { return true; }
        if (task == null || !"Unknown".equals(task.getStatus())) { return false; }
        String reason = task.getReason();
        return reason != null && (reason.contains("HOST.1011") || reason.contains("HOST.1010")
                || reason.contains("MEMORY_HOST_NOT_CONNECTED") || reason.contains("HOST_ADMISSION_NOT_SENT"));
    }

    private boolean reconcilableUnknownTask(MemoryTaskVO task) {
        return task != null && ("Unknown".equals(task.getStatus()) || rejectedResumeRecoveryTask(task));
    }

    private boolean rejectedResumeRecoveryTask(MemoryTaskVO task) {
        return task != null && "Host".equals(task.getScope()) && "resume".equals(task.getAction())
                && "Failed".equals(task.getStatus())
                && "CONTROL_OPERATION_FENCED".equals(task.getReason())
                && task.getExpectedControlOperationUuid() != null;
    }

    private String observedBootId(String host) {
        List<MemoryStateVO> states = repository.states(Collections.singletonList(host), 0, 1);
        if (states.isEmpty()) { return null; }
        try {
            Map<?, ?> root = JSONObjectUtil.toObject(states.get(0).getState(), Map.class);
            Object boot = root.get("bootId");
            return boot instanceof String && !((String) boot).trim().isEmpty() ? (String) boot : null;
        } catch (RuntimeException ignored) { return null; }
    }

    private void observeRuntime() {
        List<String> hosts = repository.targets("Global", "global");
        for (String host : observationCursor.next("global", hosts,
                MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.HOST_BATCH_SIZE, 100))) {
            observeHostAfterConnect(host);
        }
    }

    /** Shared event/scan path: read-only observation first, then the repository's guarded coordinator. */
    void observeHostAfterConnect(String host) {
        if (host == null || host.trim().isEmpty() || !observingHosts.add(host)) { return; }
        try {
            call(host, "state", command(host, Platform.getUuid()), response -> {
                try {
                    if (successfulFreshObservation(response)) {
                        repository.observe(host, response);
                        applyInheritedPolicyAfterObservation(host);
                        lifecycleHooks.reconcileObservedBindings(host, response);
                    }
                } catch (RuntimeException e) {
                    logger.warn(String.format("Host[%s] memory observation reconciliation failed; periodic scan will retry", host), e);
                } finally {
                    observingHosts.remove(host);
                }
            });
        } catch (RuntimeException e) {
            observingHosts.remove(host);
            logger.warn(String.format("Host[%s] memory observation could not be sent; periodic scan will retry", host), e);
        }
    }

    private static boolean successfulFreshObservation(MemoryAgentResponse response) {
        if (response == null || !response.isSuccess() || !"Succeeded".equals(response.status)
                || response.state == null || response.sampleTime == null) { return false; }
        long now = System.currentTimeMillis();
        long age = now - response.sampleTime;
        return response.sampleTime <= now + 5000 && age >= 0
                && age < MemoryOptimizationGlobalConfig.displayTtlMillis();
    }

    /**
     * Reconcile new/reconnected Hosts from the current Global/Cluster/Host
     * policy only after a fresh observation. This is not a first-install
     * detector; admission and the persisted policy hash make it idempotent.
     */
    private void applyInheritedPolicyAfterObservation(String host) {
        if (licenseDeadline() <= System.currentTimeMillis()) { return; }
        try { repository.applyEffectivePolicyAfterConnect(host); }
        catch (MemoryOperationException e) {
            logger.debug(String.format("Host[%s] memory policy remains unapplied: %s", host, e.getCode()));
        } catch (RuntimeException e) {
            logger.warn(String.format("Host[%s] memory policy reconciliation was not accepted", host), e);
        }
    }

    private Map<String, Object> command(String host, String operation) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("hostUuid", host); command.put("operationUuid", operation); return command;
    }

    private final MemoryBatchCursor observationCursor = new MemoryBatchCursor();

    void call(String host, String action, Map<String, Object> command, Consumer<MemoryAgentResponse> completion) {
        Map<String, Integer> communication = new LinkedHashMap<>();
        communication.put("requestBytes", MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.REQUEST_BYTES, 1 << 20));
        communication.put("responseBytes", MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.RESPONSE_BYTES, 16 << 20));
        communication.put("communicationSeconds", MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.COMMUNICATION_SECONDS, 30));
        command = new LinkedHashMap<>(command); command.put("communication", communication);
        final Map<String, Object> dispatchedCommand = command;
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(host); msg.setPath("/memory/optimization/" + action); msg.setCommand(command);
        // Read-only observations must still be obtainable while the cached
        // KVMHost status is reconnecting.  KVMHost's normal status gate can
        // reject the HTTP call before it reaches the Agent, which otherwise
        // collapses into the misleading MEMORY_*_UNAVAILABLE envelope.
        if (readOnlyAgentQuery(action)) {
            msg.setNoStatusCheck(true);
        }
        msg.setTimeout(communication.get("communicationSeconds") * 1000L);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host);
        bus.send(msg, new CloudBusCallBack(msg) {
            @Override public void run(MessageReply reply) {
                MemoryAgentResponse response = null;
                if (!reply.isSuccess()) {
                    logger.warn(String.format("memory Agent call failed: host=%s, action=%s, error=%s",
                            host, action, reply.getError()));
                    response = transportFailure(dispatchedCommand, reply.getError() == null ? null : reply.getError().getDetails());
                } else if (reply instanceof KVMHostAsyncHttpCallReply) {
                    try { response = ((KVMHostAsyncHttpCallReply) reply).toResponse(MemoryAgentResponse.class); }
                    catch (RuntimeException e) {
                        logger.warn("Invalid memory Agent response", e);
                        response = transportFailure(dispatchedCommand, "Invalid Agent response: " + e.getMessage());
                    }
                }
                completion.accept(response);
            }
        });
    }

    static boolean readOnlyAgentQuery(String action) {
        return "state".equals(action) || "capabilities".equals(action)
                || "preview".equals(action) || "accounting".equals(action)
                || "operations".equals(action) || "reconcile".equals(action);
    }

    private static MemoryAgentResponse transportFailure(Map<String, Object> command, String reason) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        Object operation = command.get("operationUuid");
        response.operationUuid = operation == null ? null : String.valueOf(operation);
        response.status = "Unknown";
        response.reason = reason == null ? "Host transport rejected the request before Agent execution" : reason;
        response.setSuccess(false);
        response.setError(response.reason);
        return response;
    }
}
