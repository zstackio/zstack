package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigInitExtensionPoint;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.Q;
import org.zstack.core.progress.ProgressReportService;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.longjob.SubmitLongJobReply;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.longjob.LongJobUtils;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQLContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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
        HostAfterConnectedExtensionPoint, GlobalConfigInitExtensionPoint, MarshalZQLASTTreeExtensionPoint {
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
    // Hosts and resources whose configuration change is committed but not yet planned; true keeps a ZRAM re-enable.
    private final Map<String, Boolean> unplannedHosts = new ConcurrentHashMap<>();
    private final Map<String, Boolean> unplannedResources = new ConcurrentHashMap<>();
    // A global change has no resource; the maps take no null key.
    private static final String GLOBAL_RESOURCE = "";
    /** Operations waiting for their Host generation, by task. */
    private final Map<String, Waiter> waiters = new ConcurrentHashMap<>();
    private final MemoryBatchCursor observationCursor = new MemoryBatchCursor();
    private Future<?> poller;
    private long lastObservation;
    private Boolean lastLicensed;
    private long lastHistoryPurge;

    static final String SERVICE_ID = "memoryOptimization";
    static final String PAUSE = "pause", RESUME = "resume", DRAIN = "drain";

    @Override public String getId() { return bus.makeLocalServiceId(SERVICE_ID); }
    @Override public boolean start() { return true; }
    @Override public boolean stop() { if (poller != null) { poller.cancel(false); } return true; }

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
        try {
            // Host connection callbacks run in the connection flow; only enqueue work here.
            thdf.submit(new Task<Void>() {
                @Override public String getName() { return "memory-host-connected-" + hostUuid; }
                @Override public Void call() {
                    try {
                        // The Host may have rebooted or been reinstalled: send its generation again.
                        repository.plan(hostUuid, null, "connect", "system:memory-host-connect", licensed(), false);
                        repository.resend(hostUuid);
                        deliver(hostUuid);
                    } catch (RuntimeException e) {
                        logger.warn(String.format("Unable to plan memory configuration of connected Host[%s]", hostUuid), e);
                    }
                    observe(hostUuid);
                    return null;
                }
            });
        } catch (RuntimeException e) {
            logger.warn(String.format("Unable to enqueue memory configuration after Host[%s] connected; periodic scan will retry",
                    hostUuid), e);
        }
    }

    @Override public void managementNodeReady() {
        poller = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override public TimeUnit getTimeUnit() { return TimeUnit.SECONDS; }
            @Override public long getInterval() { return 5; }
            @Override public String getName() { return "memory-optimization-delivery"; }
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
            // Reuse owner/public/private sharing semantics without running
            // legacy UserVO policy evaluation on an IAM2 VirtualID session.
            List<String> accessible = accountManager.getResourceUuidsCanAccessByAccount(
                    api.getSession().getAccountUuid(), VmInstanceVO.class);
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

    private boolean licensed() {
        List<MemoryLicenseExtensionPoint> providers = pluginRgty.getExtensionList(MemoryLicenseExtensionPoint.class);
        return providers.size() == 1 && providers.get(0).licenseDeadlineMillis() > System.currentTimeMillis();
    }

    private void requireLicense() {
        if (!licensed()) {
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
            if (System.currentTimeMillis() >= deadline) {
                results.put(host, "Skipped: the job timed out"); next.done(); return;
            }
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
    private void hostOperation(MemoryHostOperationMsg msg) {
        String host = msg.getHostUuid();
        String actor = msg.getAccountUuid() + ":" + msg.getUserUuid();
        Consumer<String> finish = outcome -> {
            if (msg.getJobUuid() != null) { markActiveJob(host, null, msg.getJobUuid()); }
            replyHostOperation(msg, outcome);
        };
        if (msg.getJobUuid() != null) { markActiveJob(host, msg.getJobUuid(), null); }
        // Reply before the caller times out; the Host keeps converging afterwards.
        long deadline = System.currentTimeMillis() + Math.max(msg.getTimeout() - TimeUnit.SECONDS.toMillis(30),
                TimeUnit.MINUTES.toMillis(1));
        try {
            requireLicense();
            repository.targets("Host", host);
            if (MemoryBackendPreparationRules.ACTION.equals(msg.getAction())) {
                MemoryBackendPreparationRules.validate(msg.getPreparation());
                prepareBackend(host, msg.getPreparation(), actor, deadline, finish);
                return;
            }
            String control = PAUSE.equals(msg.getAction()) ? "paused" : RESUME.equals(msg.getAction()) ? "active"
                    : DRAIN.equals(msg.getAction()) ? "drained" : null;
            if (control == null) { throw new MemoryOperationException("MEMORY_INVALID_ACTION", "Unknown Host operation " + msg.getAction()); }
            // A Host without a pool has nothing to drain; holding it paused keeps one from forming.
            if ("drained".equals(control) && !repository.hasPool(host)) { control = "paused"; }
            MemoryRepository.Planned planned = repository.plan(host, control, msg.getAction(), actor, true, true);
            waiters.put(planned.task, new Waiter(host, deadline, finish));
        } catch (MemoryOperationException e) {
            finish.accept(e.getCode() + ": " + e.getMessage());
            return;
        } catch (RuntimeException e) {
            finish.accept("Failed: " + e.getMessage());
            return;
        }
        deliver(host);
        completeWaiters();
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

    private void markActiveJob(String host, String job, String previous) {
        try {
            repository.setActiveJob(host, job, previous);
        } catch (RuntimeException e) {
            logger.warn(String.format("unable to record the memory LongJob of Host[%s]; it is display-only", host), e);
        }
    }

    private static final class Waiter {
        final String host;
        final long deadline;
        final Consumer<String> done;
        Waiter(String host, long deadline, Consumer<String> done) { this.host = host; this.deadline = deadline; this.done = done; }
    }

    /** Finishes the operations whose task reached a final status. */
    private void completeWaiters() {
        if (waiters.isEmpty()) { return; }
        Map<String, MemoryTaskVO> tasks;
        Set<String> connected;
        try {
            tasks = repository.tasks(new ArrayList<>(waiters.keySet()));
            connected = new HashSet<>(repository.connectedHosts());
        } catch (RuntimeException e) {
            logger.warn("Unable to read memory operation status; waiting operations are checked again", e);
            return;
        }
        long now = System.currentTimeMillis();
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
                    : waiting.deadline <= now ? "MEMORY_RESULT_CONVERGING: " + task.getReason() : null;
            if (outcome == null) { continue; }
            Waiter waiter = waiters.remove(uuid);
            if (waiter == null) { continue; }
            try { waiter.done.accept(outcome); }
            catch (RuntimeException e) { logger.warn(String.format("Unable to reply memory operation[%s]", uuid), e); }
        }
    }

    /**
     * Sends a Host its current generation. The Agent converges as far as it
     * can and reports Succeeded, Converging (send again later) or Failed.
     */
    private void deliver(String host) {
        // Registered before looking at preparing, as a preparation registers before looking at delivering.
        if (!delivering.add(host)) { return; }
        if (preparing.contains(host)) { delivering.remove(host); return; }
        MemoryRepository.Delivery delivery;
        try {
            delivery = repository.pending(host);
        } catch (RuntimeException e) {
            delivering.remove(host);
            logger.warn(String.format("Unable to read the memory configuration of Host[%s]", host), e);
            return;
        }
        if (delivery == null) { delivering.remove(host); return; }
        Map<String, Object> command = command(host, Platform.getUuid());
        command.put("generation", delivery.generation);
        command.put("desired", delivery.desired);
        Consumer<MemoryAgentResponse> recorded = response -> {
            try {
                repository.record(host, delivery.generation, response);
            } catch (RuntimeException e) {
                logger.warn(String.format("Unable to record the memory apply result of Host[%s]; it is sent again", host), e);
            } finally {
                delivering.remove(host);
                completeWaiters();
            }
        };
        try {
            call(host, "apply", command, recorded);
        } catch (RuntimeException e) {
            recorded.accept(transportFailure(command, e.getMessage()));
        }
    }

    /** Prepares the selected disk, resending until the Agent finishes, then binds it to the Host. */
    private void prepareBackend(String host, MemoryBackendPreparation preparation, String actor, long deadline,
                                Consumer<String> finish) {
        if (!preparing.add(host)) {
            throw new MemoryOperationException("MEMORY_HOST_BUSY", "A writeback backend is already being prepared on this Host");
        }
        long generation;
        String task;
        try {
            generation = repository.plan(host, null, "connect", actor, true, false).generation;
            task = repository.startTask(host, MemoryBackendPreparationRules.ACTION, actor, generation);
        } catch (RuntimeException e) {
            preparing.remove(host);
            throw e;
        }
        Consumer<String> done = outcome -> {
            preparing.remove(host);
            try {
                repository.finishTask(task, "success".equals(outcome) ? MemoryRepository.SUCCEEDED : MemoryRepository.FAILED,
                        "success".equals(outcome) ? null : outcome);
            } catch (RuntimeException e) {
                logger.warn(String.format("Unable to record the backend preparation of Host[%s]", host), e);
            }
            deliver(host);
            finish.accept(outcome);
        };
        Map<String, Object> command = command(host, Platform.getUuid());
        command.put("action", "prepare");
        command.put("generation", generation);
        command.put("candidateId", preparation.getCandidateId());
        command.put("expectedHostBootId", preparation.getExpectedHostBootId());
        command.put("expectedPoolGeneration", preparation.getExpectedPoolGeneration());
        command.put("backendCapacityBytes", preparation.getBackendCapacityBytes());
        command.put("resetConfirmed", preparation.isResetConfirmed());
        afterApplyInFlight(host, deadline,
                () -> sendPrepare(host, command, preparation.getBackendCapacityBytes(), actor, deadline, 0, done), done);
    }

    /** An apply sent before the preparation registered finishes first; the Agent would interleave the two. */
    private void afterApplyInFlight(String host, long deadline, Runnable then, Consumer<String> done) {
        if (!delivering.contains(host)) { then.run(); return; }
        if (System.currentTimeMillis() > deadline) { done.accept("MEMORY_RESULT_CONVERGING: APPLY_IN_FLIGHT"); return; }
        later(() -> afterApplyInFlight(host, deadline, then, done), 1);
    }

    private void sendPrepare(String host, Map<String, Object> command, long capacity, String actor, long deadline,
                             int failures, Consumer<String> done) {
        call(host, "writeback-backends", command, response -> {
            try {
                if (response != null && response.isSuccess() && MemoryRepository.SUCCEEDED.equals(response.status)) {
                    Object receipt = response.state == null ? null : response.state.get("maintenance");
                    Object resource = receipt instanceof Map ? ((Map<?, ?>) receipt).get("backend_resource_uuid") : null;
                    if (!(resource instanceof String) || ((String) resource).isEmpty()) {
                        done.accept("Failed: BACKEND_RESOURCE_UNREPORTED");
                        return;
                    }
                    repository.recordPreparedBackend(host, (String) resource, capacity);
                    repository.plan(host, null, MemoryBackendPreparationRules.ACTION, actor, licensed(), false);
                    done.accept("success");
                    return;
                }
                boolean converging = response != null && response.isSuccess() && MemoryRepository.CONVERGING.equals(response.status);
                boolean unreachable = response == null || response.status == null;
                if (!converging && !(unreachable && failures + 1 < MemoryRepository.FAILURES_BEFORE_FAILED)) {
                    done.accept("Failed: " + (response == null ? "AGENT_UNREACHABLE"
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
            } catch (RuntimeException e) {
                done.accept("Failed: " + e.getMessage());
            }
        });
    }

    /**
     * A committed standard configuration change gives every affected Host a
     * new generation. Re-enabling ZRAM is the configuration form of resuming
     * a drained Host.
     */
    public void standardConfigurationChanged(String resourceUuid, boolean zramReenabled) {
        String key = resourceUuid == null ? GLOBAL_RESOURCE : resourceUuid;
        // A re-enable of the same resource still waiting for its Hosts is carried by this change.
        boolean reenabled = zramReenabled || Boolean.TRUE.equals(unplannedResources.remove(key));
        List<String> hosts;
        try {
            hosts = repository.standardConfigurationTargets(resourceUuid);
        } catch (RuntimeException e) {
            unplannedResources.merge(key, reenabled, Boolean::logicalOr);
            logger.error(String.format("Unable to list the Hosts of resource[%s] for its memory configuration; the next scan retries",
                    resourceUuid), e);
            return;
        }
        for (String host : hosts) { planConfiguration(host, reenabled); }
    }

    private void planConfiguration(String host, boolean zramReenabled) {
        // A retry keeps an earlier re-enable, so a drained Host still resumes.
        boolean reenabled = zramReenabled || Boolean.TRUE.equals(unplannedHosts.get(host));
        try {
            // Only while its configuration still wants ZRAM: a later disable keeps the Host drained.
            boolean resume = reenabled && repository.isDrained(host) && repository.wantsZram(host);
            repository.plan(host, resume ? "active" : null, resume ? RESUME : "configure",
                    "system:standard-config", licensed(), false);
            unplannedHosts.remove(host);
            if (destMaker.isManagedByUs(host)) { deliver(host); }
        } catch (RuntimeException e) {
            unplannedHosts.merge(host, reenabled, Boolean::logicalOr);
            logger.error(String.format("Unable to plan the memory configuration of Host[%s]; the next scan retries", host), e);
        }
    }

    /** A committed configuration change is planned by the next scans, not left for a Host reconnect. */
    void planLicenseChange() {
        boolean now = licensed();
        Boolean before = lastLicensed;
        lastLicensed = now;
        if (before == null || before == now) { return; }
        // A plan carries the license; an expired or renewed one is planned again for the Hosts this node manages.
        for (String host : repository.hostsWithState()) {
            if (destMaker.isManagedByUs(host)) { planConfiguration(host, false); }
        }
    }

    void planUnplanned() {
        new HashMap<>(unplannedResources).forEach((key, reenabled) ->
                standardConfigurationChanged(GLOBAL_RESOURCE.equals(key) ? null : key, reenabled));
        new HashMap<>(unplannedHosts).forEach(this::planConfiguration);
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

    private void tick() {
        if (!ticking.compareAndSet(false, true)) { return; }
        try {
            long now = System.currentTimeMillis();
            Set<String> connected = new HashSet<>(repository.connectedHosts());
            planUnplanned();
            for (String host : repository.dueHosts(now)) {
                if (connected.contains(host) && destMaker.isManagedByUs(host)) { deliver(host); }
            }
            completeWaiters();
            if (now - lastObservation >= TimeUnit.SECONDS.toMillis(30)) {
                lastObservation = now;
                planLicenseChange();
                observeRuntime(connected);
                // Each management node keeps the public state of the Hosts it manages, as it observes them.
                repository.refreshHostStates(repository.hostsWithState().stream()
                        .filter(destMaker::isManagedByUs).collect(Collectors.toList()));
            }
            if (now - lastHistoryPurge >= TimeUnit.HOURS.toMillis(1)) {
                lastHistoryPurge = now;
                repository.purgeHistory(now);
            }
        } catch (Exception e) {
            logger.warn("Memory optimization scan failed; the next scan retries", e);
        } finally { ticking.set(false); }
    }

    private void observeRuntime(Set<String> connected) {
        List<String> hosts = connected.stream().filter(destMaker::isManagedByUs).collect(Collectors.toList());
        for (String host : observationCursor.next("global", hosts, MemoryOptimizationGlobalConfig.observationBatch(hosts.size()))) {
            observe(host);
        }
    }

    /** A read-only state sample: public state, service events and VM bindings follow it. */
    void observe(String host) {
        if (host == null || host.trim().isEmpty() || !observingHosts.add(host)) { return; }
        try {
            call(host, "state", command(host, Platform.getUuid()), response -> {
                try {
                    if (response == null || !response.isSuccess() || response.state == null) { return; }
                    repository.observe(host, response);
                    lifecycleHooks.reconcileObservedBindings(host, response);
                    lifecycleHooks.reconcileUnknownMigrationHolds(host);
                } catch (RuntimeException e) {
                    logger.warn(String.format("Host[%s] memory observation failed; periodic scan will retry", host), e);
                } finally {
                    observingHosts.remove(host);
                }
            });
        } catch (RuntimeException e) {
            observingHosts.remove(host);
            logger.warn(String.format("Host[%s] memory observation could not be sent; periodic scan will retry", host), e);
        }
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
