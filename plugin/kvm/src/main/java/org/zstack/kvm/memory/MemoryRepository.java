package org.zstack.kvm.memory;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Each Host has one desired configuration: the effective standard configs and
 * the operator's lifecycle control. A change increments its generation; the
 * owning management node resends that generation until the Agent applies it.
 */
public class MemoryRepository {
    private static final CLogger logger = Utils.getLogger(MemoryRepository.class);
    static final String SUCCEEDED = "Succeeded";
    static final String CONVERGING = "Converging";
    static final String FAILED = "Failed";
    static final int FAILURES_BEFORE_FAILED = 5;
    static final long FIRST_RETRY_MILLIS = TimeUnit.SECONDS.toMillis(30);
    static final long MAX_RETRY_MILLIS = TimeUnit.MINUTES.toMillis(10);
    static final long HISTORY_MILLIS = TimeUnit.DAYS.toMillis(7);

    @Autowired private DatabaseFacade dbf;
    @Autowired private EventFacade evtf;
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
        if ("Global".equals(scope)) {
            if (!"global".equals(resource)) { throw error("MEMORY_INVALID_SCOPE", "Global resource must be global"); }
        } else if ("Cluster".equals(scope)) {
            List<String> types = em.createQuery("select c.hypervisorType from ClusterVO c where c.uuid = :uuid", String.class)
                    .setParameter("uuid", resource).getResultList();
            if (types.isEmpty() || !"KVM".equals(types.get(0))) {
                throw error("MEMORY_INVALID_SCOPE", "Expected an existing KVM Cluster");
            }
        } else if ("Host".equals(scope)) {
            List<String> types = em.createQuery("select h.hypervisorType from HostVO h where h.uuid = :uuid", String.class)
                    .setParameter("uuid", resource).getResultList();
            if (types.isEmpty() || !"KVM".equals(types.get(0))) {
                throw error("MEMORY_INVALID_SCOPE", "Expected an existing KVM Host");
            }
        } else if ("VM".equals(scope)) {
            if (em.createQuery("select v.uuid from VmInstanceVO v where v.uuid = :uuid", String.class)
                    .setParameter("uuid", resource).getResultList().isEmpty()) {
                throw error("MEMORY_INVALID_SCOPE", "VM does not exist");
            }
        } else { throw error("MEMORY_INVALID_SCOPE", "Unknown memory scope"); }
    }

    private static boolean nonKvm(EntityManager em, String scope, String resource) {
        String entity = "Host".equals(scope) ? "HostVO h" : "Cluster".equals(scope) ? "ClusterVO h" : null;
        if (entity == null) { return false; }
        List<String> types = em.createQuery("select h.hypervisorType from " + entity + " where h.uuid = :uuid", String.class)
                .setParameter("uuid", resource).getResultList();
        return !types.isEmpty() && !"KVM".equals(types.get(0));
    }

    private List<String> targets(EntityManager em, String scope, String resource) {
        validateResource(em, scope, resource);
        if ("Global".equals(scope)) {
            return em.createQuery("select h.uuid from HostVO h where h.hypervisorType = :type order by h.uuid", String.class)
                    .setParameter("type", "KVM").getResultList();
        }
        if ("Host".equals(scope)) { return Collections.singletonList(resource); }
        if ("Cluster".equals(scope)) {
            return em.createQuery("select h.uuid from HostVO h where h.clusterUuid = :cluster " +
                    "and h.hypervisorType = :type order by h.uuid", String.class)
                    .setParameter("cluster", resource).setParameter("type", "KVM").getResultList();
        }
        String host = em.createQuery("select v.hostUuid from VmInstanceVO v where v.uuid = :uuid", String.class)
                .setParameter("uuid", resource).getSingleResult();
        return host == null ? Collections.emptyList() : Collections.singletonList(host);
    }

    public List<String> targets(String scope, String resource) {
        return transaction(em -> targets(em, scope, resource));
    }

    public List<String> connectedHosts() {
        return transaction(em -> em.createQuery("select h.uuid from HostVO h where h.hypervisorType = :type"
                        + " and h.status = :status order by h.uuid", String.class)
                .setParameter("type", "KVM").setParameter("status", HostStatus.Connected).getResultList());
    }

    /** Current VM placement snapshot used by read-only batched observations. */
    public Map<String, String> currentVmHosts(Collection<String> vmUuids) {
        if (vmUuids == null || vmUuids.isEmpty()) { return Collections.emptyMap(); }
        return transaction(em -> {
            List<Object[]> rows = em.createQuery(
                    "select v.uuid, v.hostUuid from VmInstanceVO v where v.uuid in (:uuids)", Object[].class)
                    .setParameter("uuids", new ArrayList<>(new LinkedHashSet<>(vmUuids))).getResultList();
            Map<String, String> result = new LinkedHashMap<>();
            for (Object[] row : rows) {
                if (row[0] instanceof String && row[1] instanceof String) { result.put((String) row[0], (String) row[1]); }
            }
            return result;
        });
    }

    public List<String> summaryTargets(List<String> requested, String zoneUuid) {
        if (requested != null && requested.isEmpty()) { return Collections.emptyList(); }
        return transaction(em -> {
            List<String> hosts;
            if (requested == null) {
                hosts = targets(em, "Global", "global");
            } else {
                Set<String> selected = new LinkedHashSet<>(requested);
                hosts = em.createQuery("select h.uuid from HostVO h where h.hypervisorType = 'KVM' "
                        + "and h.uuid in (:hosts) order by h.uuid", String.class)
                        .setParameter("hosts", selected).getResultList();
                // Recheck KVM eligibility and a resource deleted since API admission.
                if (hosts.size() != selected.size()) {
                    throw error("MEMORY_INVALID_SCOPE", "Expected an existing KVM Host");
                }
            }
            if (zoneUuid == null || hosts.isEmpty()) { return hosts; }
            return em.createQuery("select h.uuid from HostVO h where h.zoneUuid = :zone "
                    + "and h.uuid in (:hosts) order by h.uuid", String.class)
                    .setParameter("zone", zoneUuid).setParameter("hosts", hosts).getResultList();
        });
    }

    public List<MemoryStateVO> allStates(List<String> hosts) {
        if (hosts == null || hosts.isEmpty()) { return Collections.emptyList(); }
        return transaction(em -> em.createQuery("select s from MemoryStateVO s where s.hostUuid in (:hosts)", MemoryStateVO.class)
                .setParameter("hosts", hosts).getResultList());
    }

    public MemoryStateVO state(String host) {
        return transaction(em -> em.find(MemoryStateVO.class, host));
    }

    // ------------------------------------------------------- standard config

    /** Standard memory configuration is stored by GlobalConfig (no resource) or a Host/Cluster ResourceConfig. */
    private String standardScope(EntityManager em, String resourceUuid) {
        if (resourceUuid == null) { return "Global"; }
        // Only the registry discriminator is needed; materializing JOINED
        // ResourceVO exceeds MariaDB's join limit in the full product.
        String resourceType = em.createQuery("select r.resourceType from ResourceVO r where r.uuid = :uuid", String.class)
                .setParameter("uuid", resourceUuid).setMaxResults(1).getResultList().stream()
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (HostVO.class.getSimpleName().equals(resourceType)) { return "Host"; }
        if (org.zstack.header.cluster.ClusterVO.class.getSimpleName().equals(resourceType)) { return "Cluster"; }
        throw error("MEMORY_INVALID_SCOPE", "Memory configuration requires a KVM Host or Cluster");
    }

    private static MemoryStandardField.Scope scopeOf(String scope) {
        return "Global".equals(scope) ? MemoryStandardField.Scope.GLOBAL
                : "Cluster".equals(scope) ? MemoryStandardField.Scope.CLUSTER : MemoryStandardField.Scope.HOST;
    }

    /** Scope check of one standard value before GlobalConfig/ResourceConfig persists it. */
    public void validateStandardChange(MemoryStandardField field, String resourceUuid, String newValue) {
        transaction(em -> {
            String scope = standardScope(em, resourceUuid);
            if (!field.allows(scopeOf(scope))) {
                throw error("MEMORY_INVALID_SCOPE", field.path() + " does not support " + scope);
            }
            // The legacy host.ksm switch was settable on any Host or Cluster before memory optimization.
            if (field != MemoryStandardField.KSM_ENABLED || !nonKvm(em, scope, resourceUuid)) {
                validateResource(em, scope, resourceUuid == null ? "global" : resourceUuid);
            }
            return null;
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

    /** The most specific stored value wins; none leaves the setting unmanaged. */
    private Object effectiveValue(EntityManager em, MemoryStandardField field, String host, String cluster) {
        Object value = field.allows(MemoryStandardField.Scope.HOST) ? standardConfigs.readOverrideOwn(em, field.path(), host) : null;
        if (value == null && cluster != null && field.allows(MemoryStandardField.Scope.CLUSTER)) {
            value = standardConfigs.readOverrideOwn(em, field.path(), cluster);
        }
        if (value == null && field.allows(MemoryStandardField.Scope.GLOBAL)) {
            value = standardConfigs.readGlobalOwn(em, field.path());
        }
        return MemoryStandardConfigAdapter.isUnmanaged(value) ? null : value;
    }

    Map<String, Object> desired(EntityManager em, String host, String control, boolean licensed) {
        String cluster = em.createQuery("select h.clusterUuid from HostVO h where h.uuid = :uuid", String.class)
                .setParameter("uuid", host).getResultList().stream().findFirst().orElse(null);
        Map<String, Object> ksm = new LinkedHashMap<>();
        ksm.put("enabled", effectiveValue(em, MemoryStandardField.KSM_ENABLED, host, cluster));
        Map<String, Object> zram = new LinkedHashMap<>();
        Map<String, Object> writeback = new LinkedHashMap<>();
        // The legacy host.ksm switch stays license-free; memory optimization does not.
        if (licensed) {
            ksm.put("zeroPagesEnabled", effectiveValue(em, MemoryStandardField.KSM_ZERO_PAGES, host, cluster));
            zram.put("enabled", effectiveValue(em, MemoryStandardField.ZRAM_ENABLED, host, cluster));
            zram.put("logicalCapacityBytes", effectiveValue(em, MemoryStandardField.ZRAM_LOGICAL_CAPACITY, host, cluster));
            zram.put("ramLimitBytes", effectiveValue(em, MemoryStandardField.ZRAM_RAM_LIMIT, host, cluster));
            Object enabled = effectiveValue(em, MemoryStandardField.WRITEBACK_ENABLED, host, cluster);
            // Writeback runs inside ZRAM on the Host's prepared backend; without either it stays off.
            boolean prepared = effectiveValue(em, MemoryStandardField.WRITEBACK_BACKEND_RESOURCE, host, cluster) != null;
            boolean zramOn = Boolean.TRUE.equals(zram.get("enabled"));
            writeback.put("enabled", Boolean.TRUE.equals(enabled) && !(prepared && zramOn) ? Boolean.FALSE : enabled);
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

    private MemoryStateVO lockState(EntityManager em, String host) {
        MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
        if (state == null) {
            state = new MemoryStateVO(); state.setHostUuid(host); state.setStatus(CONVERGING);
            state.setControl("active");
            em.persist(state);
        }
        return state;
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
    public Planned plan(String host, String control, String action, String actor, boolean licensed, boolean operation) {
        return transaction(em -> {
            validateResource(em, "Host", host);
            MemoryStateVO state = lockState(em, host);
            if (control != null) { state.setControl(control); }
            String desired = GSON.toJson(desired(em, host, state.getControl(), licensed));
            boolean changed = !desired.equals(state.getDesired());
            if (changed) {
                long generation = state.getDesiredRevision() + 1;
                state.setDesiredRevision(generation); state.setDesired(desired);
                state.setStatus(CONVERGING); state.setReason(null); state.setFailures(0);
                state.setNextAttemptAt(System.currentTimeMillis());
                finishTasks(em, host, generation - 1, "Superseded", "Superseded by generation " + generation);
            } else if (operation && !CONVERGING.equals(state.getStatus())) {
                // An operator asking again resends the current generation, also to a Host that gave up.
                state.setStatus(CONVERGING); state.setFailures(0); state.setNextAttemptAt(System.currentTimeMillis());
            }
            String task = null;
            if (changed || operation) {
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
        return transaction(em -> newTask(em, host, action, actor, generation, "Running"));
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
        return transaction(em -> em.createQuery("select t from MemoryTaskVO t where t.uuid in (:uuids)", MemoryTaskVO.class)
                .setParameter("uuids", uuids).getResultList().stream()
                .collect(java.util.stream.Collectors.toMap(MemoryTaskVO::getUuid, t -> t)));
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
        return Math.min(MAX_RETRY_MILLIS, FIRST_RETRY_MILLIS << Math.min(failures - 1, 10));
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

    /** The current delivery of a Host, if it still needs one. */
    @SuppressWarnings("unchecked")
    public Delivery pending(String host) {
        return transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, host);
            if (state == null || state.getDesired() == null || !CONVERGING.equals(state.getStatus())) { return null; }
            return new Delivery(host, state.getDesiredRevision(), JSONObjectUtil.toObject(state.getDesired(), LinkedHashMap.class));
        });
    }

    public List<String> dueHosts(long now) {
        return transaction(em -> em.createQuery("select s.hostUuid from MemoryStateVO s where s.status = :status"
                        + " and s.nextAttemptAt <= :now order by s.nextAttemptAt", String.class)
                .setParameter("status", CONVERGING).setParameter("now", now).getResultList());
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
     * generation is ignored; failures back off and become Failed after five.
     */
    public Outcome record(String host, long generation, MemoryAgentResponse response) {
        List<MemoryTaskFailureEvent> events = new ArrayList<>();
        Outcome outcome = transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
            if (state == null || state.getDesiredRevision() != generation) { return null; }
            long now = System.currentTimeMillis();
            observe(state, response, now);
            String status = response == null ? null : response.status;
            String reason = response == null ? "AGENT_UNREACHABLE"
                    : response.reasonCode != null ? response.reasonCode : response.reason;
            if (SUCCEEDED.equals(status) && response.isSuccess()) {
                state.setAppliedRevision(generation); state.setStatus(SUCCEEDED); state.setReason(null);
                state.setFailures(0); state.setNextAttemptAt(null);
                finishTasks(em, host, generation, SUCCEEDED, null);
            } else if (response != null && "STALE_GENERATION".equals(response.reasonCode)
                    && response.appliedGeneration != null && response.appliedGeneration >= generation) {
                // The Host applied a later generation before this record existed, e.g. it was re-added;
                // continue numbering above it so the Host accepts the current configuration.
                state.setDesiredRevision(response.appliedGeneration + 1);
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
        for (MemoryTaskFailureEvent event : events) {
            try {
                evtf.fire(MemoryTaskFailureEvent.PATH, event);
            } catch (RuntimeException e) {
                logger.warn(String.format("Unable to fire the memory task[%s] failure event", event.getTaskUuid()), e);
            }
        }
        return outcome;
    }

    private List<MemoryTaskFailureEvent> finishTasks(EntityManager em, String host, long generation, String status, String reason) {
        List<MemoryTaskVO> tasks = em.createQuery("select t from MemoryTaskVO t where t.hostUuid = :host"
                        + " and t.desiredRevision <= :generation and t.status = :running", MemoryTaskVO.class)
                .setParameter("host", host).setParameter("generation", generation).setParameter("running", CONVERGING)
                .getResultList();
        List<MemoryTaskFailureEvent> events = new ArrayList<>();
        for (MemoryTaskVO task : tasks) {
            task.setStatus(status); task.setReason(reason);
            if (FAILED.equals(status) && task.getDesiredRevision() == generation) {
                events.add(MemoryTaskFailureEvent.fromTask(task));
            }
        }
        return events;
    }

    /** Stores a read-only Agent observation; a newer generation on the Host triggers a resend. */
    public void observe(String host, MemoryAgentResponse response) {
        transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
            if (state == null) { return null; }
            observe(state, response, System.currentTimeMillis());
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
    }

    private static void observe(MemoryStateVO state, MemoryAgentResponse response, long now) {
        if (response == null || response.state == null || response.state.isEmpty()) { return; }
        state.setState(GSON.toJson(response.state));
        Object sampled = response.state.get("sampleTime");
        state.setLastSampleTime(sampled instanceof Number ? ((Number) sampled).longValue() : now);
        Object capabilities = response.state.get("capabilities");
        if (capabilities instanceof Map) { state.setCapabilities(GSON.toJson(capabilities)); }
    }

    // ------------------------------------------------------ public state

    public List<String> hostsWithState() {
        return transaction(em -> em.createQuery("select s.hostUuid from MemoryStateVO s", String.class).getResultList());
    }

    /** Recompute the public state from freshness; observation writes refresh it as they happen. */
    public void refreshHostStates(Collection<String> hosts) {
        for (String host : new LinkedHashSet<>(hosts)) {
            transaction(em -> {
                MemoryStateVO state = em.find(MemoryStateVO.class, host);
                if (state != null) { refreshHostState(em, state); }
                return null;
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshHostState(EntityManager em, MemoryStateVO state) {
        HostMemoryStateVO row = em.find(HostMemoryStateVO.class, state.getHostUuid());
        boolean created = row == null;
        if (created) { row = new HostMemoryStateVO(); row.setHostUuid(state.getHostUuid()); }
        Map<String, Object> observed = Collections.emptyMap();
        boolean fresh = state.getLastSampleTime() != null
                && System.currentTimeMillis() - state.getLastSampleTime() < MemoryOptimizationGlobalConfig.displayTtlMillis();
        if (fresh && state.getState() != null) {
            try { observed = JSONObjectUtil.toObject(state.getState(), LinkedHashMap.class); }
            catch (RuntimeException ignored) { observed = Collections.emptyMap(); }
        }
        boolean zramWanted = Boolean.TRUE.equals(field(desired(state).get("zram"), "enabled"));
        row.setStatus(publicStatus(state));
        String reason = state.getReason();
        if (reason == null && zramWanted && !zramSupported(observed)) {
            // The Host leaves a ZRAM it cannot run unmanaged; say why it is not running.
            Object code = field(observed.get("capabilities"), "zramReasonCode");
            reason = code == null || "".equals(code) ? "ZRAM_UNSUPPORTED" : "ZRAM_UNSUPPORTED: " + code;
        }
        row.setReason(trim(reason));
        row.setFeatureState(featureState(observed, zramWanted && zramSupported(observed)));
        row.setCapabilities(state.getCapabilities());
        row.setAllowedActions(JSONObjectUtil.toJsonString(allowedActions(state.getControl(), FAILED.equals(state.getStatus()), observed)));
        row.setLastObservedAt(state.getLastSampleTime());
        if (row.getActiveJobUuid() != null && !longJobUnfinished(em, row.getActiveJobUuid())) { row.setActiveJobUuid(null); }
        if (created) { em.persist(row); }
    }

    /** Applied, Applying (sent, no answer yet), Converging (waiting or retrying) or Failed. */
    static String publicStatus(MemoryStateVO state) {
        if (SUCCEEDED.equals(state.getStatus())) { return "Applied"; }
        if (FAILED.equals(state.getStatus())) { return FAILED; }
        return state.getReason() == null ? "Applying" : CONVERGING;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> desired(MemoryStateVO state) {
        if (state.getDesired() == null) { return Collections.emptyMap(); }
        try { return JSONObjectUtil.toObject(state.getDesired(), LinkedHashMap.class); }
        catch (RuntimeException invalid) { return Collections.emptyMap(); }
    }

    /** A Host that reports it cannot run ZRAM runs without it; the Agent leaves it unmanaged. */
    static boolean zramSupported(Map<String, Object> observed) {
        return !Boolean.FALSE.equals(field(observed.get("capabilities"), "zram"));
    }

    /** The observed Host contradicts its applied desired configuration. */
    static boolean drifted(Map<String, Object> desired, Map<String, Object> observed) {
        Object control = desired.get("control"), observedControl = observed.get("control");
        if ("drained".equals(control)) { return "active".equals(observedControl) || "paused".equals(observedControl); }
        if ("paused".equals(control)) { return "active".equals(observedControl); }
        if (!"active".equals(control)) { return false; }
        if (Boolean.TRUE.equals(field(desired.get("zram"), "enabled")) && zramSupported(observed)
                && (Arrays.asList("paused", "drained", "stopped").contains(observedControl)
                || !"active".equals(field(observed.get("zram"), "serviceState")))) { return true; }
        // ZRAM turned off must also stop reclaiming.
        if (Boolean.FALSE.equals(field(desired.get("zram"), "enabled"))
                && Boolean.TRUE.equals(field(observed.get("zram"), "efficiencyEnabled"))) { return true; }
        Object ksm = field(desired.get("ksm"), "enabled"), observedKsm = field(observed.get("ksm"), "enabled");
        // A wanted KSM whose ksm or ksmtuned stopped is started again, like a stopped ZRAM service.
        if (Boolean.TRUE.equals(ksm) && !Boolean.FALSE.equals(field(observed.get("capabilities"), "ksm"))
                && observed.get("ksm") instanceof Map && !Boolean.TRUE.equals(observedKsm)) { return true; }
        return ksm instanceof Boolean && observedKsm instanceof Boolean && !ksm.equals(observedKsm);
    }

    static String featureState(Map<String, Object> observed, boolean zramWanted) {
        if (observed.isEmpty()) { return "Unknown"; }
        Map<?, ?> capabilities = map(observed.get("capabilities"));
        if (capabilities != null && Boolean.FALSE.equals(capabilities.get("supported"))) { return "Unsupported"; }
        Object control = observed.get("control");
        if ("drained".equals(control)) { return "Drained"; }
        if ("paused".equals(control)) { return "Paused"; }
        // A wanted ZRAM whose service is not running cannot be observed.
        if (zramWanted && !"active".equals(field(observed.get("zram"), "serviceState"))) { return "Unknown"; }
        Object ksm = field(observed.get("ksm"), "enabled");
        Object zram = field(observed.get("zram"), "efficiencyEnabled");
        if (Boolean.TRUE.equals(ksm) || Boolean.TRUE.equals(zram)) { return "Active"; }
        if (Boolean.FALSE.equals(ksm) && !Boolean.TRUE.equals(zram)) { return "Disabled"; }
        return "Unknown";
    }

    static List<String> allowedActions(String control, boolean failed, Map<String, Object> observed) {
        List<String> actions = new ArrayList<>();
        actions.add("query");
        Object observedControl = observed.get("control");
        // Resume also retries a Host that gave up or is still observed paused or drained.
        if (!"active".equals(control) || failed || "paused".equals(observedControl) || "drained".equals(observedControl)) {
            actions.add("resume");
        }
        if (!"active".equals(control)) { return actions; }
        if ("active".equals(observedControl) && !"Disabled".equals(featureState(observed, false))) { actions.add("pause"); }
        if (field(observed.get("zram"), "poolGeneration") != null) { actions.add("drain"); }
        return actions;
    }

    private static Map<?, ?> map(Object value) { return value instanceof Map ? (Map<?, ?>) value : null; }
    private static Object field(Object value, String key) { Map<?, ?> map = map(value); return map == null ? null : map.get(key); }

    private static boolean longJobUnfinished(EntityManager em, String job) {
        List<?> states = em.createQuery("select j.state from LongJobVO j where j.uuid = :uuid")
                .setParameter("uuid", job).getResultList();
        if (states.isEmpty()) { return false; }
        String state = String.valueOf(states.get(0));
        return !"Succeeded".equals(state) && !"Failed".equals(state) && !"Canceled".equals(state);
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
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(MemoryStandardField.WRITEBACK_BACKEND_RESOURCE.path(), resourceUuid);
            values.put(MemoryStandardField.WRITEBACK_BACKEND_CAPACITY.path(), capacityBytes);
            standardConfigs.writeOverrides(em, host, HostVO.class.getSimpleName(), values);
            return null;
        });
    }

    // ------------------------------------------------------------ cleanup

    /** Removes memory records of deleted Hosts and VMs that hold no unreleased migration. */
    public void cleanupDeletedResources() {
        transaction(em -> {
            em.createQuery("delete from MemoryMigrationVO m where m.status = 'Released'"
                    + " and not exists (select v.uuid from VmInstanceVO v where v.uuid = m.vmUuid)").executeUpdate();
            String deletedHost = "not exists (select h.uuid from HostVO h where h.uuid = %s)";
            String unreleased = " and not exists (select m.vmUuid from MemoryMigrationVO m where m.status <> 'Released'"
                    + " and (m.sourceHostUuid = s.hostUuid or m.targetHostUuid = s.hostUuid))";
            em.createQuery("delete from MemoryTaskVO t where " + String.format(deletedHost, "t.hostUuid")).executeUpdate();
            em.createQuery("delete from HostMemoryStateVO s where " + String.format(deletedHost, "s.hostUuid")).executeUpdate();
            em.createQuery("delete from MemoryStateVO s where " + String.format(deletedHost, "s.hostUuid") + unreleased)
                    .executeUpdate();
            return null;
        });
    }

    /** Finished operation records are kept for a week. */
    public void purgeHistory(long now) {
        transaction(em -> em.createQuery("delete from MemoryTaskVO t where t.status <> :running and t.lastOpDate < :before")
                .setParameter("running", CONVERGING).setParameter("before", new java.sql.Timestamp(now - HISTORY_MILLIS))
                .executeUpdate());
    }

    static String trim(String value) {
        return value == null || value.length() <= 2048 ? value : value.substring(0, 2048);
    }
}
