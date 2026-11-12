package org.zstack.kvm.memory;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Each Host has one desired configuration: the effective standard configs and
 * the operator's lifecycle control. A change increments its generation; the
 * owning management node resends that generation until the Agent applies it.
 */
public class MemoryRepository {
    private static final CLogger logger = Utils.getLogger(MemoryRepository.class);
    static final String SUCCEEDED = "Succeeded";
    static final String RELEASED = "Released";
    static final String CONVERGING = "Converging";
    static final String FAILED = "Failed";
    static final String RUNNING = "Running";
    static final String SUPERSEDED = "Superseded";
    static final int FAILURES_BEFORE_FAILED = 5;

    /** Whether a valid Cloud License allows the optimization that a plan configures. */
    public enum License { VALID, MISSING }

    /** Who asked for a plan: an operator's request is recorded as a task even when nothing changed. */
    public enum Origin { OPERATOR, SYSTEM }

    /** Whether the Host is expected to run the ZRAM service: wanted by its configuration and supported. */
    enum ZramExpectation { RUNNING, NONE }
    /** Public state reasons for stored data that cannot be read. */
    static final String CONFIGURATION_UNREADABLE = "CONFIGURATION_UNREADABLE";
    static final String OBSERVATION_UNREADABLE = "OBSERVATION_UNREADABLE";
    /** Hosts refreshed in one transaction. */
    private static final int REFRESH_BATCH = 200;
    static final long MAX_RETRY_MILLIS = TimeUnit.MINUTES.toMillis(10);

    @Autowired private DatabaseFacade dbf;
    @Autowired private EventFacade evtf;
    @Autowired private GlobalConfigFacade gcf;
    private final MemoryStandardConfigAdapter standardConfigs = new MemoryStandardConfigAdapter();
    private static final Gson GSON = new Gson();

    protected <T> T transaction(Function<EntityManager, T> function) {
        return new SQLBatchWithReturn<T>() {
            @Override protected T scripts() { return function.apply(dbf.getEntityManager()); }
        }.execute();
    }

    static MemoryOperationException error(String code, String message) { return new MemoryOperationException(code, message); }

    // ---------------------------------------------------------------- scope

    private void validateResource(EntityManager em, String scope, String resource) {
        String refusal = resourceRefusal(scope, resource);
        if (refusal != null) { throw error("MEMORY_INVALID_SCOPE", refusal); }
    }

