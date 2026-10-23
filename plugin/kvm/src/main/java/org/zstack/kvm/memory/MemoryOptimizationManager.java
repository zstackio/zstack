package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.identity.AccountManager;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.core.thread.Task;
import org.zstack.header.message.*;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQLContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MemoryOptimizationManager extends AbstractService implements ManagementNodeReadyExtensionPoint,
        ManagementNodeChangeListener, HostAfterConnectedExtensionPoint, GlobalConfigInitExtensionPoint,
        MarshalZQLASTTreeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MemoryOptimizationManager.class);
    @Autowired private CloudBus bus;
    @Autowired private EventFacade evtf;
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

    /**
     * Supplies memory defaults during GlobalConfig initialization on management
     * node startup. The legacy host.ksm row identifies an existing cloud even
     * without KVM Hosts; an existing cloud keeps its stored values and code defaults.
     */
    @Override
    public List<GlobalConfig> getGenerationGlobalConfig() {
        GlobalConfigVO hostKsm = Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, KVMGlobalConfig.CATEGORY)
                .eq(GlobalConfigVO_.name, KVMGlobalConfig.HOST_KSM.getName()).find();
        if (hostKsm != null) {
            List<GlobalConfig> existing = new ArrayList<>();
            preserveExisting(existing, KVMGlobalConfig.HOST_KSM);
            preserveExisting(existing, MemoryStandardGlobalConfig.KSM_ZERO_PAGES);
            preserveExisting(existing, MemoryStandardGlobalConfig.ZRAM_ENABLED);
            return existing;
        }

        // A new cloud enables KSM; an upgraded cloud keeps the legacy host.ksm default (none).
        List<GlobalConfig> configs = new ArrayList<>();
        configs.add(config(KVMGlobalConfig.HOST_KSM, "enable host ksm", "true"));
        configs.add(config(MemoryStandardGlobalConfig.KSM_ZERO_PAGES,
                "KSM zero-page scanning", "true"));
        configs.add(config(MemoryStandardGlobalConfig.ZRAM_ENABLED,
                "Enable ZRAM", "true"));
        return configs;
    }

    private void preserveExisting(List<GlobalConfig> configs, GlobalConfig source) {
        GlobalConfigVO row = Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, source.getCategory())
                .eq(GlobalConfigVO_.name, source.getName()).find();
        if (row != null) {
            configs.add(config(source, row.getDescription(), row.getDefaultValue(), row.getValue()));
        }
    }

    private GlobalConfig config(GlobalConfig source, String description, String value) {
        return config(source, description, value, value);
    }

    private GlobalConfig config(GlobalConfig source, String description, String defaultValue, String value) {
        org.zstack.core.config.schema.GlobalConfig.Config definition =
                new org.zstack.core.config.schema.GlobalConfig.Config();
        definition.setCategory(source.getCategory()); definition.setName(source.getName());
        definition.setDescription(description); definition.setDefaultValue(defaultValue); definition.setValue(value);
        definition.setType(String.class.getName());
        return GlobalConfig.valueOf(definition);
    }

    @Override public void afterHostConnected(HostInventory host) {
        if (host == null || host.getUuid() == null || host.getUuid().trim().isEmpty()
                || !"Connected".equals(host.getStatus())
                || !KVMConstant.KVM_HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            return;
        }
        final String hostUuid = host.getUuid();
        observationBackoff.remove(hostUuid);
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
            if (message instanceof APIUpdateMemoryPolicyMsg) { update((APIUpdateMemoryPolicyMsg) message); }
            else if (message instanceof APIGetMemoryPolicyMsg) { policy((APIGetMemoryPolicyMsg) message); }
            else if (message instanceof APIGetMemoryStatesMsg) { states((APIGetMemoryStatesMsg) message); }
            else if (message instanceof APIGetMemoryTasksMsg) { tasks((APIGetMemoryTasksMsg) message); }
            else if (message instanceof APIGetVmMemoryOptimizationsMsg) { vmAccountings((APIGetVmMemoryOptimizationsMsg) message); }
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

        if (api instanceof APIGetVmMemoryOptimizationsMsg) {
            List<String> accessible;
            try {
                // Reuse owner/public/private sharing semantics without running
                // legacy UserVO policy evaluation on an IAM2 VirtualID session.
                accessible = accountManager.getResourceUuidsCanAccessByAccount(
                        api.getSession().getAccountUuid(), VmInstanceVO.class);
            } catch (RuntimeException e) {
                throw new MemoryOperationException("MEMORY_PERMISSION_DENIED", "The account cannot access the requested VM memory statistics");
            }
            List<String> requested = ((APIGetVmMemoryOptimizationsMsg) api).getVmUuids();
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

    @Override
    public void marshalZQLASTTree(ASTNode.Query query) {
        Deque<ASTNode> pending = new ArrayDeque<>();
        Set<ASTNode> visited = Collections.newSetFromMap(new IdentityHashMap<ASTNode, Boolean>());
        pending.add(query);
        while (!pending.isEmpty()) {
            ASTNode node = pending.removeFirst();
            if (!visited.add(node)) { continue; }
            String entity = node instanceof ASTNode.QueryTarget ? ((ASTNode.QueryTarget) node).getEntity()
                    : node instanceof ASTNode.SubQueryTarget ? ((ASTNode.SubQueryTarget) node).getEntity()
                    : node instanceof ASTNode.RestrictExpr ? ((ASTNode.RestrictExpr) node).getEntity() : null;
            if ("MemoryState".equalsIgnoreCase(entity)) {
                APIGetMemoryStatesMsg request = new APIGetMemoryStatesMsg();
                request.setSession(ZQLContext.getAPISession());
                try {
                    checkReadAuthorization(request);
                } catch (MemoryOperationException denied) {
                    throw new OperationFailureException(denied.toErrorCode());
                }
                return;
            }
            // Conditions/values are interface-typed and joins are lists: getChildren()
            // alone misses nested targets. Inspect parsed AST nodes, never query text.
            for (Field field : FieldUtils.getAllFields(node.getClass())) {
                if (Modifier.isStatic(field.getModifiers())) { continue; }
                Object value = FieldUtils.getFieldValue(field.getName(), node);
                if (value instanceof ASTNode) {
                    pending.add((ASTNode) value);
                } else if (value instanceof Collection) {
                    for (Object child : (Collection<?>) value) {
                        if (child instanceof ASTNode) { pending.add((ASTNode) child); }
                    }
                }
            }
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
        if (MemoryBackendPreparationRules.ACTION.equals(msg.getAction())) {
            // Device selection is authorized by the existing preparation DTO;
            // the physical identity comes only from this fresh Agent probe.
            repository.targets("Host", msg.getResourceUuid());
            call(msg.getResourceUuid(), "writeback-backends", command(msg.getResourceUuid(), Platform.getUuid()), response -> {
                try {
                    MemoryAgentResponse observed = response;
                    if (observed == null) { observed = new MemoryAgentResponse(); observed.setSuccess(false); }
                    event.setInventory(repository.submit(msg, observed));
                    bus.publish(event);
                } catch (MemoryOperationException invalid) {
                    bus.replyErrorByMessageType(msg, invalid.toErrorCode());
                } catch (IllegalArgumentException invalid) {
                    bus.replyErrorByMessageType(msg, new MemoryOperationException(
                            "MEMORY_INVALID_REQUEST", invalid.getMessage()).toErrorCode());
                }
            });
            return;
        }
        event.setInventory(repository.submit(msg));
        bus.publish(event);
    }

    private void policy(APIGetMemoryPolicyMsg msg) {
        APIGetMemoryPolicyReply reply = new APIGetMemoryPolicyReply();
        MemoryPolicyInventory inventory = repository.getPolicy(msg.getScope(), msg.getResourceUuid());
        describe(inventory);
        reply.setInventory(inventory);
        bus.reply(msg, reply);
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
            addPolicyCapabilities(fields, licensed, inventory.getScope());
        }
        if ("Host".equals(inventory.getScope())) {
            List<MemoryStateVO> states = repository.states(Collections.singletonList(inventory.getResourceUuid()), 0, 1);
            MemoryCapabilityRules.restrict(fields, states.isEmpty() ? null : states.get(0), System.currentTimeMillis());
        }
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
    private static void addPolicyCapabilities(Map<String, MemoryFieldCapability> fields, boolean writable,
                                              String scope) {
        fields.put("schemaVersion", capability(writable, false, "int32", "version", "PersistedSchema"));
        fields.put("zram.enabled", capability(writable, true, "boolean", null, "EnableOrDrain"));
        // VM selection is a CLI/advanced policy control; the basic UI only
        // exposes enablement, capacity, inheritance and status per FS 14.2.
        fields.put("zram.selectionMode", capability(writable, false, "enum", null, "NextPolicyCycle"));
        fields.put("zram.selectedVmUuids", capability(writable, false, "uuid[]", null, "InstanceFenced"));
        fields.put("zram.logicalCapacityBytes", capability(writable, true, "int64", "bytes", "CapacityChecked"));
        fields.put("zram.ramLimitBytes", capability(writable, true, "int64", "bytes", "CapacityChecked"));
        fields.put("writeback.enabled", capability(writable, false, "boolean", null, "OfflinePreparationMayBeRequired"));
        boolean hostBackend = "Host".equals(scope);
        fields.put("writeback.backendResourceUuid", capability(writable && hostBackend, false, "uuid", null, "DrainAndOfflinePreparation"));
        fields.put("writeback.backendCapacityBytes", capability(writable && hostBackend, false, "int64", "bytes", "DrainAndOfflinePreparation"));
    }

    private static MemoryFieldCapability capability(boolean writable, boolean basicUi, String type, String unit, String effect) {
        return new MemoryFieldCapability(writable, basicUi, type, unit, effect);
    }

    private void states(APIGetMemoryStatesMsg msg) {
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

    private void tasks(APIGetMemoryTasksMsg msg) {
        APIQueryMemoryTaskReply reply = new APIQueryMemoryTaskReply();
        MemoryQueryPage<MemoryTaskInventory> page = repository.taskPage(msg);
        reply.setInventories(page.items); reply.setTotal(page.total); reply.setSnapshotId(page.snapshotId);
        reply.setNextPage(nextPage(msg.getStart(), reply.getInventories().size(), reply.getTotal())); bus.reply(msg, reply);
    }

    static Integer nextPage(int start, int size, long total) {
        long next = (long) start + size;
        return size > 0 && next < total && next <= Integer.MAX_VALUE ? (int) next : null;
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
        boolean ksmReconnectCandidate = active && !poll && "Host".equals(task.getScope())
                && "apply".equals(task.getAction())
                && "system:memory-host-connect-policy".equals(task.getActorUuid())
                && task.getExpectedControlOperationUuid() != null;
        boolean confirmedKsmReconnect = ksmReconnectCandidate
                && repository.isConfirmedKsmReconnectTask(task.getUuid());
        String reconnectObservationBootId = confirmedKsmReconnect ? observedBootId(task.getHostUuid()) : null;
        confirmedKsmReconnect = confirmedKsmReconnect && reconnectObservationBootId != null;
        if (ksmReconnectCandidate && !confirmedKsmReconnect) {
            // Never let a stale/internal reconnect task fall through to an
            // ordinary apply. The Agent's special journal guard is attached
            // only to a fully proven request and cannot be synthesized later.
            repository.result(task.getUuid(), "Unknown", "KSM_RECONNECT_PROOF_UNQUALIFIED", null);
            return;
        }
        if (configuration && !poll && licenseDeadline() <= System.currentTimeMillis() && !confirmedKsmReconnect) {
            if (MemoryUncertainRecoveryRules.ACTION.equals(task.getAction())) {
                repository.recoveryLicenseExpiredBeforeDispatch(task.getUuid()); return;
            }
            repository.result(task.getUuid(), "Failed", "Cloud License expired before dispatch", null); return;
        }
        if (active && !poll) {
            String rejection = coldKsmDispatchRejection(task, System.currentTimeMillis());
            if (rejection != null) {
                repository.result(task.getUuid(), "Failed", rejection, null);
                return;
            }
        }
        Map<String, Object> command = command(task.getHostUuid(), task.getUuid());
        command.put("scope", task.getScope()); command.put("resourceUuid", task.getResourceUuid());
        command.put("action", "clearOverride".equals(task.getAction()) ? "apply" : task.getAction());
        if (active) {
            String policyPlanHash = task.getPolicyPlanHash();
            command.put("policyMode", !"clearOverride".equals(task.getAction())
                    && policyPlanHash != null && !policyPlanHash.trim().isEmpty()
                    ? "delta" : "effective");
        }
        command.put("desiredRevision", task.getDesiredRevision());
        // The Agent's VM-list shard protocol needs one stable generation for
        // every chunk. The policy revision is the MN snapshot generation for
        // a configuration task; it is never refreshed between chunks.
        command.put("targetSnapshotGeneration", "policy-" + task.getDesiredRevision());
        command.put("expectedInstanceGeneration", task.getExpectedInstanceGeneration());
        command.put("expectedControlOperationUuid", task.getExpectedControlOperationUuid());
        if (confirmedKsmReconnect) {
            command.put("confirmedKsmReconnect", true);
            command.put("reconnectObservationBootId", reconnectObservationBootId);
        }
        command.put("policy", active ? MemoryPolicyRules.decode(task.getPolicy()) : Collections.emptyMap());
        if (active && Boolean.TRUE.equals(task.getRequiresKsmNativePartialFence())) {
            command.put("requiresKsmNativePartialFence", true);
            command.put("ksmNativeObservationBootId", task.getKsmNativeObservationBootId());
            command.put("ksmNativeObservationSampleTime", task.getKsmNativeObservationSampleTime());
        }
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
        MemoryTaskVO original = null;
        if ("reconcile".equals(task.getAction()) && task.getReconcileOperationUuid() != null) {
            operation = task.getReconcileOperationUuid(); command.put("operationUuid", operation);
            original = repository.findTask(operation);
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
            if (original != null && MemoryUncertainRecoveryRules.ACTION.equals(original.getAction())) {
                // A recovery query must describe the original recovery, not
                // borrow today's effective policy or the query child's identity.
                command.remove("reconcileAction");
                command.remove("classification");
                command.remove("admissionConnected");
                command.put("expectedControlOperationUuid", original.getExpectedControlOperationUuid());
                command.put("desiredRevision", original.getDesiredRevision());
                command.put("recovery", JSONObjectUtil.toObject(original.getPolicy(), Map.class));
                // The standard admission stores the immutable request hash on
                // the parent, not each dispatched Host child.
                MemoryTaskVO requestOwner = original.getParentUuid() == null ? original
                        : repository.findTask(original.getParentUuid());
                String recoveryRequestHash = requestOwner != null
                        && "Host".equals(requestOwner.getScope())
                        && MemoryUncertainRecoveryRules.ACTION.equals(requestOwner.getAction())
                        && Objects.equals(original.getResourceUuid(), requestOwner.getResourceUuid())
                        && Objects.equals(original.getExpectedControlOperationUuid(), requestOwner.getExpectedControlOperationUuid())
                        ? requestOwner.getRequestHash() : null;
                command.put("recoveryRequestHash", recoveryRequestHash);
                // This precise legacy result was returned before the Agent
                // claimed or issued anything. Absence of a journal, a generic
                // timeout, or a substring of an error is not that evidence.
                if (recoveryRequestHash != null && recoveryRequestHash.matches("[0-9a-f]{64}")
                        && "Host".equals(original.getScope()) && "Unknown".equals(original.getStatus())
                        && Objects.equals(task.getHostUuid(), original.getHostUuid())
                        && "Uncertain recovery is not proven: RECOVERY_REQUEST_INVALID".equals(original.getReason())) {
                    command.put("reconcileAction", "seal-rejected-recovery");
                    command.put("classification", "RECOVERY_REQUEST_INVALID");
                }
            }
        }
        if (MemoryRepository.isConfigurationReconcile(task)) {
            if (original == null || !Objects.equals(task.getHostUuid(), original.getHostUuid())
                    || original.getDesiredRevision() != task.getDesiredRevision()
                    || !Arrays.asList("Host", "Cluster", "Global").contains(original.getScope())
                    || original.getResourceUuid() == null) {
                repository.result(task.getUuid(), "Unknown", "READ_ONLY_COMPLETION_ORIGINAL_IDENTITY_INVALID", null);
                return;
            }
            // Automatic completion is a read of the original completed
            // receipt, never a request to continue an unfinished executor.
            // The query task is Host-scoped, but the original standard
            // Global/Cluster configuration retains its own scope on the wire.
            command.put("scope", original.getScope());
            command.put("resourceUuid", original.getResourceUuid());
            command.put("reconcileAction", "verify-completed");
            command.remove("classification"); command.remove("admissionConnected");
            command.put("bootId", observedBootId(task.getHostUuid()));
        }
        final String queriedOperation = operation;
        MemoryTaskVO maintenanceTask = original == null ? task : original;
        boolean maintenanceActivation = Arrays.asList("apply", "clearOverride").contains(maintenanceTask.getAction());
        MemoryTaskVO maintenanceDrain = (maintenanceActivation || Arrays.asList(MemoryZramPoolPreparationRules.ACTION,
                MemoryBackendPreparationRules.ACTION).contains(maintenanceTask.getAction()))
                && maintenanceTask.getExpectedControlOperationUuid() != null
                ? repository.findTask(maintenanceTask.getExpectedControlOperationUuid()) : maintenanceTask;
        MemoryTaskVO maintenanceRoot = maintenanceDrain == null || maintenanceDrain.getParentUuid() == null
                ? null : repository.findTask(maintenanceDrain.getParentUuid());
        MemoryTaskVO selectedRoot = maintenanceTask.getParentUuid() == null ? null
                : repository.findTask(maintenanceTask.getParentUuid());
        final boolean automaticMaintenance = MemoryRepository.isSelectedBackendMaintenance(maintenanceTask, selectedRoot)
                || MemoryRepository.isAutomaticCapacityDrain(maintenanceDrain, maintenanceRoot)
                && Objects.equals(maintenanceTask.getHostUuid(), maintenanceDrain.getHostUuid())
                && Objects.equals(maintenanceTask.getParentUuid(), maintenanceDrain.getParentUuid())
                && (maintenanceActivation
                    || Objects.equals(maintenanceTask.getPolicyPlanHash(), maintenanceDrain.getPolicyPlanHash()));
        final String maintenanceObservation = "drain".equals(maintenanceTask.getAction())
                ? "writeback-backends" : "state";
        final boolean ordinaryReconcile = original != null && !"VM".equals(task.getScope())
                && !"VM".equals(original.getScope())
                && Arrays.asList("apply", "clearOverride", "pause", "resume", "drain").contains(original.getAction())
                && !rejectedResumeRecoveryTask(original);
        final String queryBootId = ordinaryReconcile ? observedBootId(task.getHostUuid()) : null;
        String endpoint = poll || "reconcile".equals(task.getAction()) ? "reconcile" : "apply";
        try {
            Consumer<MemoryAgentResponse> completion = response -> {
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
                if (automaticMaintenance && "Succeeded".equals(status)) {
                    // Keep the immutable executor receipt separate from the
                    // fresh proof needed to admit the next maintenance step.
                    // Drain has stopped the service: its local inventory is
                    // available even when a normal live state read is not.
                    try {
                        call(task.getHostUuid(), maintenanceObservation,
                                command(task.getHostUuid(), Platform.getUuid()), observation ->
                                        repository.result(task.getUuid(), status, response.reason, response, observation));
                    } catch (RuntimeException observationFailure) {
                        repository.result(task.getUuid(), "Unknown",
                                "Maintenance result requires a fresh Host observation", response);
                    }
                    return;
                }
                if (ordinaryReconcile && "Succeeded".equals(status)) {
                    // Agent reconciliation can return an immutable historical
                    // journal receipt. Collect current proof using the existing
                    // read-only endpoint, without overwriting either response or
                    // dispatching configuration from this callback.
                    try {
                        call(task.getHostUuid(), "state", command(task.getHostUuid(), Platform.getUuid()), observation -> {
                            if (queryBootId == null || !successfulFreshObservation(task.getHostUuid(), observation)
                                    || !queryBootId.equals(observation.bootId)) {
                                repository.result(task.getUuid(), "Unknown",
                                        "Original result requires fresh same-boot Host observation", null);
                                return;
                            }
                            repository.result(task.getUuid(), status, response.reason, response, observation);
                        });
                    } catch (RuntimeException observationFailure) {
                        repository.result(task.getUuid(), "Unknown",
                                "Original result could not be confirmed by current Host observation", null);
                    }
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
            };
            if (MemoryRepository.isConfigurationReconcile(task)) {
                // Older Agents ignore unknown reconcileAction values and may
                // resume an executor. Probe only the established read-only
                // state endpoint before sending this new read-only action.
                call(task.getHostUuid(), "state", command(task.getHostUuid(), Platform.getUuid()), support -> {
                    if (!successfulFreshObservation(task.getHostUuid(), support)
                            || !Objects.equals(task.getHostUuid(), support.hostUuid)
                            || command.get("bootId") == null
                            || !Objects.equals(command.get("bootId"), support.bootId)) {
                        repository.result(task.getUuid(), "Unknown", "READ_ONLY_COMPLETION_OBSERVATION_UNQUALIFIED", null);
                        return;
                    }
                    if (support.capabilities == null
                            || !Boolean.TRUE.equals(support.capabilities.get("readOnlyCompletedReconcile"))) {
                        repository.result(task.getUuid(), "Unknown", "READ_ONLY_COMPLETION_UNSUPPORTED", null);
                        return;
                    }
                    try { call(task.getHostUuid(), endpoint, command, completion); }
                    catch (RuntimeException dispatchFailure) {
                        repository.result(task.getUuid(), "Unknown",
                                "Read-only original result could not be confirmed", null);
                    }
                });
            } else {
                call(task.getHostUuid(), endpoint, command, completion);
            }
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
    static String coldKsmDispatchRejection(MemoryTaskVO task, long now) {
        if (Boolean.TRUE.equals(task.getRequiresKsmNativePartialFence())) {
            Long sample = task.getKsmNativeObservationSampleTime();
            String boot = task.getKsmNativeObservationBootId();
            if (!Arrays.asList("Host", "Cluster", "Global").contains(task.getScope())
                    || boot == null || boot.trim().isEmpty() || sample == null
                    || sample < 0 || sample > now
                    || now - sample >= MemoryOptimizationGlobalConfig.displayTtlMillis()) {
                return "Cold KSM admission evidence expired or is missing; refresh Host state and reapply configuration";
            }
        } else if (task.getRequiresKsmNativePartialFence() == null
                && !"VM".equals(task.getScope())) {
            MemoryPolicyConfig policy = MemoryPolicyRules.decode(task.getPolicy());
            if (policy != null && policy.ksm != null && policy.zram == null && policy.writeback == null) {
                // Pre-upgrade queued tasks cannot prove which admission path
                // accepted them. Reject before sending, without inventing proof.
                return "Queued KSM task has no persisted admission evidence; refresh Host state and reapply configuration";
            }
        }
        return null;
    }

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
        long now = System.currentTimeMillis();
        List<String> hosts = repository.connectedHosts().stream()
                .filter(host -> observationBackoff.getOrDefault(host, 0L) <= now).collect(Collectors.toList());
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
                    // Use the existing observation and persisted control intent before
                    // reconciliation can queue another operation. No health poller or
                    // service restart is introduced here.
                    try {
                        for (HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData event :
                                MemoryObservationRules.serviceEvents(host, response,
                                        repository.serviceObservationPolicy(host), System.currentTimeMillis())) {
                            evtf.fire(HostCanonicalEvents.HOST_MEMORY_OPTIMIZATION_SERVICE_STATE_CHANGED_PATH, event);
                        }
                    } catch (RuntimeException eventFailure) {
                        logger.warn(String.format("Host[%s] memory service event failed; observation will continue", host), eventFailure);
                    }
                    // An unreachable or older Agent is retried slowly; a reconnect clears the backoff.
                    // A native KSM partial result is the normal reply before ZRAM is first enabled.
                    boolean ksmPartial = MemoryObservationRules.isKsmNativePartial(response, host, System.currentTimeMillis());
                    if (!ksmPartial && (response == null || !response.isSuccess())) {
                        observationBackoff.put(host, System.currentTimeMillis() + OBSERVATION_FAILURE_BACKOFF_MS);
                    } else {
                        observationBackoff.remove(host);
                    }
                    if (ksmPartial) {
                        repository.observePartialKsm(host, response);
                    } else if (successfulFreshObservation(host, response)) {
                        repository.observe(host, response);
                        applyInheritedPolicyAfterObservation(host);
                        reconcileVerifiedPoolBlock(host, response);
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

    private static boolean successfulFreshObservation(String host, MemoryAgentResponse response) {
        if (response == null || !response.isSuccess() || !"Succeeded".equals(response.status)
                || response.state == null || response.sampleTime == null) { return false; }
        // The Agent's outer identity and the RuntimeSnapshot boot are both
        // optional for older Agents, but when present they must agree with
        // the target and with each other. A new boot is valid current data;
        // repository sample ordering below still rejects delayed old samples.
        if (host == null || host.trim().isEmpty()
                || response.hostUuid != null && !host.equals(response.hostUuid)) { return false; }
        Object stateBoot = response.state.get("bootId");
        if (stateBoot != null && (!(stateBoot instanceof String) || ((String) stateBoot).trim().isEmpty())) {
            return false;
        }
        if (response.bootId != null && (response.bootId.trim().isEmpty()
                || stateBoot instanceof String && !response.bootId.equals(stateBoot))) { return false; }
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

    private void reconcileVerifiedPoolBlock(String host, MemoryAgentResponse response) {
        try { repository.reconcileVerifiedPoolBlock(host, response); }
        catch (MemoryOperationException e) {
            logger.debug(String.format("Host[%s] verified ZRAM pool reconcile was not queued: %s", host, e.getCode()));
        } catch (RuntimeException e) {
            logger.warn(String.format("Host[%s] verified ZRAM pool reconcile was not accepted", host), e);
        }
    }

    private Map<String, Object> command(String host, String operation) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("hostUuid", host); command.put("operationUuid", operation); return command;
    }

    private final MemoryBatchCursor observationCursor = new MemoryBatchCursor();
    private static final long OBSERVATION_FAILURE_BACKOFF_MS = TimeUnit.MINUTES.toMillis(10);
    private final Map<String, Long> observationBackoff = new ConcurrentHashMap<>();

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
                || "accounting".equals(action) || "reconcile".equals(action);
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
