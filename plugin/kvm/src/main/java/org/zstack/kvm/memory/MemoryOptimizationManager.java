package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityEvent;
import org.zstack.core.db.EntityLifeCycleCallback;
import org.zstack.core.db.Q;
import org.zstack.core.progress.ProgressReportService;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.longjob.SubmitLongJobReply;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.SessionInventory;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
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
    @Autowired private DatabaseFacade dbf;
    private final Set<String> dirtyHostStates = ConcurrentHashMap.newKeySet();
    private volatile long lastHostStateRefresh;
    @Autowired private PluginRegistry pluginRgty;
    @Autowired private MemoryRepository repository;
    @Autowired private AccountManager accountManager;
    @Autowired private MemoryLifecycleHooks lifecycleHooks;
    @Autowired private ResourceDestinationMaker destMaker;
    private final AtomicBoolean ticking = new AtomicBoolean();
    /** Coalesce event and periodic observations on this MN; DB admission remains the cross-MN fence. */
    private final Set<String> observingHosts = ConcurrentHashMap.newKeySet();
    private Future<?> poller;
    private long lastObservation;
    private long lastHistoryPurge;

    static final String SERVICE_ID = "memoryOptimization";

    @Override public String getId() { return bus.makeLocalServiceId(SERVICE_ID); }
    @Override public boolean start() { return true; }
    @Override public boolean stop() { if (poller != null) { poller.cancel(false); } return true; }
    @Override public void nodeJoin(ManagementNodeInventory node) { }
    @Override public void nodeLeft(ManagementNodeInventory node) { expireUnconfirmed(); }
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
        observationFailures.remove(hostUuid);
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
        EntityLifeCycleCallback dirty = (event, entity) -> dirtyHostStates.add(((MemoryStateVO) entity).getHostUuid());
        dbf.installEntityLifeCycleCallback(MemoryStateVO.class, EntityEvent.POST_PERSIST, dirty);
        dbf.installEntityLifeCycleCallback(MemoryStateVO.class, EntityEvent.POST_UPDATE, dirty);
        repository.initialize();
        expireUnconfirmed();
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
            if (message instanceof APIGetVmMemoryOptimizationsMsg) { vmAccountings((APIGetVmMemoryOptimizationsMsg) message); }
            else if (message instanceof APIGetHostMemoryWritebackBackendsMsg) { writebackBackends((APIGetHostMemoryWritebackBackendsMsg) message); }
            else if (message instanceof APIGetMemorySummaryMsg) { summary((APIGetMemorySummaryMsg) message); }
            else if (message instanceof MemoryClusterOperationMsg) { clusterOperation((MemoryClusterOperationMsg) message); }
            else if (message instanceof MemoryHostOperationMsg) { hostOperation((MemoryHostOperationMsg) message); }
            else if (message instanceof APIPrepareHostMemoryWritebackBackendMsg) {
                APIPrepareHostMemoryWritebackBackendEvent event = new APIPrepareHostMemoryWritebackBackendEvent(message.getId());
                submitLongJob((APICreateMessage) message, event, event::setInventory);
            } else if (message instanceof APIRecoverHostMemoryOptimizationMsg) {
                APIRecoverHostMemoryOptimizationEvent event = new APIRecoverHostMemoryOptimizationEvent(message.getId());
                submitLongJob((APICreateMessage) message, event, event::setInventory);
            }
            else if (message instanceof APIPauseMemoryOptimizationMsg) {
                APIPauseMemoryOptimizationEvent event = new APIPauseMemoryOptimizationEvent(message.getId());
                submitLongJob((APICreateMessage) message, event, event::setInventory);
            } else if (message instanceof APIResumeMemoryOptimizationMsg) {
                APIResumeMemoryOptimizationEvent event = new APIResumeMemoryOptimizationEvent(message.getId());
                submitLongJob((APICreateMessage) message, event, event::setInventory);
            } else if (message instanceof APIDrainMemoryOptimizationMsg) {
                APIDrainMemoryOptimizationEvent event = new APIDrainMemoryOptimizationEvent(message.getId());
                submitLongJob((APICreateMessage) message, event, event::setInventory);
            }
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
            if ("HostMemoryState".equalsIgnoreCase(entity)) {
                APIQueryHostMemoryStateMsg request = new APIQueryHostMemoryStateMsg();
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

    /** Queues the request and returns the task whose status is the operation outcome. */
    private void submitHostAction(MemoryActionRequest msg, Consumer<String> done,
                                  Consumer<MemoryOperationException> failed) {
        if (MemoryBackendPreparationRules.ACTION.equals(msg.getAction())) {
            // Device selection is authorized by the existing preparation DTO;
            // the physical identity comes only from this fresh Agent probe.
            repository.targets("Host", msg.getResourceUuid());
            call(msg.getResourceUuid(), "writeback-backends", command(msg.getResourceUuid(), Platform.getUuid()), response -> {
                String submitted;
                try {
                    MemoryAgentResponse observed = response;
                    if (observed == null) { observed = new MemoryAgentResponse(); observed.setSuccess(false); }
                    submitted = repository.submit(msg, observed);
                } catch (MemoryOperationException invalid) {
                    failed.accept(invalid);
                    return;
                } catch (IllegalArgumentException invalid) {
                    failed.accept(new MemoryOperationException("MEMORY_INVALID_REQUEST", invalid.getMessage()));
                    return;
                } catch (RuntimeException unknown) {
                    // This Agent callback is outside the caller's try; never strand the Host queue.
                    failed.accept(new MemoryOperationException("MEMORY_RESULT_UNKNOWN", unknown.getMessage()));
                    return;
                }
                done.accept(submitted);
            });
            return;
        }
        if ("resume".equals(msg.getAction()) && repository.isDrainedHost(msg.getResourceUuid())) {
            String actor = msg.getSession().getAccountUuid() + ":" + msg.getSession().getUserUuid();
            recoverDrain(msg.getResourceUuid(), actor, done, failed);
            return;
        }
        done.accept(repository.submit(msg));
    }

    /** The API path submits the same LongJob that SubmitLongJob creates from this request. */
    private void submitLongJob(APICreateMessage msg, APIEvent event,
                                        Consumer<LongJobInventory> inventory) {
        SubmitLongJobMsg submit = new SubmitLongJobMsg();
        submit.setJobName(msg.getClass().getSimpleName());
        submit.setJobData(JSONObjectUtil.toJsonString(msg));
        submit.setResourceUuid(msg.getResourceUuid());
        submit.setSystemTags(msg.getSystemTags());
        submit.setUserTags(msg.getUserTags());
        submit.setAccountUuid(msg.getSession().getAccountUuid());
        bus.makeLocalServiceId(submit, LongJobConstants.SERVICE_ID);
        bus.send(submit, new CloudBusCallBack(msg) {
            @Override public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    bus.replyErrorByMessageType(msg, reply.getError());
                    return;
                }
                inventory.accept(((SubmitLongJobReply) reply).getInventory());
                bus.publish(event);
            }
        });
    }

    /** Hosts run one at a time, each on the management node that owns it. */
    private void clusterOperation(MemoryClusterOperationMsg msg) {
        requireLicense();
        List<String> members = repository.targets("Cluster", msg.getClusterUuid());
        if (msg.getHostUuid() != null && !members.contains(msg.getHostUuid())) {
            throw new MemoryOperationException("MEMORY_INVALID_REQUEST",
                    "Host[" + msg.getHostUuid() + "] is not a KVM Host of Cluster[" + msg.getClusterUuid() + "]");
        }
        MemoryClusterOperationReply reply = new MemoryClusterOperationReply();
        List<String> hosts = msg.getHostUuid() != null ? Collections.singletonList(msg.getHostUuid()) : members;
        Map<String, String> results = new LinkedHashMap<>();
        reply.setResults(results);
        new While<>(hosts).each((host, next) -> {
            MemoryHostOperationMsg operation = new MemoryHostOperationMsg();
            operation.setHostUuid(host); operation.setAction(msg.getAction()); operation.setJobUuid(msg.getJobUuid());
            operation.setAccountUuid(msg.getAccountUuid()); operation.setUserUuid(msg.getUserUuid());
            // A Host step lasts as long as the job may, including its Draining polls.
            operation.setTimeout(msg.getTimeout());
            bus.makeTargetServiceIdByResourceUuid(operation, SERVICE_ID, host);
            bus.send(operation, new CloudBusCallBack(next) {
                @Override public void run(MessageReply hostReply) {
                    results.put(host, hostOutcome(host, hostReply));
                    reportClusterProgress(results.size(), hosts.size());
                    next.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override public void done(ErrorCodeList errors) { bus.reply(msg, reply); }
        });
    }

    static String hostOutcome(String host, MessageReply reply) {
        if (reply instanceof MemoryClusterOperationReply && ((MemoryClusterOperationReply) reply).getResults() != null
                && ((MemoryClusterOperationReply) reply).getResults().containsKey(host)) {
            return ((MemoryClusterOperationReply) reply).getResults().get(host);
        }
        if (reply.isSuccess()) { return "success"; }
        return reply.getError() == null ? "Unknown: no reply" : reply.getError().getCode() + ": " + reply.getError().getDetails();
    }

    void reportClusterProgress(int done, int total) {
        ProgressReportService.reportProgress(String.valueOf(100 * done / total));
    }

    /** One Host's operation, run on its owner's queue; the reply carries the Host outcome. */
    private void hostOperation(MemoryHostOperationMsg msg) {
        requireLicense();
        String host = msg.getHostUuid();
        repository.targets("Host", host);
        thdf.chainSubmit(new ChainTask(msg) {
            @Override public String getSyncSignature() { return hostQueue(host); }
            @Override public String getName() { return getSyncSignature(); }
            @Override public void run(SyncTaskChain chain) {
                AtomicBoolean finished = new AtomicBoolean();
                Consumer<String> finish = result -> {
                    if (!finished.compareAndSet(false, true)) { return; }
                    try {
                        if (msg.getJobUuid() != null) { markActiveJob(host, null, msg.getJobUuid()); }
                        replyHostOperation(msg, result);
                    } finally { chain.next(); }
                };
                if (msg.getJobUuid() != null) { markActiveJob(host, msg.getJobUuid(), null); }
                Consumer<MemoryOperationException> failed = error -> finish.accept(error.getCode() + ": " + error.getMessage());
                try {
                    submitHostAction(hostActionRequest(msg), task -> drive(host, () -> finish.accept(outcome(task))), failed);
                } catch (MemoryOperationException error) {
                    failed.accept(error);
                } catch (IllegalArgumentException error) {
                    failed.accept(new MemoryOperationException("MEMORY_INVALID_REQUEST", error.getMessage()));
                } catch (RuntimeException error) {
                    // The request may have been recorded; never leave the Host queue or the job waiting.
                    finish.accept("Unknown: " + error.getMessage());
                }
            }
        });
    }

    void replyHostOperation(MemoryHostOperationMsg msg, String outcome) {
        MemoryClusterOperationReply reply = new MemoryClusterOperationReply();
        reply.setResults(Collections.singletonMap(msg.getHostUuid(), outcome));
        if (!"success".equals(outcome)) {
            reply.setError(new MemoryOperationException("MEMORY_HOST_OPERATION_FAILED",
                    "Host[" + msg.getHostUuid() + "] " + msg.getAction() + ": " + outcome).toErrorCode());
        }
        bus.reply(msg, reply);
    }

    private MemoryActionRequest hostActionRequest(MemoryHostOperationMsg msg) {
        MemoryActionRequest request = new MemoryActionRequest();
        request.setScope("Host"); request.setResourceUuid(msg.getHostUuid()); request.setAction(msg.getAction());
        SessionInventory session = new SessionInventory();
        session.setAccountUuid(msg.getAccountUuid()); session.setUserUuid(msg.getUserUuid());
        request.setSession(session);
        request.setBackendPreparation(msg.getPreparation());
        // The uncertain control is the caller's confirmation, not the Host's current fence.
        request.setExpectedControlOperationUuid(msg.getExpectedControlOperationUuid());
        request.setRecovery(msg.getRecovery());
        return request;
    }

    private void markActiveJob(String host, String job, String previous) {
        try {
            repository.setActiveJob(host, job, previous);
        } catch (RuntimeException e) {
            logger.warn(String.format("unable to record the memory LongJob of Host[%s]; it is display-only", host), e);
        }
    }

    private String outcome(String taskUuid) {
        MemoryTaskVO task;
        try {
            task = repository.findTask(taskUuid);
        } catch (RuntimeException error) {
            return "Unknown: task status unavailable: " + error.getMessage();
        }
        return task == null ? "Unknown: task not found" : "Succeeded".equals(task.getStatus()) ? "success"
                : task.getStatus() + (task.getReason() == null ? "" : ": " + task.getReason());
    }

    static String hostQueue(String host) { return "memory-host-operation-" + host; }

    /** Hosts with a scan-triggered drive waiting on their queue. */
    private final Set<String> queuedDrives = ConcurrentHashMap.newKeySet();
    /** Hosts whose Agent requests this management node is sending. */
    private final Set<String> activeDrives = ConcurrentHashMap.newKeySet();

    /** Runs the queued work of a Host this management node owns. */
    private void scheduleDrive(String host) {
        if (!queuedDrives.add(host)) { return; }
        thdf.chainSubmit(new ChainTask(null) {
            @Override public String getSyncSignature() { return hostQueue(host); }
            @Override public String getName() { return getSyncSignature(); }
            @Override public void run(SyncTaskChain chain) {
                queuedDrives.remove(host);
                drive(host, chain::next);
            }
        });
    }

    /**
     * Sends the Host's active operation until none is queued: each result may
     * queue the next maintenance step. A Draining result is polled every 5s.
     * The caller holds the Host queue.
     */
    private void drive(String host, Runnable done) {
        activeDrives.add(host);
        AtomicBoolean finished = new AtomicBoolean();
        driveNext(host, () -> {
            if (!finished.compareAndSet(false, true)) { return; }
            activeDrives.remove(host);
            done.run();
        });
    }

    private void driveNext(String host, Runnable done) {
        MemoryTaskVO task;
        try {
            task = repository.claim(host);
        } catch (RuntimeException e) {
            logger.warn(String.format("Host[%s] memory operation could not be claimed", host), e);
            done.run();
            return;
        }
        if (task == null) { done.run(); return; }
        Runnable next = () -> driveNext(host, done);
        boolean poll = "Draining".equals(task.getStatus());
        Runnable send = () -> {
            try {
                dispatch(task, poll, next);
            } catch (RuntimeException e) {
                // Building the request failed before or while sending it.
                settle(next, () -> repository.result(task.getUuid(), "Unknown",
                        "Memory Agent dispatch outcome is unknown: " + e.getMessage(), null));
            }
        };
        if (poll) {
            thdf.submitTimeoutTask(send, TimeUnit.SECONDS, 5);
        } else {
            send.run();
        }
    }

    /** Records a result, then continues the Host queue even if recording failed. */
    private void settle(Runnable next, Runnable result) {
        try {
            result.run();
        } catch (RuntimeException e) {
            logger.warn("Unable to record a memory operation result; the Host keeps its in-flight state", e);
        } finally {
            next.run();
        }
    }

    /** Re-enabling ZRAM is the configuration form of resuming a drained Host. */
    public void recoverDrainedHosts(String resourceUuid) {
        for (String host : repository.drainedHostsWithZramEnabled(resourceUuid)) {
            recoverDrain(host, "system:memory-zram-reenabled", task -> { },
                    error -> logger.warn(String.format("drained Host[%s] was not recovered after ZRAM was re-enabled: %s",
                            host, error.getMessage())));
        }
    }

    private void recoverDrain(String host, String actor, Consumer<String> done,
                              Consumer<MemoryOperationException> failed) {
        call(host, "writeback-backends", command(host, Platform.getUuid()), response -> {
            String recovered;
            try {
                recovered = repository.recoverDrain(host, actor, response);
            } catch (MemoryOperationException invalid) {
                failed.accept(invalid);
                return;
            } catch (IllegalArgumentException invalid) {
                failed.accept(new MemoryOperationException("MEMORY_INVALID_REQUEST", invalid.getMessage()));
                return;
            } catch (RuntimeException unknown) {
                failed.accept(new MemoryOperationException("MEMORY_RESULT_UNKNOWN", unknown.getMessage()));
                return;
            }
            done.accept(recovered);
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

    private void summary(APIGetMemorySummaryMsg msg) {
        APIGetMemorySummaryReply reply = new APIGetMemorySummaryReply();
        List<String> hosts = repository.summaryTargets(msg.getHostUuids(), msg.getZoneUuid());
        long now = System.currentTimeMillis();
        Map<String, Map<String, Object>> samples = hostMetricSamples(hosts, now);
        List<MemoryStateVO> current = repository.allStates(hosts);
        List<MemoryStateVO> states = current.stream()
                .map(state -> MemoryMetricsPresentation.withSavings(state, samples.get(state.getHostUuid())))
                .collect(Collectors.toList());
        reply.setSummary(MemorySummaryRules.summarize(hosts, states, now));
        if (msg.getHostUuids() != null) {
            reply.getSummary().setHosts(MemorySummaryRules.hostDetails(hosts, current, samples, now));
        }
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

    /**
     * An in-flight request of a Host this node now owns, with no drive here, was
     * sent by a management node that restarted or left; its outcome is Unknown.
     */
    private void expireUnconfirmed() {
        try {
            repository.expireUnconfirmed(repository.hostsWithUnconfirmedWork().stream()
                    .filter(host -> destMaker.isManagedByUs(host) && !activeDrives.contains(host))
                    .collect(Collectors.toList()));
        } catch (RuntimeException e) {
            logger.warn("Unable to expire memory operations of a departed management node", e);
        }
    }

    private void tick() {
        if (!ticking.compareAndSet(false, true)) { return; }
        try {
            for (String host : repository.hostsWithPendingWork()) {
                if (destMaker.isManagedByUs(host)) { scheduleDrive(host); }
            }
            if (System.currentTimeMillis() - lastObservation >= 30000) {
                lastObservation = System.currentTimeMillis(); observeRuntime();
            }
            refreshHostStates();
            if (System.currentTimeMillis() - lastHistoryPurge >= TimeUnit.HOURS.toMillis(1)) {
                lastHistoryPurge = System.currentTimeMillis();
                repository.purgeHistory(lastHistoryPurge);
            }
        } catch (Exception e) {
            logger.warn("Memory optimization scan failed; no task is assumed successful", e);
        } finally { ticking.set(false); }
    }

    /**
     * The public Host state follows committed MemoryStateVO changes within a tick. Every 30 seconds all
     * Hosts are recomputed, so freshness changes and other management nodes' writes are reflected too.
     */
    private void refreshHostStates() {
        long now = System.currentTimeMillis();
        boolean full = now - lastHostStateRefresh >= 30000;
        List<String> dirty = new ArrayList<>(dirtyHostStates);
        dirtyHostStates.removeAll(dirty);
        try {
            List<String> hosts = full ? repository.hostsWithState() : dirty;
            if (!hosts.isEmpty()) { repository.refreshHostStates(hosts, now); }
            if (full) { lastHostStateRefresh = now; }
        } catch (RuntimeException e) {
            dirtyHostStates.addAll(dirty);
            throw e;
        }
    }

    private void dispatch(MemoryTaskVO task, boolean poll, Runnable next) {
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
            settle(next, () -> repository.result(task.getUuid(), "Unknown", "KSM_RECONNECT_PROOF_UNQUALIFIED", null));
            return;
        }
        if (configuration && !poll && licenseDeadline() <= System.currentTimeMillis() && !confirmedKsmReconnect) {
            if (MemoryUncertainRecoveryRules.ACTION.equals(task.getAction())) {
                settle(next, () -> repository.recoveryLicenseExpiredBeforeDispatch(task.getUuid())); return;
            }
            settle(next, () -> repository.result(task.getUuid(), "Failed", "Cloud License expired before dispatch", null)); return;
        }
        if (active && !poll) {
            String rejection = coldKsmDispatchRejection(task, System.currentTimeMillis());
            if (rejection != null) {
                settle(next, () -> repository.result(task.getUuid(), "Failed", rejection, null));
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
        command.put("expectedControlOperationUuid", task.getExpectedControlOperationUuid());
        if (confirmedKsmReconnect) {
            command.put("confirmedKsmReconnect", true);
            command.put("reconnectObservationBootId", reconnectObservationBootId);
        }
        command.put("policy", active ? MemoryPolicyRules.decode(task.getPolicy()) : Collections.emptyMap());
        // Command serialization drops null fields, so name the capacity sides left to the service default.
        List<String> defaultCapacitySides = active ? MemoryPolicyRules.defaultCapacitySides(task.getPolicy())
                : Collections.emptyList();
        if (!defaultCapacitySides.isEmpty()) { command.put("defaultCapacitySides", defaultCapacitySides); }
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
            }
        }
        if (MemoryRepository.isConfigurationReconcile(task)) {
            if (original == null || !Objects.equals(task.getHostUuid(), original.getHostUuid())
                    || original.getDesiredRevision() != task.getDesiredRevision()
                    || !Arrays.asList("Host", "Cluster", "Global").contains(original.getScope())
                    || original.getResourceUuid() == null) {
                settle(next, () -> repository.result(task.getUuid(), "Unknown", "READ_ONLY_COMPLETION_ORIGINAL_IDENTITY_INVALID", null));
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
                || MemoryRepository.isDrainRecoveryMaintenance(maintenanceTask, selectedRoot)
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
                try {
                    String status = MemoryResultRules.status(queriedOperation, active ? task.getDesiredRevision() : -1, response);
                    if (automaticMaintenance && "Succeeded".equals(status)) {
                        // Keep the immutable executor receipt separate from the
                        // fresh proof needed to admit the next maintenance step.
                        // Drain has stopped the service: its local inventory is
                        // available even when a normal live state read is not.
                        try {
                            call(task.getHostUuid(), maintenanceObservation,
                                    command(task.getHostUuid(), Platform.getUuid()), observation ->
                                            settle(next, () -> repository.result(task.getUuid(), status, response.reason, response, observation)));
                        } catch (RuntimeException observationFailure) {
                            settle(next, () -> repository.result(task.getUuid(), "Unknown",
                                    "Maintenance result requires a fresh Host observation", response));
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
                                    settle(next, () -> repository.result(task.getUuid(), "Unknown",
                                            "Original result requires fresh same-boot Host observation", null));
                                    return;
                                }
                                settle(next, () -> repository.result(task.getUuid(), status, response.reason, response, observation));
                            });
                        } catch (RuntimeException observationFailure) {
                            settle(next, () -> repository.result(task.getUuid(), "Unknown",
                                    "Original result could not be confirmed by current Host observation", null));
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
                    String recorded = ("Succeeded".equals(status) && reason == null) ? null
                            : (reason == null ? "Agent returned no reason" : reason);
                    settle(next, () -> repository.result(task.getUuid(), status, recorded, response));
                } catch (RuntimeException e) {
                    settle(next, () -> repository.result(task.getUuid(), "Unknown",
                            "Memory Agent result could not be processed: " + e.getMessage(), response));
                }
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
                        settle(next, () -> repository.result(task.getUuid(), "Unknown", "READ_ONLY_COMPLETION_OBSERVATION_UNQUALIFIED", null));
                        return;
                    }
                    if (support.capabilities == null
                            || !Boolean.TRUE.equals(support.capabilities.get("readOnlyCompletedReconcile"))) {
                        settle(next, () -> repository.result(task.getUuid(), "Unknown", "READ_ONLY_COMPLETION_UNSUPPORTED", null));
                        return;
                    }
                    try { call(task.getHostUuid(), endpoint, command, completion); }
                    catch (RuntimeException dispatchFailure) {
                        settle(next, () -> repository.result(task.getUuid(), "Unknown",
                                "Read-only original result could not be confirmed", null));
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
            settle(next, () -> repository.result(task.getUuid(), "Unknown", "Memory Agent dispatch outcome is unknown: " + error.getMessage(), null));
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
                .filter(host -> destMaker.isManagedByUs(host) && observationBackoff.getOrDefault(host, 0L) <= now)
                .collect(Collectors.toList());
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
                    // A transient failure is retried on the next scan; an Agent that keeps failing
                    // (unreachable or older) is retried slowly, and a reconnect clears the backoff.
                    // A native KSM partial result is the normal reply before ZRAM is first enabled.
                    boolean ksmPartial = MemoryObservationRules.isKsmNativePartial(response, host, System.currentTimeMillis());
                    if (!ksmPartial && (response == null || !response.isSuccess())) {
                        if (observationFailures.merge(host, 1, Integer::sum) >= OBSERVATION_FAILURES_BEFORE_BACKOFF) {
                            observationBackoff.put(host, System.currentTimeMillis() + OBSERVATION_FAILURE_BACKOFF_MS);
                        }
                    } else {
                        observationFailures.remove(host);
                        observationBackoff.remove(host);
                    }
                    if (ksmPartial) {
                        repository.observePartialKsm(host, response);
                    } else if (successfulFreshObservation(host, response)) {
                        repository.observe(host, response);
                        applyInheritedPolicyAfterObservation(host);
                        reconcileVerifiedPoolBlock(host, response);
                        reconcileUnknownControl(host);
                        reconcileRejectedResume(host);
                        lifecycleHooks.reconcileObservedBindings(host, response);
                        lifecycleHooks.reconcileUnknownMigrationHolds(host);
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

    private void reconcileUnknownControl(String host) {
        try { repository.reconcileUnknownControl(host); }
        catch (MemoryOperationException e) {
            logger.debug(String.format("Host[%s] unknown control reconcile was not queued: %s", host, e.getCode()));
        } catch (RuntimeException e) {
            logger.warn(String.format("Host[%s] unknown control reconcile was not accepted", host), e);
        }
    }

    private void reconcileRejectedResume(String host) {
        try { repository.reconcileRejectedResume(host); }
        catch (MemoryOperationException e) {
            logger.debug(String.format("Host[%s] rejected resume reconcile was not queued: %s", host, e.getCode()));
        } catch (RuntimeException e) {
            logger.warn(String.format("Host[%s] rejected resume reconcile was not accepted", host), e);
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
    private static final int OBSERVATION_FAILURES_BEFORE_BACKOFF = 3;
    private final Map<String, Long> observationBackoff = new ConcurrentHashMap<>();
    private final Map<String, Integer> observationFailures = new ConcurrentHashMap<>();

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
