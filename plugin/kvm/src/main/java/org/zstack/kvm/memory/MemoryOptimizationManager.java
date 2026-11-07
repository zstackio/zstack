package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.kvm.memory.MemoryRepository.License;
import org.zstack.kvm.memory.MemoryRepository.Origin;
import org.zstack.kvm.memory.MemoryStandardConfigurationCoordinator.ZramChange;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.progress.ProgressReportService;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.longjob.SubmitLongJobReply;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.*;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.longjob.LongJobUtils;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.FieldUtils;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.tag.SystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQLContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Sends each owned Host its desired memory configuration and resends it until
 * the Agent reports the generation applied. Operator operations change the
 * desired configuration and finish when the Host reaches that generation.
 */
public class MemoryOptimizationManager extends AbstractService implements ManagementNodeReadyExtensionPoint,
        ManagementNodeChangeListener, HostAfterConnectedExtensionPoint, GlobalConfigInitExtensionPoint,
        MarshalZQLASTTreeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MemoryOptimizationManager.class);
    @Autowired private CloudBus bus;
    @Autowired private ThreadFacade thdf;
    @Autowired private PluginRegistry pluginRgty;
    @Autowired private MemoryRepository repository;
    @Autowired private AccountManager accountManager;
    @Autowired private MemoryLifecycleHooks lifecycleHooks;
    @Autowired private ResourceDestinationMaker destMaker;
    private final AtomicBoolean ticking = new AtomicBoolean();
    /** Hosts with an apply in flight from this management node. */
    private final Set<String> delivering = ConcurrentHashMap.newKeySet();
    /** Hosts preparing a writeback backend; their apply waits for it. */
    private final Set<String> preparing = ConcurrentHashMap.newKeySet();
    private final Set<String> observingHosts = ConcurrentHashMap.newKeySet();
    /** The supported capabilities last written as each Host's system tags by this node. */
    private final Map<String, Set<String>> capabilityTags = new ConcurrentHashMap<>();
    /** Host system tags of the memory capabilities, as KVM's capability::iothread-vq-mapping. */
    private static final Map<String, SystemTag> CAPABILITY_TAGS = new LinkedHashMap<>();
    static {
        CAPABILITY_TAGS.put("ksm", KVMSystemTags.MEMORY_KSM);
        CAPABILITY_TAGS.put("ksmZeroPages", KVMSystemTags.MEMORY_KSM_ZERO_PAGES);
        CAPABILITY_TAGS.put("zram", KVMSystemTags.MEMORY_ZRAM);
        CAPABILITY_TAGS.put("writeback", KVMSystemTags.MEMORY_ZRAM_WRITEBACK);
    }
    // Hosts and resources whose configuration change is committed but not yet planned; true keeps a ZRAM re-enable.
    private final Map<String, ZramChange> unplannedHosts = new ConcurrentHashMap<>();
    private final Map<String, ZramChange> unplannedResources = new ConcurrentHashMap<>();
    // A global change has no resource; the maps take no null key.
    private static final String GLOBAL_RESOURCE = "";
    /** Operations waiting for their Host generation, by task. */
    private final Map<String, Waiter> waiters = new ConcurrentHashMap<>();
    private final MemoryBatchCursor observationCursor = new MemoryBatchCursor();
    /** Set when the service stops; the delivery task then cancels itself. */
    private volatile boolean stopped;
    private long lastObservation;
    private License lastLicense;
    private long lastHistoryPurge;
    /** Preparation outcomes the database did not take, by task; each scan writes them again. */
    private final Map<String, Unrecorded> unrecordedPreparations = new ConcurrentHashMap<>();

    static final class Unrecorded {
        final String host;
        final String outcome;
        Unrecorded(String host, String outcome) { this.host = host; this.outcome = outcome; }
    }

    static final String SERVICE_ID = "memoryOptimization";
    static final String PAUSE = "pause", RESUME = "resume", DRAIN = "drain";
    /** A Host step replies this long before its timeout, and waits at least HOST_STEP_MIN_WAIT. */
    private static final long HOST_STEP_REPLY_MARGIN = TimeUnit.SECONDS.toMillis(30);
    private static final long HOST_STEP_MIN_WAIT = TimeUnit.MINUTES.toMillis(1);

    @Override public String getId() { return bus.makeLocalServiceId(SERVICE_ID); }
    @Override public boolean start() { return true; }
    @Override public boolean stop() { stopped = true; return true; }

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

    @Override @ExceptionSafe
    public void afterHostConnected(HostInventory host) {
        if (host == null || host.getUuid() == null || host.getUuid().trim().isEmpty()
                || !"Connected".equals(host.getStatus())
                || !KVMConstant.KVM_HYPERVISOR_TYPE.equals(host.getHypervisorType())) {
            return;
        }
        final String hostUuid = host.getUuid();
        // Host connection callbacks run in the connection flow; only enqueue work here.
        thdf.submit(new Task<Void>() {
            @Override public String getName() { return "memory-host-connected-" + hostUuid; }
            @Override public Void call() {
                planConnected(hostUuid);
                observe(hostUuid);
                return null;
            }
        });
    }

    /** The Host may have rebooted or been reinstalled: send its generation again. */
    @ExceptionSafe @Deferred
    void planConnected(String host) {
        // The Host keeps its last applied configuration; the next observation sends it again.
        Defer.guard(() -> logger.error(String.format("Unable to plan memory configuration of connected Host[%s]", host)));
        repository.plan(host, null, "connect", "system:memory-host-connect", license(), Origin.SYSTEM);
        repository.resend(host);
        deliver(host);
    }

    @Override public void managementNodeReady() {
        clearTakenOverJobs();
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            @Override public TimeUnit getTimeUnit() { return TimeUnit.SECONDS; }
            @Override public long getInterval() { return 5; }
            @Override public String getName() { return "memory-optimization-delivery"; }
            @Override public boolean run() {
                if (stopped) { return true; }
                tick();
                return false;
            }
        });
    }

    @Override @MessageSafe
    public void handleMessage(Message message) {
        if (message instanceof APIMessage) {
            checkReadAuthorization((APIMessage) message);
        }
        if (message instanceof APIGetHostMemoryWritebackBackendsMsg) { writebackBackends((APIGetHostMemoryWritebackBackendsMsg) message); }
        else if (message instanceof APIGetMemorySummaryMsg) { summary((APIGetMemorySummaryMsg) message); }
        else if (message instanceof MemoryClusterOperationMsg) { clusterOperation((MemoryClusterOperationMsg) message); }
        else if (message instanceof MemoryHostOperationMsg) { hostOperation((MemoryHostOperationMsg) message); }
        else if (message instanceof APIPrepareHostMemoryWritebackBackendMsg) {
            APIPrepareHostMemoryWritebackBackendEvent event = new APIPrepareHostMemoryWritebackBackendEvent(message.getId());
            submitLongJob((APICreateMessage) message, event, event::setInventory);
        } else if (message instanceof APIRecoverVmMemoryMigrationHoldMsg) {
            APIRecoverVmMemoryMigrationHoldEvent event = new APIRecoverVmMemoryMigrationHoldEvent(message.getId());
            submitLongJob((APICreateMessage) message, event, event::setInventory);
        } else if (message instanceof MemoryVmMigrationHoldRecoveryMsg) {
            recoverMigrationHold((MemoryVmMigrationHoldRecoveryMsg) message);
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
    }

    /**
     * API permission/session evaluation belongs to the platform authorization
     * backend before service dispatch (including IAM2). This service retains
     * the object-level read boundary: infrastructure data remain admin-only.
     * Internal callers must use the normal API entry, not forge a session here.
     */
    void checkReadAuthorization(APIMessage api) {
        if (api.getSession() == null || api.getSession().getAccountUuid() == null) {
            throw new MemoryOperationException("MEMORY_PERMISSION_DENIED", "An authenticated account session is required");
        }

        // The memory APIs expose host-wide state, tasks, operations, policy,
        // or controls. Preserve the existing admin-only boundary.
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
                checkReadAuthorization(request);
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

    private License license() {
        List<MemoryLicenseExtensionPoint> providers = pluginRgty.getExtensionList(MemoryLicenseExtensionPoint.class);
        return providers.size() == 1 && providers.get(0).licenseDeadlineMillis() > System.currentTimeMillis()
                ? License.VALID : License.MISSING;
    }

    private void requireLicense() {
        if (license() != License.VALID) {
            throw new MemoryOperationException("MEMORY_LICENSE_UNAVAILABLE", "A valid Cloud License is required for memory configuration changes");
        }
    }

    /** The API path submits the same LongJob that SubmitLongJob creates from this request. */
    private void submitLongJob(APICreateMessage msg, APIEvent event, Consumer<LongJobInventory> inventory) {
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
        long deadline = System.currentTimeMillis() + msg.getTimeout();
        new While<>(hosts).each((host, next) -> {
            // A canceled or timed-out job leaves the Hosts it has not reached as they are.
            if (msg.getJobUuid() != null && jobCanceled(msg.getJobUuid())) {
                results.put(host, "Canceled"); next.done(); return;
            }
            // A Host step, including its Draining polls, ends before the job does, even when its owner never replies.
            long step = deadline - System.currentTimeMillis() - HOST_STEP_REPLY_MARGIN;
            if (step < HOST_STEP_MIN_WAIT + HOST_STEP_REPLY_MARGIN) {
                results.put(host, "Skipped: the job timed out"); next.done(); return;
            }
            MemoryHostOperationMsg operation = new MemoryHostOperationMsg();
            operation.setHostUuid(host); operation.setAction(msg.getAction()); operation.setJobUuid(msg.getJobUuid());
            operation.setAccountUuid(msg.getAccountUuid()); operation.setUserUuid(msg.getUserUuid());
            operation.setTimeout(step);
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

    private void recoverMigrationHold(MemoryVmMigrationHoldRecoveryMsg msg) {
        requireLicense();
        lifecycleHooks.recoverMigrationHold(msg.getVmUuid(), msg.getConfirmedHostUuid(), new Completion(msg) {
            @Override public void success() { bus.reply(msg, new MessageReply()); }
            @Override public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                MessageReply reply = new MessageReply(); reply.setError(errorCode); bus.reply(msg, reply);
            }
        });
    }

    boolean jobCanceled(String job) {
        return LongJobUtils.jobCanceled(job);
    }

    void reportClusterProgress(int done, int total) {
        ProgressReportService.reportProgress(String.valueOf(100 * done / total));
    }

    /** One Host's operation on its owner; the reply carries the Host outcome. */
    @Deferred
    private void hostOperation(MemoryHostOperationMsg msg) {
        String host = msg.getHostUuid();
        String actor = msg.getAccountUuid() + ":" + msg.getUserUuid();
        Consumer<String> finish = outcome -> {
            if (msg.getJobUuid() != null) { markActiveJob(host, null, msg.getJobUuid()); }
            replyHostOperation(msg, outcome);
        };
        if (msg.getJobUuid() != null) {
            markActiveJob(host, msg.getJobUuid(), null);
            // A rejected operation ends here: handleMessage replies its error.
            Defer.guard(() -> markActiveJob(host, null, msg.getJobUuid()));
        }
        // Reply before the caller times out; the Host keeps converging afterwards.
        long deadline = System.currentTimeMillis() + Math.max(msg.getTimeout() - HOST_STEP_REPLY_MARGIN, HOST_STEP_MIN_WAIT);
        requireLicense();
        repository.targets("Host", host);
        if (MemoryBackendPreparationRules.ACTION.equals(msg.getAction())) {
            MemoryBackendPreparationRules.validate(msg.getCandidateId());
            prepareBackend(host, msg.getCandidateId(), actor, deadline, finish);
            return;
        }
        String control = PAUSE.equals(msg.getAction()) ? "paused" : RESUME.equals(msg.getAction()) ? "active"
                : DRAIN.equals(msg.getAction()) ? "drained" : null;
        if (control == null) { throw new MemoryOperationException("MEMORY_INVALID_ACTION", "Unknown Host operation " + msg.getAction()); }
        // A Host without a pool has nothing to drain; holding it paused keeps one from forming.
        if ("drained".equals(control) && !repository.hasPool(host)) { control = "paused"; }
        MemoryRepository.Planned planned = repository.plan(host, control, msg.getAction(), actor, License.VALID, Origin.OPERATOR);
        waiters.put(planned.task, new Waiter(host, deadline, finish));
        deliver(host);
        completeWaiters();
    }

    void replyHostOperation(MemoryHostOperationMsg msg, String outcome) {
        MemoryClusterOperationReply reply = new MemoryClusterOperationReply();
        reply.setResults(Collections.singletonMap(msg.getHostUuid(), outcome));
        if (!"success".equals(outcome)) {
            reply.setError(new MemoryOperationException(MemoryClusterOperationReply.hostError(outcome).getCode(),
                    "Host[" + msg.getHostUuid() + "] " + msg.getAction() + ": " + outcome).toErrorCode());
        }
        bus.reply(msg, reply);
    }

    /** LongJobs running on this node, by job; their Host markers survive a takeover. */
    private final Set<String> runningJobs = ConcurrentHashMap.newKeySet();

    @Override public void nodeJoin(ManagementNodeInventory inv) { }
    @Override public void iAmDead(ManagementNodeInventory inv) { }
    @Override public void iJoin(ManagementNodeInventory inv) { }

    /** Hosts of a node that left are managed here from now on. */
    @Override public void nodeLeft(ManagementNodeInventory inv) { clearTakenOverJobs(); }

    private void clearTakenOverJobs() {
        repository.clearActiveJobs(repository.hostsWithState().stream().filter(destMaker::isManagedByUs)
                .collect(Collectors.toList()), new ArrayList<>(runningJobs));
    }

    private void markActiveJob(String host, String job, String previous) {
        if (job != null) { runningJobs.add(job); } else if (previous != null) { runningJobs.remove(previous); }
        recordActiveJob(host, job, previous);
    }

    @ExceptionSafe @Deferred
    void recordActiveJob(String host, String job, String previous) {
        Defer.guard(() -> logger.error(String.format("Unable to record the memory LongJob of Host[%s]; the job shown for it may be stale", host)));
        repository.setActiveJob(host, job, previous);
    }

    private static final class Waiter {
        final String host;
        final long deadline;
        final Consumer<String> done;
        Waiter(String host, long deadline, Consumer<String> done) { this.host = host; this.deadline = deadline; this.done = done; }
    }

    /** Finishes the operations whose task reached a final status. */
    @ExceptionSafe @Deferred
    void completeWaiters() {
        if (waiters.isEmpty()) { return; }
        long now = System.currentTimeMillis();
        // An operation past its deadline is still answered: its caller must not wait for the database.
        Defer.guard(() -> {
            logger.error("Unable to read memory operation status; operations past their deadline are answered as unknown");
            for (String uuid : new ArrayList<>(waiters.keySet())) {
                Waiter waiting = waiters.get(uuid);
                if (waiting != null && waiting.deadline <= now) { answer(uuid, "MEMORY_RESULT_UNKNOWN: OPERATION_STATUS_UNREADABLE"); }
            }
        });
        Map<String, MemoryTaskVO> tasks = repository.tasks(new ArrayList<>(waiters.keySet()));
        Set<String> connected = new HashSet<>(repository.connectedHosts());
        // A converging task has no reason until it finishes; the Host state records why it is still converging.
        Map<String, String> reasons = new HashMap<>();
        List<String> expired = waiters.values().stream().filter(w -> w.deadline <= now).map(w -> w.host)
                .distinct().collect(Collectors.toList());
        for (MemoryStateVO state : repository.allStates(expired)) {
            if (state.getReason() != null) { reasons.put(state.getHostUuid(), state.getReason()); }
        }
        for (String uuid : new ArrayList<>(waiters.keySet())) {
            MemoryTaskVO task = tasks.get(uuid);
            Waiter waiting = waiters.get(uuid);
            if (waiting == null) { continue; }
            // A disconnected Host is sent its generation when it reconnects; do not hold the operation for it.
            String outcome = task == null ? "Failed: operation record not found"
                    : MemoryRepository.SUCCEEDED.equals(task.getStatus()) ? "success"
                    : !MemoryRepository.CONVERGING.equals(task.getStatus())
                    ? task.getStatus() + (task.getReason() == null ? "" : ": " + task.getReason())
                    : !connected.contains(waiting.host) ? "MEMORY_RESULT_CONVERGING: HOST_DISCONNECTED"
                    : waiting.deadline <= now ? "MEMORY_RESULT_CONVERGING: " + reasons.getOrDefault(waiting.host, "APPLYING") : null;
            if (outcome != null) { answer(uuid, outcome); }
        }
    }

    /**
     * Each operation is answered once; one that cannot be replied does not hold the others. A reply that fails is
     * sent again as unknown, so the caller gets an answer instead of waiting out its timeout.
     */
    @ExceptionSafe @Deferred
    void answer(String uuid, String outcome) {
        Waiter waiter = waiters.remove(uuid);
        if (waiter == null) { return; }
        Defer.guard(() -> {
            logger.error(String.format("Unable to reply memory operation[%s] with %s; it is answered as unknown", uuid, outcome));
            waiter.done.accept("MEMORY_RESULT_UNKNOWN: OPERATION_REPLY_FAILED");
        });
        waiter.done.accept(outcome);
    }

    /**
     * Sends a Host its current generation. The Agent converges as far as it
     * can and reports Succeeded, Converging (send again later) or Failed.
     */
    void deliver(String host) {
        deliver(host, round(String.format("Host[%s] memory configuration delivery", host), 1));
    }

    @ExceptionSafe @Deferred
    void deliver(String host, MemoryLifecycleHooks.Round round) {
        // Registered before looking at preparing, as a preparation registers before looking at delivering.
        if (!delivering.add(host)) { round.finished(host, null); return; }
        // Until the Agent is called, a failure frees the Host for the next scan.
        Defer.guard(() -> {
            delivering.remove(host);
            round.finished(host, "DELIVERY_NOT_SENT");
        });
        if (preparing.contains(host)) { delivering.remove(host); round.finished(host, null); return; }
        MemoryRepository.Delivery delivery = repository.pending(host);
        if (delivery == null) { delivering.remove(host); round.finished(host, null); return; }
        // A disconnected Host is sent its generation when it reconnects, as by the scan.
        if (!repository.isConnected(host)) { delivering.remove(host); round.finished(host, null); return; }
        Map<String, Object> command = command(host, Platform.getUuid());
        command.put("generation", delivery.generation);
        command.put("desired", delivery.desired);
        call(host, "apply", command, response -> recordApply(host, delivery.generation, response, round));
    }

    /** A Converging Host is sent its generation again later; only a failed or unrecorded apply is a failure. */
    @ExceptionSafe @Deferred
    void recordApply(String host, long generation, MemoryAgentResponse response, MemoryLifecycleHooks.Round round) {
        String[] failure = {"APPLY_NOT_RECORDED"};
        Defer.defer(() -> {
            delivering.remove(host);
            round.finished(host, failure[0]);
            completeWaiters();
        });
        repository.record(host, generation, response);
        boolean applied = response != null && response.isSuccess() && (MemoryRepository.SUCCEEDED.equals(response.status)
                || MemoryRepository.CONVERGING.equals(response.status));
        failure[0] = applied ? null : String.format("generation %d: %s", generation, failure(response));
    }

    /** Prepares the selected disk, resending until the Agent finishes, then binds it to the Host. */
    @Deferred
    private void prepareBackend(String host, String candidateId, String actor, long deadline,
                                Consumer<String> finish) {
        if (!preparing.add(host)) {
            throw new MemoryOperationException("MEMORY_HOST_BUSY", "A writeback backend is already being prepared on this Host");
        }
        // A preparation that does not start leaves the Host free for the next one.
        Defer.guard(() -> preparing.remove(host));
        long generation = repository.plan(host, null, "connect", actor, License.VALID, Origin.SYSTEM).generation;
        String task = repository.startTask(host, MemoryBackendPreparationRules.ACTION, actor, generation);
        AtomicBoolean answered = new AtomicBoolean();
        Consumer<String> done = outcome -> {
            if (!answered.compareAndSet(false, true)) { return; }
            // Recorded before the Host is free, so a scan never takes this task for an interrupted one.
            recordPreparation(host, task, outcome);
            preparing.remove(host);
            deliver(host);
            // A result the platform has not recorded is unknown to the caller; the next scans record it.
            finish.accept(unrecordedPreparations.containsKey(task) ? "MEMORY_RESULT_UNKNOWN: OPERATION_STATUS_UNRECORDED" : outcome);
        };
        afterApplyInFlight(host, deadline, () -> preparationCommand(host, generation, candidateId, done,
                (command, capacity) -> sendPrepare(host, command, capacity, actor, deadline, 0, done)), done);
    }

    /** The Host's boot, pool and disk size for the candidate, read just before the disk is reset. */
    private void preparationCommand(String host, long generation, String candidateId, Consumer<String> done,
                                    java.util.function.BiConsumer<Map<String, Object>, Long> then) {
        call(host, "writeback-backends", command(host, Platform.getUuid()), response -> {
            if (response == null || !response.isSuccess() || response.state == null) {
                done.accept("Failed: " + (response == null ? "AGENT_UNREACHABLE"
                        : response.reasonCode != null ? response.reasonCode : response.reason));
                return;
            }
            String refusal = MemoryBackendPreparationRules.refusal(response.state, candidateId);
            if (refusal != null) {
                done.accept("MEMORY_BACKEND_PREPARATION_INVALID: " + refusal);
                return;
            }
            long capacity = ((Number) MemoryBackendPreparationRules.candidate(response.state, candidateId)
                    .get("capacityBytes")).longValue();
            Map<String, Object> command = command(host, Platform.getUuid());
            command.put("action", "prepare");
            command.put("generation", generation);
            command.put("candidateId", candidateId);
            command.put("expectedHostBootId", response.state.get("bootId"));
            command.put("expectedPoolGeneration", response.state.get("poolGeneration"));
            command.put("backendCapacityBytes", capacity);
            // The administrator confirmed the reset by calling the API.
            command.put("resetConfirmed", true);
            then.accept(command, capacity);
        });
    }

    /** Records the outcome on the preparation's task; one the database refuses is kept and written again by the next scans. */
    @ExceptionSafe @Deferred
    void recordPreparation(String host, String task, String outcome) {
        Defer.guard(() -> {
            unrecordedPreparations.put(task, new Unrecorded(host, outcome));
            logger.error(String.format("Unable to record the backend preparation of Host[%s], task[%s], outcome[%s];"
                    + " the next scan records it again", host, task, outcome));
        });
        repository.finishTask(task, "success".equals(outcome) ? MemoryRepository.SUCCEEDED : MemoryRepository.FAILED,
                "success".equals(outcome) ? null : outcome);
        unrecordedPreparations.remove(task);
    }

    /** Writes again the preparation outcomes the database refused. */
    void recordUnrecordedPreparations() {
        new HashMap<>(unrecordedPreparations).forEach((task, unrecorded) -> recordPreparation(unrecorded.host, task, unrecorded.outcome));
    }

    /**
     * A running preparation of a Host this node manages that no preparation of this node owns was interrupted, by a
     * restart or a takeover; its result is unknown. Each observation ends such tasks, so one not written now is
     * tried again, and keeps the ones whose result waits to be written again.
     */
    @ExceptionSafe @Deferred
    void finishInterruptedPreparations() {
        Defer.guard(() -> logger.error("Unable to finish the interrupted backend preparations; the next observation tries again"));
        Map<String, String> running = repository.runningTasks(repository.hostsWithState().stream()
                .filter(destMaker::isManagedByUs).collect(Collectors.toList()), MemoryBackendPreparationRules.ACTION);
        // Checked after the tasks are read: a preparation that started before holds its Host by now, since it holds the
        // Host before it creates its task, and one that starts later is not among the tasks read.
        List<String> interrupted = running.entrySet().stream()
                .filter(task -> !preparing.contains(task.getValue()) && !unrecordedPreparations.containsKey(task.getKey()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        repository.failRunningTasks(interrupted, "MEMORY_RESULT_UNKNOWN: PREPARATION_INTERRUPTED");
    }

    /** An apply sent before the preparation registered finishes first; the Agent would interleave the two. */
    private void afterApplyInFlight(String host, long deadline, Runnable then, Consumer<String> done) {
        if (!delivering.contains(host)) { then.run(); return; }
        if (System.currentTimeMillis() > deadline) { done.accept("MEMORY_RESULT_CONVERGING: APPLY_IN_FLIGHT"); return; }
        later(() -> afterApplyInFlight(host, deadline, then, done), 1);
    }

    private void sendPrepare(String host, Map<String, Object> command, long capacity, String actor, long deadline,
                             int failures, Consumer<String> done) {
        call(host, "writeback-backends", command,
                response -> preparationReplied(host, command, capacity, actor, deadline, failures, done, response));
    }

    /** The Agent's answer to one preparation request: bind the backend, ask again later, or finish. */
    @ExceptionSafe @Deferred
    void preparationReplied(String host, Map<String, Object> command, long capacity, String actor, long deadline,
                            int failures, Consumer<String> done, MemoryAgentResponse response) {
        // The operation is answered even when the answer cannot be recorded.
        Defer.guard(() -> done.accept("Failed: the preparation result could not be recorded"));
        if (response != null && response.isSuccess() && MemoryRepository.SUCCEEDED.equals(response.status)) {
            Object receipt = response.state == null ? null : response.state.get("maintenance");
            Object resource = receipt instanceof Map ? ((Map<?, ?>) receipt).get("backend_resource_uuid") : null;
            if (!(resource instanceof String) || ((String) resource).isEmpty()) {
                done.accept("Failed: BACKEND_RESOURCE_UNREPORTED");
                return;
            }
            repository.recordPreparedBackend(host, (String) resource, capacity);
            // The preparation reset the drained pool; a Host whose configuration still wants ZRAM activates it.
            boolean resume = repository.isDrained(host) && repository.wantsZram(host);
            repository.plan(host, resume ? "active" : null, MemoryBackendPreparationRules.ACTION, actor, license(), Origin.SYSTEM);
            done.accept("success");
            return;
        }
        boolean converging = response != null && response.isSuccess() && MemoryRepository.CONVERGING.equals(response.status);
        boolean unreachable = response == null || response.status == null;
        if (!converging && !(unreachable && failures + 1 < MemoryRepository.FAILURES_BEFORE_FAILED)) {
            // The Agent refused this request on this Host; a step it could not complete is a failure.
            boolean refused = !unreachable && MemoryRepository.FAILED.equals(response.status)
                    && response.reasonCode != null && !"BACKEND_MAINTENANCE_FAILED".equals(response.reasonCode);
            done.accept(refused ? "MEMORY_BACKEND_PREPARATION_INVALID: " + response.reasonCode
                    : "Failed: " + (response == null ? "AGENT_UNREACHABLE"
                    : response.reasonCode != null ? response.reasonCode : response.reason));
            return;
        }
        long delay = converging ? TimeUnit.SECONDS.toMillis(response.retryAfterSeconds == null ? 5
                : Math.max(1, response.retryAfterSeconds)) : MemoryRepository.retryDelay(failures + 1);
        if (System.currentTimeMillis() + delay > deadline) {
            done.accept("MEMORY_RESULT_CONVERGING: " + (response == null ? null : response.reasonCode));
            return;
        }
        thdf.submitTimeoutTask(() -> sendPrepare(host, command, capacity, actor, deadline,
                converging ? 0 : failures + 1, done), TimeUnit.MILLISECONDS, delay);
    }

    /**
     * A committed standard configuration change gives every affected Host a
     * new generation. Re-enabling ZRAM is the configuration form of resuming
     * a drained Host.
     */
    @ExceptionSafe @Deferred
    public void standardConfigurationChanged(String resourceUuid, ZramChange change) {
        String key = resourceUuid == null ? GLOBAL_RESOURCE : resourceUuid;
        // A re-enable of the same resource still waiting for its Hosts is carried by this change.
        ZramChange zram = change.or(unplannedResources.remove(key));
        Defer.guard(() -> {
            unplannedResources.merge(key, zram, ZramChange::or);
            logger.error(String.format("Unable to list the Hosts of resource[%s] for its memory configuration; the next scan retries",
                    resourceUuid));
        });
        List<String> hosts = repository.standardConfigurationTargets(resourceUuid);
        for (String host : hosts) { planConfiguration(host, zram); }
    }

    @ExceptionSafe @Deferred
    void planConfiguration(String host, ZramChange change) {
        // A retry keeps an earlier re-enable, so a drained Host still resumes.
        ZramChange zram = change.or(unplannedHosts.get(host));
        Defer.guard(() -> {
            unplannedHosts.merge(host, zram, ZramChange::or);
            logger.error(String.format("Unable to plan the memory configuration of Host[%s]; the next scan retries", host));
        });
        // Only while its configuration still wants ZRAM: a later disable keeps the Host drained.
        boolean resume = zram == ZramChange.REENABLED && repository.isDrained(host) && repository.wantsZram(host);
        repository.plan(host, resume ? "active" : null, resume ? RESUME : "configure",
                "system:standard-config", license(), Origin.SYSTEM);
        unplannedHosts.remove(host);
        if (destMaker.isManagedByUs(host)) { deliver(host); }
    }

    /** A committed configuration change is planned by the next scans, not left for a Host reconnect. */
    void planLicenseChange() {
        License now = license();
        if (lastLicense == null || lastLicense == now) { lastLicense = now; return; }
        // A plan carries the license; an expired or renewed one is planned again for the Hosts this node manages.
        // The change is taken only once its Hosts are known, so a failed read is retried at the next scan.
        List<String> hosts = repository.hostsWithState();
        lastLicense = now;
        for (String host : hosts) {
            if (destMaker.isManagedByUs(host)) { planConfiguration(host, ZramChange.NONE); }
        }
    }

    void planUnplanned() {
        new HashMap<>(unplannedResources).forEach((key, zram) ->
                standardConfigurationChanged(GLOBAL_RESOURCE.equals(key) ? null : key, zram));
        new HashMap<>(unplannedHosts).forEach(this::planConfiguration);
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
            ErrorableValue<MemoryWritebackBackendInventory> inventory = backendInventory(msg.getHostUuid(), response);
            if (!inventory.isSuccess()) {
                bus.replyErrorByMessageType(msg, new MemoryOperationException("MEMORY_BACKEND_QUERY_UNAVAILABLE",
                        "Writeback device observation is malformed; no storage was modified").toErrorCode());
                return;
            }
            APIGetHostMemoryWritebackBackendsReply reply = new APIGetHostMemoryWritebackBackendsReply();
            reply.setInventory(inventory.result);
            bus.reply(msg, reply);
        });
    }

    /** The Agent inventory as the API shows it; an inventory the Agent malformed is an error value. */
    @ExceptionSafe
    ErrorableValue<MemoryWritebackBackendInventory> backendInventory(String host, MemoryAgentResponse response) {
        return ErrorableValue.of(MemoryWritebackBackendInventory.fromAgentState(host, response.hostUuid, response.state));
    }

    private void summary(APIGetMemorySummaryMsg msg) {
        APIGetMemorySummaryReply reply = new APIGetMemorySummaryReply();
        List<String> hosts = repository.summaryTargets(msg.getHostUuids(), msg.getZoneUuid());
        long now = System.currentTimeMillis();
        // Cached Agent metrics are not a fallback: an unavailable monitoring query fails the summary.
        MemoryHostSavingsSamples measured = hostMetricSamples(hosts, now);
        Map<String, Map<String, Object>> samples = measured.getHosts();
        List<MemoryStateVO> current = repository.allStates(hosts);
        List<MemoryStateVO> states = current.stream()
                .map(state -> MemoryMetricsPresentation.withSavings(state, samples.get(state.getHostUuid())))
                .collect(Collectors.toList());
        reply.setSummary(MemorySummaryRules.summarize(hosts, states, now));
        reply.getSummary().setReason(measured.getReason());
        if (msg.getHostUuids() != null) {
            reply.getSummary().setHosts(MemorySummaryRules.hostDetails(hosts, current, samples, now));
        }
        bus.reply(msg, reply);
    }

    private MemoryMetricsExtensionPoint metricProvider() {
        List<MemoryMetricsExtensionPoint> providers = pluginRgty.getExtensionList(MemoryMetricsExtensionPoint.class);
        return providers.size() == 1 ? providers.get(0) : null;
    }

    private MemoryHostSavingsSamples hostMetricSamples(List<String> hosts, long now) {
        MemoryMetricsExtensionPoint provider = hosts.isEmpty() ? null : metricProvider();
        MemoryHostSavingsSamples samples = provider == null ? null
                : provider.hostSavings(hosts, now, MemoryOptimizationGlobalConfig.displayTtlMillis());
        return samples == null ? new MemoryHostSavingsSamples(null, null) : samples;
    }

    @ExceptionSafe @Deferred
    void tick() {
        if (!ticking.compareAndSet(false, true)) { return; }
        Defer.defer(() -> ticking.set(false));
        Defer.guard(() -> logger.error("Memory optimization scan failed; the next scan retries"));
        long now = System.currentTimeMillis();
        Set<String> connected = new HashSet<>(repository.connectedHosts());
        planUnplanned();
        List<String> due = repository.dueHosts(now).stream()
                .filter(host -> connected.contains(host) && destMaker.isManagedByUs(host)).collect(Collectors.toList());
        MemoryLifecycleHooks.Round deliveries = round("Memory configuration delivery", due.size());
        for (String host : due) { deliver(host, deliveries); }
        completeWaiters();
        boolean observation = now - lastObservation >= TimeUnit.SECONDS.toMillis(30);
        if (observation) {
            lastObservation = now;
            observeRuntime(connected);
            // Each management node keeps the public state of the Hosts it manages, as it observes them.
            // A Host whose stored configuration cannot be read gets it written again by a new plan.
            for (String unreadable : repository.refreshHostStates(repository.hostsWithState().stream()
                    .filter(destMaker::isManagedByUs).collect(Collectors.toList()))) {
                planConfiguration(unreadable, ZramChange.NONE);
            }
            finishInterruptedPreparations();
        }
        recordUnrecordedPreparations();
        if (now - lastHistoryPurge >= MemoryOptimizationGlobalConfig.historyPurgeMillis()) {
            lastHistoryPurge = now;
            repository.purgeHistory(now);
        }
        // Last: a license that cannot be read skips only this scan's license change.
        if (observation) { planLicenseChange(); }
    }

    private void observeRuntime(Set<String> connected) {
        List<String> hosts = connected.stream().filter(destMaker::isManagedByUs).collect(Collectors.toList());
        List<String> batch = new ArrayList<>(observationCursor.next("global", hosts,
                MemoryOptimizationGlobalConfig.observationBatch(hosts.size())));
        MemoryLifecycleHooks.Round observations = round("Memory observation", batch.size());
        for (String host : batch) { observe(host, observations); }
    }

    /** One round of Agent calls whose failures are logged once, as an error, by Host and reason. */
    private static MemoryLifecycleHooks.Round round(String what, int calls) {
        return new MemoryLifecycleHooks.Round(what, calls, logger::error);
    }

    /** Why an Agent call did not succeed, for a round's failures; never null, as null is a success there. */
    static String failure(MemoryAgentResponse response) {
        if (response == null) { return "AGENT_UNREACHABLE"; }
        for (String reason : Arrays.asList(response.reasonCode, response.reason, response.getError(), response.status)) {
            if (reason != null && !reason.trim().isEmpty()) { return reason; }
        }
        return "INVALID_AGENT_RESPONSE";
    }

    /** A read-only state sample: public state, service events and VM bindings follow it. */
    void observe(String host) {
        observe(host, round(String.format("Host[%s] memory observation", host), 1));
    }

    @ExceptionSafe @Deferred
    void observe(String host, MemoryLifecycleHooks.Round round) {
        // A Host already being observed is observed by that call.
        if (host == null || host.trim().isEmpty() || !observingHosts.add(host)) { round.finished(host, null); return; }
        Defer.guard(() -> {
            observingHosts.remove(host);
            round.finished(host, "OBSERVATION_NOT_SENT");
        });
        call(host, "state", command(host, Platform.getUuid()), response -> recordObservation(host, response, round));
    }

    /** A read-only sample: public state, capability tags, VM bindings and migration holds follow it. */
    @ExceptionSafe @Deferred
    void recordObservation(String host, MemoryAgentResponse response, MemoryLifecycleHooks.Round round) {
        String[] failure = {"OBSERVATION_NOT_RECORDED"};
        Defer.defer(() -> {
            observingHosts.remove(host);
            round.finished(host, failure[0]);
        });
        if (response == null || !response.isSuccess() || response.state == null) { failure[0] = failure(response); return; }
        repository.observe(host, response);
        syncCapabilityTags(host, response.state);
        lifecycleHooks.reconcileObservedBindings(host, response);
        lifecycleHooks.reconcileUnknownMigrationHolds(host);
        failure[0] = incompleteSample(response.state);
    }

    /** A sample the Agent could not complete counts as a failed observation; the Agent log names the cause. */
    static String incompleteSample(Map<String, Object> state) {
        if (Boolean.FALSE.equals(state.get("inventoryComplete"))) { return "VM_INVENTORY_INCOMPLETE"; }
        Object zram = state.get("zram");
        return zram instanceof Map && "unknown".equals(((Map<?, ?>) zram).get("serviceState")) ? "SERVICE_STATE_UNKNOWN" : null;
    }

    /** The capabilities a sample reports as supported, or null when the Agent could not discover them. */
    static Set<String> supportedCapabilities(Map<String, Object> state) {
        Object value = state == null ? null : state.get("capabilities");
        if (!(value instanceof Map) || ((Map<?, ?>) value).get("supported") == null) { return null; }
        Map<?, ?> capabilities = (Map<?, ?>) value;
        return CAPABILITY_TAGS.keySet().stream().filter(key -> Boolean.TRUE.equals(capabilities.get(key)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** A Host has the tag of each capability it supports; unknown capabilities keep the tags as they are. */
    private void syncCapabilityTags(String host, Map<String, Object> state) {
        Set<String> supported = supportedCapabilities(state);
        if (supported == null || supported.equals(capabilityTags.get(host))) { return; }
        CAPABILITY_TAGS.forEach((key, tag) -> {
            if (supported.contains(key)) {
                SystemTagCreator creator = tag.newSystemTagCreator(host);
                creator.inherent = false;
                creator.ignoreIfExisting = true;
                creator.create();
            } else {
                tag.delete(host);
            }
        });
        capabilityTags.put(host, supported);
    }

    private Map<String, Object> command(String host, String operation) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("hostUuid", host); command.put("operationUuid", operation); return command;
    }

    /** Runs a follow-up step off the callback thread after the given delay. */
    void later(Runnable task, long seconds) {
        thdf.submitTimeoutTask(task::run, TimeUnit.SECONDS, seconds);
    }

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
                    ErrorableValue<MemoryAgentResponse> decoded = agentResponse((KVMHostAsyncHttpCallReply) reply);
                    response = decoded.isSuccess() ? decoded.result : transportFailure(dispatchedCommand,
                            "Invalid Agent response: " + decoded.error.getDetails());
                }
                completion.accept(response);
            }
        });
    }

    /** The Agent's answer; one that is not a memory response is an error value. */
    @ExceptionSafe
    ErrorableValue<MemoryAgentResponse> agentResponse(KVMHostAsyncHttpCallReply reply) {
        return ErrorableValue.of(reply.toResponse(MemoryAgentResponse.class));
    }

    static boolean readOnlyAgentQuery(String action) {
        return "state".equals(action) || "accounting".equals(action);
    }

    private static MemoryAgentResponse transportFailure(Map<String, Object> command, String reason) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        Object operation = command.get("operationUuid");
        response.operationUuid = operation == null ? null : String.valueOf(operation);
        response.reasonCode = "AGENT_UNREACHABLE";
        response.reason = reason == null ? "Host transport rejected the request before Agent execution" : reason;
        response.setSuccess(false);
        response.setError(response.reason);
        return response;
    }
}