    /** Why the resource of the scope cannot be targeted, or null when it can. */
    private static String resourceRefusal(String scope, String resource) {
        if ("Global".equals(scope)) { return "global".equals(resource) ? null : "Global resource must be global"; }
        if ("Cluster".equals(scope)) {
            return KVMConstant.KVM_HYPERVISOR_TYPE.equals(clusterHypervisor(resource)) ? null : "Expected an existing KVM Cluster";
        }
        if ("Host".equals(scope)) {
            return KVMConstant.KVM_HYPERVISOR_TYPE.equals(hostHypervisor(resource)) ? null : "Expected an existing KVM Host";
        }
        if ("VM".equals(scope)) {
            return Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, resource).isExists() ? null : "VM does not exist";
        }
        return "Unknown memory scope";
    }

    private static boolean nonKvm(EntityManager em, String scope, String resource) {
        String type = "Host".equals(scope) ? hostHypervisor(resource) : "Cluster".equals(scope) ? clusterHypervisor(resource) : null;
        return type != null && !KVMConstant.KVM_HYPERVISOR_TYPE.equals(type);
    }

    private static String hostHypervisor(String host) {
        return Q.New(HostVO.class).select(HostVO_.hypervisorType).eq(HostVO_.uuid, host).findValue();
    }

    private static String clusterHypervisor(String cluster) {
        return Q.New(ClusterVO.class).select(ClusterVO_.hypervisorType).eq(ClusterVO_.uuid, cluster).findValue();
    }

    private static String hostCluster(String host) {
        return Q.New(HostVO.class).select(HostVO_.clusterUuid).eq(HostVO_.uuid, host).findValue();
    }

    private List<String> targets(EntityManager em, String scope, String resource) {
        validateResource(em, scope, resource);
        if ("Global".equals(scope)) {
            return Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                    .orderBy(HostVO_.uuid, SimpleQuery.Od.ASC).listValues();
        }
        if ("Host".equals(scope)) { return Collections.singletonList(resource); }
        if ("Cluster".equals(scope)) {
            return Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.clusterUuid, resource)
                    .eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE).orderBy(HostVO_.uuid, SimpleQuery.Od.ASC).listValues();
        }
        String host = Q.New(VmInstanceVO.class).select(VmInstanceVO_.hostUuid).eq(VmInstanceVO_.uuid, resource).findValue();
        return host == null ? Collections.emptyList() : Collections.singletonList(host);
    }

    public List<String> targets(String scope, String resource) {
        return transaction(em -> targets(em, scope, resource));
    }

    public List<String> connectedHosts() {
        return transaction(em -> Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .eq(HostVO_.status, HostStatus.Connected).orderBy(HostVO_.uuid, SimpleQuery.Od.ASC).listValues());
    }

    public boolean isConnected(String host) {
        return transaction(em -> Q.New(HostVO.class).eq(HostVO_.uuid, host).eq(HostVO_.status, HostStatus.Connected).isExists());
    }

    public List<String> summaryTargets(List<String> requested, String zoneUuid) {
        if (requested != null && requested.isEmpty()) { return Collections.emptyList(); }
        return transaction(em -> {
            List<String> hosts;
            if (requested == null) {
                hosts = targets(em, "Global", "global");
            } else {
                Set<String> selected = new LinkedHashSet<>(requested);
                hosts = Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                        .in(HostVO_.uuid, selected).orderBy(HostVO_.uuid, SimpleQuery.Od.ASC).listValues();
                // Recheck KVM eligibility and a resource deleted since API admission.
                if (hosts.size() != selected.size()) {
                    throw error("MEMORY_INVALID_SCOPE", "Expected an existing KVM Host");
                }
            }
            if (zoneUuid == null || hosts.isEmpty()) { return hosts; }
            return Q.New(HostVO.class).select(HostVO_.uuid).eq(HostVO_.zoneUuid, zoneUuid).in(HostVO_.uuid, hosts)
                    .orderBy(HostVO_.uuid, SimpleQuery.Od.ASC).listValues();
        });
    }

    public List<MemoryStateVO> allStates(List<String> hosts) {
        if (hosts == null || hosts.isEmpty()) { return Collections.emptyList(); }
        return transaction(em -> Q.New(MemoryStateVO.class).in(MemoryStateVO_.hostUuid, hosts).list());
    }

    public MemoryStateVO state(String host) {
        return transaction(em -> em.find(MemoryStateVO.class, host));
    }

    // ------------------------------------------------------- standard config

    /** Standard memory configuration is stored by GlobalConfig (no resource) or a Host/Cluster ResourceConfig. */
    private String standardScope(EntityManager em, String resourceUuid) {
        String scope = standardScopeOrNull(resourceUuid);
        if (scope == null) { throw error("MEMORY_INVALID_SCOPE", NOT_HOST_OR_CLUSTER); }
        return scope;
    }

    private static final String NOT_HOST_OR_CLUSTER = "Memory configuration requires a KVM Host or Cluster";

    /** Global, Host or Cluster; null for any other resource. */
    private static String standardScopeOrNull(String resourceUuid) {
        if (resourceUuid == null) { return "Global"; }
        // Only the registry discriminator is needed; materializing JOINED
        // ResourceVO exceeds MariaDB's join limit in the full product.
        String resourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).eq(ResourceVO_.uuid, resourceUuid).findValue();
        if (HostVO.class.getSimpleName().equals(resourceType)) { return "Host"; }
        if (org.zstack.header.cluster.ClusterVO.class.getSimpleName().equals(resourceType)) { return "Cluster"; }
        return null;
    }

    private static MemoryStandardField.Scope scopeOf(String scope) {
        return "Global".equals(scope) ? MemoryStandardField.Scope.GLOBAL
                : "Cluster".equals(scope) ? MemoryStandardField.Scope.CLUSTER : MemoryStandardField.Scope.HOST;
    }

    /** Why a standard value cannot be stored on the resource, or null when it can. */
    public String standardChangeRefusal(MemoryStandardField field, String resourceUuid) {
        return transaction(em -> {
            String scope = standardScopeOrNull(resourceUuid);
            if (scope == null) { return NOT_HOST_OR_CLUSTER; }
            if (!field.allows(scopeOf(scope))) { return field.path() + " does not support " + scope; }
            // The legacy host.ksm switch was settable on any Host or Cluster before memory optimization.
            if (field == MemoryStandardField.KSM_ENABLED && nonKvm(em, scope, resourceUuid)) { return null; }
            return resourceRefusal(scope, resourceUuid == null ? "global" : resourceUuid);
        });
    }

    /** The KVM Hosts whose effective configuration a committed standard change may alter. */
    public List<String> standardConfigurationTargets(String resourceUuid) {
        return transaction(em -> {
            String scope = standardScope(em, resourceUuid);
            if (nonKvm(em, scope, resourceUuid)) { return Collections.<String>emptyList(); }
            return targets(em, scope, resourceUuid == null ? "global" : resourceUuid);
        });
    }

    /**
     * The most specific stored value of each field wins; none leaves the setting unmanaged.
     * The Host's and Cluster's overrides are read together under the Host's lock, so a concurrent
     * change is never planned over by an older read; GlobalConfig values are the node's own.
     */
    private Map<MemoryStandardField, Object> effectiveValues(EntityManager em, String host, String cluster) {
        Map<String, Map<MemoryStandardField, Object>> overrides = standardConfigs.readOverridesOwn(em,
                cluster == null ? Collections.singletonList(host) : Arrays.asList(host, cluster));
        Map<MemoryStandardField, Object> hostValues = overrides.getOrDefault(host, Collections.emptyMap());
        Map<MemoryStandardField, Object> clusterValues = cluster == null ? Collections.emptyMap()
                : overrides.getOrDefault(cluster, Collections.emptyMap());
        Map<MemoryStandardField, Object> values = new EnumMap<>(MemoryStandardField.class);
        for (MemoryStandardField field : MemoryStandardField.values()) {
            Object value = field.allows(MemoryStandardField.Scope.HOST) ? hostValues.get(field) : null;
            if (value == null && field.allows(MemoryStandardField.Scope.CLUSTER)) { value = clusterValues.get(field); }
            if (value == null && field.allows(MemoryStandardField.Scope.GLOBAL)) { value = standardConfigs.readGlobalOwn(gcf, field); }
            values.put(field, MemoryStandardConfigAdapter.isUnmanaged(value) ? null : value);
        }
        return values;
    }

    Map<String, Object> desired(EntityManager em, String host, String control, License license) {
        Map<MemoryStandardField, Object> values = effectiveValues(em, host, hostCluster(host));
        Map<String, Object> ksm = new LinkedHashMap<>();
        ksm.put("enabled", values.get(MemoryStandardField.KSM_ENABLED));
        Map<String, Object> zram = new LinkedHashMap<>();
        Map<String, Object> writeback = new LinkedHashMap<>();
        // The legacy host.ksm switch stays license-free; memory optimization does not.
        if (license == License.VALID) {
            ksm.put("zeroPagesEnabled", values.get(MemoryStandardField.KSM_ZERO_PAGES));
            zram.put("enabled", values.get(MemoryStandardField.ZRAM_ENABLED));
            zram.put("logicalCapacityBytes", values.get(MemoryStandardField.ZRAM_LOGICAL_CAPACITY));
            zram.put("ramLimitBytes", values.get(MemoryStandardField.ZRAM_RAM_LIMIT));
            Object enabled = values.get(MemoryStandardField.WRITEBACK_ENABLED);
            // Writeback runs inside ZRAM on the Host's prepared backend; without either it stays off.
            boolean prepared = values.get(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE) != null;
            boolean zramOn = Boolean.TRUE.equals(zram.get("enabled"));
            writeback.put("enabled", Boolean.TRUE.equals(enabled) && !(prepared && zramOn) ? Boolean.FALSE : enabled);
        } else {
            // Without a license ZRAM stops reclaiming; only a licensed none leaves it unmanaged.
            zram.put("enabled", Boolean.FALSE);
        }
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("control", control);
        desired.put("ksm", ksm);
        desired.put("zram", zram);
        desired.put("writeback", writeback);
        return desired;
    }

    // ------------------------------------------------------- generations

    /** A Host's generation to send, or null when its desired configuration is already delivered. */
    public static final class Delivery {
        public final String host;
        public final long generation;
        public final Map<String, Object> desired;
        Delivery(String host, long generation, Map<String, Object> desired) {
            this.host = host; this.generation = generation; this.desired = desired;
        }
    }

    /** The Host's state, locked; the first plan creates it, and a concurrent first plan waits for that row. */
    private MemoryStateVO lockState(EntityManager em, String host) {
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        em.createNativeQuery("INSERT IGNORE INTO MemoryStateVO (hostUuid, desiredRevision, status, control, failures, createDate, lastOpDate)"
                        + " VALUES (:hostUuid, 0, :status, 'active', 0, :now, :now)")
                .setParameter("hostUuid", host).setParameter("status", CONVERGING).setParameter("now", now)
                .executeUpdate();
        return em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
    }

    /** The generation a plan produced and, for an operation, the task that waits for it. */
    public static final class Planned {
        public final long generation;
        public final String task;
        Planned(long generation, String task) { this.generation = generation; this.task = task; }
    }

    /**
     * Recompute the desired configuration; a change starts a new generation.
     * An operator operation is always recorded as a task carrying its
     * generation, also when the configuration is already the desired one.
     */
    public Planned plan(String host, String control, String action, String actor, License license, Origin origin) {
        return transaction(em -> {
            validateResource(em, "Host", host);
            MemoryStateVO state = lockState(em, host);
            if (control != null) { state.setControl(control); }
            String desired = GSON.toJson(desired(em, host, state.getControl(), license));
            boolean changed = !desired.equals(state.getDesired());
            if (changed) {
                long generation = state.getDesiredRevision() + 1;
                state.setDesiredRevision(generation); state.setDesired(desired);
                state.setStatus(CONVERGING); state.setReason(null); state.setFailures(0);
                state.setNextAttemptAt(System.currentTimeMillis());
                finishTasks(em, host, generation - 1, SUPERSEDED, "Superseded by generation " + generation);
            } else if (origin == Origin.OPERATOR && !CONVERGING.equals(state.getStatus())) {
                // An operator asking again resends the current generation, also to a Host that gave up.
                state.setStatus(CONVERGING); state.setFailures(0); state.setNextAttemptAt(System.currentTimeMillis());
            }
            String task = null;
            if (changed || origin == Origin.OPERATOR) {
                task = newTask(em, host, action, actor, state.getDesiredRevision(), CONVERGING);
            }
            em.flush();
            refreshHostState(em, state);
            return new Planned(state.getDesiredRevision(), task);
        });
    }

    private static String newTask(EntityManager em, String host, String action, String actor, long generation, String status) {
        MemoryTaskVO task = new MemoryTaskVO();
        task.setUuid(Platform.getUuid()); task.setHostUuid(host); task.setAction(action);
        task.setActorUuid(actor); task.setStatus(status); task.setDesiredRevision(generation);
        em.persist(task);
        return task.getUuid();
    }

    /** A task that is finished by its caller rather than by a Host generation. */
    public String startTask(String host, String action, String actor, long generation) {
        return transaction(em -> newTask(em, host, action, actor, generation, RUNNING));
    }

    /** The Hosts' running tasks of the action: task UUID to Host UUID. */
    public Map<String, String> runningTasks(Collection<String> hosts, String action) {
        if (hosts.isEmpty()) { return Collections.emptyMap(); }
        return transaction(em -> Q.New(MemoryTaskVO.class).in(MemoryTaskVO_.hostUuid, hosts).eq(MemoryTaskVO_.action, action)
                .eq(MemoryTaskVO_.status, RUNNING).<MemoryTaskVO>list().stream()
                .collect(Collectors.toMap(MemoryTaskVO::getUuid, MemoryTaskVO::getHostUuid)));
    }

    /** These tasks end Failed with the reason if they are still running; a task finished meanwhile keeps its result. */
    public void failRunningTasks(Collection<String> tasks, String reason) {
        if (tasks.isEmpty()) { return; }
        transaction(em -> {
            SQL.New(MemoryTaskVO.class).in(MemoryTaskVO_.uuid, tasks).eq(MemoryTaskVO_.status, RUNNING)
                    .set(MemoryTaskVO_.status, FAILED).set(MemoryTaskVO_.reason, trim(reason)).update();
            return null;
        });
    }

    public void finishTask(String uuid, String status, String reason) {
        transaction(em -> {
            MemoryTaskVO task = em.find(MemoryTaskVO.class, uuid);
            if (task != null) { task.setStatus(status); task.setReason(trim(reason)); }
            return null;
        });
    }

    public Map<String, MemoryTaskVO> tasks(Collection<String> uuids) {
        if (uuids.isEmpty()) { return Collections.emptyMap(); }
        return transaction(em -> Q.New(MemoryTaskVO.class).in(MemoryTaskVO_.uuid, uuids).<MemoryTaskVO>list().stream()
                .collect(Collectors.toMap(MemoryTaskVO::getUuid, t -> t)));
    }

    /** Whether the Host's effective configuration turns ZRAM on now. */
    public boolean wantsZram(String host) {
        return transaction(em -> Boolean.TRUE.equals(
                effectiveValues(em, host, hostCluster(host)).get(MemoryStandardField.ZRAM_ENABLED)));
    }

    public boolean isDrained(String host) {
        MemoryStateVO state = state(host);
        return state != null && "drained".equals(state.getControl());
    }

    /** Whether the Host's last observation reports a service-owned ZRAM pool. */
    public boolean hasPool(String host) {
        MemoryStateVO state = state(host);
        return state != null && MemoryLifecycleHooks.hasPoolGeneration(state.getState());
    }

    static long retryDelay(int failures) {
        return Math.min(MAX_RETRY_MILLIS, MemoryOptimizationGlobalConfig.firstRetryMillis() << Math.min(failures - 1, 10));
    }

    /** The Host must receive its generation again, for example after it reconnected. */
    public void resend(String host) {
        transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
            if (state == null || state.getDesired() == null) { return null; }
            state.setFailures(0); state.setNextAttemptAt(System.currentTimeMillis());
            if (!CONVERGING.equals(state.getStatus())) { state.setStatus(CONVERGING); state.setReason(null); }
            refreshHostState(em, state);
            return null;
        });
    }

    /**
     * The current delivery of a Host, if it needs one now. A Host waiting for its retry is sent again when
     * it is due, also when a plan that changed nothing asks for a delivery; a new generation, a resend and
     * an operator's request are due at once.
     */
    @SuppressWarnings("unchecked")
    public Delivery pending(String host) {
        return transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, host);
            if (state == null || state.getDesired() == null || !CONVERGING.equals(state.getStatus())) { return null; }
            if (state.getNextAttemptAt() != null && state.getNextAttemptAt() > System.currentTimeMillis()) { return null; }
            return new Delivery(host, state.getDesiredRevision(), JSONObjectUtil.toObject(state.getDesired(), LinkedHashMap.class));
        });
    }

    public List<String> dueHosts(long now) {
        return transaction(em -> Q.New(MemoryStateVO.class).select(MemoryStateVO_.hostUuid).eq(MemoryStateVO_.status, CONVERGING)
                .lte(MemoryStateVO_.nextAttemptAt, now).orderBy(MemoryStateVO_.nextAttemptAt, SimpleQuery.Od.ASC).listValues());
    }

    /** The outcome of one delivery: Succeeded, Converging (resend later) or Failed. */
    public static final class Outcome {
        public final String status;
        public final String reason;
        public final long generation;
        Outcome(String status, String reason, long generation) {
            this.status = status; this.reason = reason; this.generation = generation;
        }
    }

    /**
     * Records an Agent apply result for a generation. A result for an older
     * generation, or one from a boot the Host has left, is ignored; failures
     * back off and become Failed after five.
     */
    public Outcome record(String host, long generation, MemoryAgentResponse response) {
        List<MemoryTaskFailureEvent> events = new ArrayList<>();
        List<Supplier<List<HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData>>> serviceEvents = new ArrayList<>();
        Outcome outcome = transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
            if (state == null || state.getDesiredRevision() != generation) { return null; }
            // A reply delivered after the Host rebooted says nothing of the new boot; the generation is sent again.
            if (fromEarlierBoot(state, response)) { return null; }
            long now = System.currentTimeMillis();
            // An apply reply is a sample too: a service the apply started again is reported like an observed one.
            serviceEvents.add(observe(state, response, now));
            String status = response == null ? null : response.status;
            String reason = response == null ? "AGENT_UNREACHABLE"
                    : response.reasonCode != null ? response.reasonCode : response.reason;
            if (SUCCEEDED.equals(status) && response.isSuccess()) {
                state.setAppliedRevision(generation); state.setStatus(SUCCEEDED); state.setReason(null);
                state.setFailures(0); state.setNextAttemptAt(null);
                finishTasks(em, host, generation, SUCCEEDED, null);
            } else if (response != null && "STALE_GENERATION".equals(response.reasonCode)
                    && hostGeneration(response) != null && hostGeneration(response) >= generation) {
                // The Host took a later generation before this record existed, e.g. it was re-added;
                // continue numbering above it so the Host accepts the current configuration.
                state.setDesiredRevision(hostGeneration(response) + 1);
                state.setStatus(CONVERGING); state.setReason(null); state.setFailures(0);
                state.setNextAttemptAt(now);
            } else if (CONVERGING.equals(status) && response.isSuccess()) {
                long retry = response.retryAfterSeconds == null ? 5 : Math.max(1, response.retryAfterSeconds);
                state.setStatus(CONVERGING); state.setReason(trim(reason));
                state.setNextAttemptAt(now + TimeUnit.SECONDS.toMillis(retry));
            } else {
                int failures = state.getFailures() + 1;
                state.setFailures(failures); state.setReason(trim(reason));
                if (failures >= FAILURES_BEFORE_FAILED) {
                    state.setStatus(FAILED); state.setNextAttemptAt(null);
                    events.addAll(finishTasks(em, host, generation, FAILED, trim(reason)));
                } else {
                    state.setStatus(CONVERGING);
                    state.setNextAttemptAt(now + retryDelay(failures));
                }
            }
            refreshHostState(em, state);
            return new Outcome(state.getStatus(), state.getReason(), generation);
        });
        fire(serviceEvents);
        for (MemoryTaskFailureEvent event : events) { evtf.fire(MemoryTaskFailureEvent.PATH, event); }
        return outcome;
    }

    private List<MemoryTaskFailureEvent> finishTasks(EntityManager em, String host, long generation, String status, String reason) {
        List<MemoryTaskVO> tasks = Q.New(MemoryTaskVO.class).eq(MemoryTaskVO_.hostUuid, host)
                .lte(MemoryTaskVO_.desiredRevision, generation).eq(MemoryTaskVO_.status, CONVERGING).list();
        List<MemoryTaskFailureEvent> events = new ArrayList<>();
        for (MemoryTaskVO task : tasks) {
            task.setStatus(status); task.setReason(reason);
            if (FAILED.equals(status) && task.getDesiredRevision() == generation) {
                events.add(MemoryTaskFailureEvent.fromTask(task));
            }
        }
        return events;
    }

    /** The latest generation the Host took, applied or only accepted, or null when it names none. */
    static Long hostGeneration(MemoryAgentResponse response) {
        Long applied = response.appliedGeneration, accepted = response.acceptedGeneration;
        if (applied == null) { return accepted; }
        return accepted == null ? applied : Math.max(applied, accepted);
    }

    /** Stores a read-only Agent observation; a newer generation on the Host triggers a resend. */
    public void observe(String host, MemoryAgentResponse response) {
        List<Supplier<List<HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData>>> serviceEvents = new ArrayList<>();
        transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
            if (state == null) { return null; }
            serviceEvents.add(observe(state, response, System.currentTimeMillis()));
            followPreparedBackend(em, host, response);
            Object applied = response == null || response.state == null ? null : response.state.get("appliedGeneration");
            if (SUCCEEDED.equals(state.getStatus()) && response != null && response.isSuccess() && response.state != null
                    && (!(applied instanceof Number && ((Number) applied).longValue() == state.getDesiredRevision())
                    || drifted(desired(state), response.state))) {
                // The Host no longer reports the delivered generation (e.g. it was reinstalled)
                // or no longer matches it (e.g. its service stopped): send it again.
                state.setStatus(CONVERGING); state.setReason("HOST_DRIFTED"); state.setFailures(0);
                state.setNextAttemptAt(System.currentTimeMillis());
            }
            refreshHostState(em, state);
            return null;
        });
        fire(serviceEvents);
    }

    /**
     * Stores the sample under the Host's row lock. The service changes from the sample it replaces are worked
     * out after the commit, so a failure there or in sending them reaches the caller and keeps the sample.
     */
    private static Supplier<List<HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData>> observe(
            MemoryStateVO state, MemoryAgentResponse response, long now) {
        // Only a sample carries its time; a reply whose observation failed (only a reason) keeps the last sample.
        Object sampled = response == null || response.state == null ? null : response.state.get("sampleTime");
        if (!(sampled instanceof Number)) { return Collections::emptyList; }
        // A sample older than the stored one, e.g. a reply delivered late, does not replace it.
        if (state.getLastSampleTime() != null && ((Number) sampled).longValue() < state.getLastSampleTime()) {
            return Collections::emptyList;
        }
        String previous = state.getState();
        state.setState(GSON.toJson(response.state));
        state.setLastSampleTime(((Number) sampled).longValue());
        Object capabilities = response.state.get("capabilities");
        if (capabilities instanceof Map) { state.setCapabilities(GSON.toJson(capabilities)); }
        return () -> MemoryObservationRules.serviceEvents(state.getHostUuid(), response, state, previous, now);
    }

    /** A sample older than the stored one and not of its boot: the Host rebooted after it was taken. */
    @SuppressWarnings("unchecked")
    private static boolean fromEarlierBoot(MemoryStateVO state, MemoryAgentResponse response) {
        Object sampled = response == null || response.state == null ? null : response.state.get("sampleTime");
        if (!(sampled instanceof Number) || state.getLastSampleTime() == null
                || ((Number) sampled).longValue() >= state.getLastSampleTime()) { return false; }
        Map<String, Object> stored = state.getState() == null ? null : MemoryObservationRules.readJson(state.getState(),
                LinkedHashMap.class, String.format("Host[%s] stored memory observation is unreadable", state.getHostUuid()));
        return stored == null || !Objects.equals(stored.get("bootId"), response.state.get("bootId"));
    }

    private void fire(List<Supplier<List<HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData>>> serviceEvents) {
        for (Supplier<List<HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData>> changes : serviceEvents) {
            for (HostCanonicalEvents.HostMemoryOptimizationServiceStateChangedData event : changes.get()) {
                evtf.fire(HostCanonicalEvents.HOST_MEMORY_OPTIMIZATION_SERVICE_STATE_CHANGED_PATH, event);
            }
        }
    }

    // ------------------------------------------------------ public state

    public List<String> hostsWithState() {
        return transaction(em -> Q.New(MemoryStateVO.class).select(MemoryStateVO_.hostUuid).listValues());
    }

    /**
     * Recompute the public state from freshness; observation writes refresh it as they happen.
     * Returns the Hosts whose stored configuration cannot be read; the caller plans them again.
     */
    public List<String> refreshHostStates(Collection<String> hosts) {
        List<String> all = new ArrayList<>(new LinkedHashSet<>(hosts));
        List<String> unreadable = new ArrayList<>();
        for (int from = 0; from < all.size(); from += REFRESH_BATCH) {
            List<String> batch = all.subList(from, Math.min(all.size(), from + REFRESH_BATCH));
            unreadable.addAll(transaction(em -> refreshHostStates(em, batch)));
        }
        return unreadable;
    }

    private List<String> refreshHostStates(EntityManager em, List<String> hosts) {
        Map<String, HostMemoryStateVO> rows = new HashMap<>();
        for (HostMemoryStateVO row : Q.New(HostMemoryStateVO.class).in(HostMemoryStateVO_.hostUuid, hosts).<HostMemoryStateVO>list()) {
            rows.put(row.getHostUuid(), row);
        }
        List<String> unreadable = new ArrayList<>();
        for (MemoryStateVO state : Q.New(MemoryStateVO.class).in(MemoryStateVO_.hostUuid, hosts).<MemoryStateVO>list()) {
            if (!refreshHostState(em, state, rows.get(state.getHostUuid()))) { unreadable.add(state.getHostUuid()); }
        }
        return unreadable;
    }

    private void refreshHostState(EntityManager em, MemoryStateVO state) {
        refreshHostState(em, state, em.find(HostMemoryStateVO.class, state.getHostUuid()));
    }

    /** Returns false when the stored configuration cannot be read. */
    @SuppressWarnings("unchecked")
    boolean refreshHostState(EntityManager em, MemoryStateVO state, HostMemoryStateVO existing) {
        HostMemoryStateVO row = existing;
        boolean created = row == null;
        if (created) { row = new HostMemoryStateVO(); row.setHostUuid(state.getHostUuid()); }
        Map<String, Object> observed = Collections.emptyMap();
        boolean observationUnreadable = false;
        boolean fresh = state.getLastSampleTime() != null
                && System.currentTimeMillis() - state.getLastSampleTime() < MemoryOptimizationGlobalConfig.displayTtlMillis();
        if (fresh && state.getState() != null) {
            Map<String, Object> read = MemoryObservationRules.readJson(state.getState(), LinkedHashMap.class, String.format(
                    "Host[%s] stored memory observation is unreadable; it is shown as Unknown", state.getHostUuid()));
            observationUnreadable = read == null;
            observed = observationUnreadable ? Collections.emptyMap() : read;
        }
        Map<String, Object> desired = storedDesired(state);
        boolean zramWanted = desired != null && Boolean.TRUE.equals(field(desired.get("zram"), "enabled"));
        // An unreadable configuration is planned again; until then the Host is not shown as Applied.
        row.setStatus(desired == null ? CONVERGING : publicStatus(state));
        String reason = desired == null ? CONFIGURATION_UNREADABLE : state.getReason();
        if (reason == null && observationUnreadable) { reason = OBSERVATION_UNREADABLE; }
        if (reason == null && zramWanted && !zramSupported(observed)) {
            // The Host leaves a ZRAM it cannot run unmanaged; say why it is not running.
            Object code = field(observed.get("capabilities"), "zramReasonCode");
            reason = code == null || "".equals(code) ? "ZRAM_UNSUPPORTED" : "ZRAM_UNSUPPORTED: " + code;
        }
        row.setReason(trim(reason));
        row.setFeatureState(featureState(observed, zramWanted && zramSupported(observed) ? ZramExpectation.RUNNING : ZramExpectation.NONE));
        row.setAllowedActions(JSONObjectUtil.toJsonString(allowedActions(state.getControl(), state.getStatus(), observed)));
        row.setLastObservedAt(state.getLastSampleTime());
        if (created) { em.persist(row); }
        return desired != null;
    }

    /** Applied, Applying (sent, no answer yet), Converging (waiting or retrying) or Failed. */
    static String publicStatus(MemoryStateVO state) {
        if (SUCCEEDED.equals(state.getStatus())) { return "Applied"; }
        if (FAILED.equals(state.getStatus())) { return FAILED; }
        return state.getReason() == null ? "Applying" : CONVERGING;
    }

    private static Map<String, Object> desired(MemoryStateVO state) {
        Map<String, Object> desired = storedDesired(state);
        return desired == null ? Collections.emptyMap() : desired;
    }

    /** The stored desired configuration, empty before the first plan, or null when it cannot be read. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> storedDesired(MemoryStateVO state) {
        if (state.getDesired() == null) { return Collections.emptyMap(); }
        return MemoryObservationRules.readJson(state.getDesired(), LinkedHashMap.class, String.format(
                "Host[%s] stored memory configuration is unreadable; it is planned again", state.getHostUuid()));
    }

    /** A Host that reports it cannot run ZRAM runs without it; the Agent leaves it unmanaged. */
    static boolean zramSupported(Map<String, Object> observed) {
        Object capabilities = observed.get("capabilities");
        // A stopped service cannot report its qualification; only a known verdict makes ZRAM unsupported.
        return !Boolean.FALSE.equals(field(capabilities, "zram"))
                || "SERVICE_RECLAIM_CAPABILITY_UNKNOWN".equals(field(capabilities, "zramReasonCode"));
    }

    /** The observed Host contradicts its applied desired configuration. */
    static boolean drifted(Map<String, Object> desired, Map<String, Object> observed) {
        Object control = desired.get("control"), observedControl = observed.get("control");
        if ("drained".equals(control)) { return "active".equals(observedControl) || "paused".equals(observedControl); }
        if ("paused".equals(control)) { return "active".equals(observedControl); }
        if (!"active".equals(control)) { return false; }
        // A service back on its packaged observe-only config (e.g. after a reinstall) or with reclaim
        // turned off on the Host no longer reclaims.
        if (Boolean.TRUE.equals(field(desired.get("zram"), "enabled")) && zramSupported(observed)
                && (Arrays.asList("paused", "drained", "stopped").contains(observedControl)
                || !"active".equals(field(observed.get("zram"), "serviceState"))
                || "observe".equals(field(observed.get("zram"), "mode"))
                || Boolean.FALSE.equals(field(observed.get("zram"), "efficiencyEnabled")))) { return true; }
        // ZRAM turned off must also stop reclaiming.
        if (Boolean.FALSE.equals(field(desired.get("zram"), "enabled"))
                && Boolean.TRUE.equals(field(observed.get("zram"), "efficiencyEnabled"))) { return true; }
        Object ksm = field(desired.get("ksm"), "enabled"), observedKsm = field(observed.get("ksm"), "enabled");
        // A wanted KSM whose ksm or ksmtuned stopped is started again, like a stopped ZRAM service.
        if (Boolean.TRUE.equals(ksm) && !Boolean.FALSE.equals(field(observed.get("capabilities"), "ksm"))
                && observed.get("ksm") instanceof Map && !Boolean.TRUE.equals(observedKsm)) { return true; }
        return ksm instanceof Boolean && observedKsm instanceof Boolean && !ksm.equals(observedKsm);
    }

    static String featureState(Map<String, Object> observed, ZramExpectation zram) {
        if (observed.isEmpty()) { return "Unknown"; }
        Map<?, ?> capabilities = map(observed.get("capabilities"));
        if (capabilities != null && Boolean.FALSE.equals(capabilities.get("supported"))) { return "Unsupported"; }
        Object control = observed.get("control");
        if ("drained".equals(control)) { return "Drained"; }
        if ("paused".equals(control)) { return "Paused"; }
        // A wanted ZRAM whose service is not running cannot be observed.
        if (zram == ZramExpectation.RUNNING && !"active".equals(field(observed.get("zram"), "serviceState"))) { return "Unknown"; }
        Object ksm = field(observed.get("ksm"), "enabled");
        Object zramEnabled = field(observed.get("zram"), "efficiencyEnabled");
        if (Boolean.TRUE.equals(ksm) || Boolean.TRUE.equals(zramEnabled)) { return "Active"; }
        if (Boolean.FALSE.equals(ksm) && !Boolean.TRUE.equals(zramEnabled)) { return "Disabled"; }
        return "Unknown";
    }

    static List<String> allowedActions(String control, String status, Map<String, Object> observed) {
        List<String> actions = new ArrayList<>();
        actions.add("query");
        Object observedControl = observed.get("control");
        // Resume also retries a Host that gave up or is still observed paused or drained.
        if (!"active".equals(control) || FAILED.equals(status) || "paused".equals(observedControl) || "drained".equals(observedControl)) {
            actions.add("resume");
        }
        if (!"active".equals(control)) { return actions; }
        if ("active".equals(observedControl) && !"Disabled".equals(featureState(observed, ZramExpectation.NONE))) { actions.add("pause"); }
        if (field(observed.get("zram"), "poolGeneration") != null) { actions.add("drain"); }
        return actions;
    }

    private static Map<?, ?> map(Object value) { return value instanceof Map ? (Map<?, ?>) value : null; }
    private static Object field(Object value, String key) { Map<?, ?> map = map(value); return map == null ? null : map.get(key); }

    /**
     * Clears the LongJob markers of Hosts this node has just taken over, except those of jobs running
     * here: a job of the node that managed them before cannot finish here, and a resumed job sets its marker again.
     */
    public void clearActiveJobs(Collection<String> hosts, Collection<String> runningJobs) {
        if (hosts.isEmpty()) { return; }
        UpdateQuery update = SQL.New(HostMemoryStateVO.class).in(HostMemoryStateVO_.hostUuid, hosts).notNull(HostMemoryStateVO_.activeJobUuid);
        if (!runningJobs.isEmpty()) { update.notIn(HostMemoryStateVO_.activeJobUuid, runningJobs); }
        update.set(HostMemoryStateVO_.activeJobUuid, null).update();
    }

    /** Records the LongJob operating on a Host; clearing applies only to the job that set it. */
    public void setActiveJob(String host, String job, String previous) {
        transaction(em -> {
            HostMemoryStateVO row = em.find(HostMemoryStateVO.class, host);
            if (row == null) {
                if (job == null) { return null; }
                row = new HostMemoryStateVO(); row.setHostUuid(host);
                row.setStatus("Applying"); row.setFeatureState("Unknown"); row.setActiveJobUuid(job);
                row.setAllowedActions(JSONObjectUtil.toJsonString(Collections.singletonList("query")));
                em.persist(row);
                return null;
            }
            if (job != null || Objects.equals(previous, row.getActiveJobUuid())) { row.setActiveJobUuid(job); }
            return null;
        });
    }

    /** Binds the backend prepared by the Agent to the Host's standard writeback settings. */
    public void recordPreparedBackend(String host, String resourceUuid, long capacityBytes) {
        transaction(em -> {
            recordPreparedBackend(em, host, resourceUuid, capacityBytes);
            return null;
        });
    }

    private void recordPreparedBackend(EntityManager em, String host, String resourceUuid, long capacityBytes) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE.path(), resourceUuid);
        values.put(MemoryStandardField.WRITEBACK_BACKEND_CAPACITY.path(), capacityBytes);
        standardConfigs.writeOverrides(em, host, HostVO.class.getSimpleName(), values);
    }

    /**
     * A reboot prepares the recorded disk again, and the service names the new
     * preparation with a new resource UUID. Follow it once that disk is the
     * active backend with the recorded capacity.
     */
    private void followPreparedBackend(EntityManager em, String host, MemoryAgentResponse response) {
        Object writeback = response == null || response.state == null ? null : response.state.get("writeback");
        Object backend = writeback instanceof Map ? ((Map<?, ?>) writeback).get("backend") : null;
        if (!(backend instanceof Map)) { return; }
        Map<?, ?> observed = (Map<?, ?>) backend;
        Object resource = observed.get("resource_uuid");
        Object recorded = standardConfigs.readOverrideOwn(em, MemoryStandardField.WRITEBACK_BACKEND_RESOURCE.path(), host);
        Object recordedCapacity = standardConfigs.readOverrideOwn(em, MemoryStandardField.WRITEBACK_BACKEND_CAPACITY.path(), host);
        if (!"ACTIVE".equals(observed.get("state")) || !Boolean.TRUE.equals(observed.get("active"))
                || !(resource instanceof String) || !(recorded instanceof String) || resource.equals(recorded)
                || !(recordedCapacity instanceof Long)) { return; }
        Long capacity = MemoryObservationRules.exactLong(observed.get("capacity_bytes"));
        if (capacity == null || !MemoryStandardField.isBackendResource((String) resource)) {
            logger.warn(String.format("Host[%s] reported an invalid writeback backend %s", host, observed));
            return;
        }
        if (!recordedCapacity.equals(capacity)) { return; }
        recordPreparedBackend(em, host, (String) resource, (Long) recordedCapacity);
        logger.info(String.format("Host[%s] writeback backend %s is now %s", host, recorded, resource));
    }

    // ----------------------------------------------------- migration holds

    /** A VM's state and Host; absent when the VM no longer exists. */
    static final class VmPlacement {
        final VmInstanceState state;
        final String hostUuid;
        VmPlacement(VmInstanceState state, String hostUuid) { this.state = state; this.hostUuid = hostUuid; }
    }

    public VmPlacement vmPlacement(String vmUuid) {
        return transaction(em -> Q.New(VmInstanceVO.class).select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid).listTuple().stream()
                .map(t -> new VmPlacement(t.get(0, VmInstanceState.class), t.get(1, String.class)))
                .findFirst().orElse(null));
    }

    public boolean hostExists(String host) {
        return transaction(em -> Q.New(HostVO.class).eq(HostVO_.uuid, host).isExists());
    }

    /** The Unknown holds of a source, and the unreleased holds of VMs that no longer exist. */
    public List<MemoryMigrationVO> unknownOrOrphanedHolds(String sourceHost) {
        return transaction(em -> SQL.New("from MemoryMigrationVO m where m.sourceHostUuid = :host and (m.status = :unknown"
                + " or m.status <> :released and not exists (select v.uuid from VmInstanceVO v where v.uuid = m.vmUuid))",
                MemoryMigrationVO.class).param("host", sourceHost).param("unknown", "Unknown").param("released", RELEASED).list());
    }

    /** The Preparing or Held holds of a source whose VM is not migrating. */
    public List<MemoryMigrationVO> settledHolds(String sourceHost) {
        return transaction(em -> SQL.New("from MemoryMigrationVO m where m.sourceHostUuid = :host and m.status in (:held)"
                + " and exists (select v.uuid from VmInstanceVO v where v.uuid = m.vmUuid and v.state <> :migrating)",
                MemoryMigrationVO.class).param("host", sourceHost).param("held", Arrays.asList("Preparing", "Held"))
                .param("migrating", VmInstanceState.Migrating).list());
    }

    // ------------------------------------------------------------ cleanup

    /** Removes memory records of deleted Hosts and VMs that hold no unreleased migration. */
    public void cleanupDeletedResources() {
        transaction(em -> {
            SQL.New("delete from MemoryMigrationVO m where m.status = :released"
                    + " and not exists (select v.uuid from VmInstanceVO v where v.uuid = m.vmUuid)").param("released", RELEASED).execute();
            String deletedHost = "not exists (select h.uuid from HostVO h where h.uuid = %s)";
            String unreleased = " and not exists (select m.vmUuid from MemoryMigrationVO m where m.status <> :released"
                    + " and (m.sourceHostUuid = s.hostUuid or m.targetHostUuid = s.hostUuid))";
            SQL.New("delete from MemoryTaskVO t where " + String.format(deletedHost, "t.hostUuid")).execute();
            SQL.New("delete from HostMemoryStateVO s where " + String.format(deletedHost, "s.hostUuid")).execute();
            SQL.New("delete from MemoryStateVO s where " + String.format(deletedHost, "s.hostUuid") + unreleased)
                    .param("released", RELEASED).execute();
            return null;
        });
    }

    /** Finished operation records are kept for a week. */
    public void purgeHistory(long now) {
        // Only finished operations: a running or converging one still waits for its result.
        transaction(em -> SQL.New("delete from MemoryTaskVO t where t.status in (:finished) and t.lastOpDate < :before")
                .param("finished", Arrays.asList(SUCCEEDED, FAILED, SUPERSEDED))
                .param("before", new java.sql.Timestamp(now - MemoryOptimizationGlobalConfig.historyRetentionMillis())).execute());
    }

    static String trim(String value) {
        return value == null || value.length() <= 2048 ? value : value.substring(0, 2048);
    }
}
