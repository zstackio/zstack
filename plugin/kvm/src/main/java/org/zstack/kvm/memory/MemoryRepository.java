package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigCanonicalEvents;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;
import org.zstack.resourceconfig.ResourceConfigCanonicalEvents;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import com.google.gson.*;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

/** Durable admission/claims. No network request is made while holding a DB lock. */
public class MemoryRepository {
    private static final CLogger logger = Utils.getLogger(MemoryRepository.class);
    private static final Set<String> CONTROL_STATE_FIELDS = new HashSet<>(Arrays.asList(
            "phase", "lifecycle", "ksmOnly", "migrationHold", "controlFenceProof",
            "lastConfirmedOperationUuid", "bootId", "knownBlocked", "blockers", "sections",
            "sectionComplete", "applyEvidence", "recoveryProof", "maintenanceProof"));
    private static final String GLOBAL = "Global:global";
    @Autowired private DatabaseFacade dbf;
    @Autowired private EventFacade evtf;
    @Autowired private PlatformTransactionManager transactionManager;
    private final MemoryStandardConfigAdapter standardConfigs = new MemoryStandardConfigAdapter();
    private static final long FAILURE_NOTIFICATION_LEASE_MS = 120_000L;

    protected <T> T transaction(Function<EntityManager, T> function) {
        return new SQLBatchWithReturn<T>() {
            @Override protected T scripts() { return function.apply(dbf.getEntityManager()); }
        }.execute();
    }

    public void initialize() {
        transaction(em -> {
            em.createNativeQuery("INSERT IGNORE INTO MemoryPolicyVO " +
                    "(uuid, scope, resourceUuid, revision, policy, createDate, lastOpDate) " +
                    "VALUES ('Global:global', 'Global', 'global', 0, '{\"schemaVersion\":1}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .executeUpdate();
            em.createNativeQuery("INSERT IGNORE INTO MemoryQueryVersionVO (name, revision) VALUES ('state', 0), ('task', 0)")
                    .executeUpdate();
            return null;
        });
    }

    private static MemoryStandardField.Scope scopeOf(String scope) {
        if ("Global".equals(scope)) { return MemoryStandardField.Scope.GLOBAL; }
        if ("Cluster".equals(scope)) { return MemoryStandardField.Scope.CLUSTER; }
        if ("Host".equals(scope)) { return MemoryStandardField.Scope.HOST; }
        return null;
    }

    private MemoryPolicyVO lock(EntityManager em) {
        MemoryPolicyVO vo = em.find(MemoryPolicyVO.class, GLOBAL, LockModeType.PESSIMISTIC_WRITE);
        if (vo == null) { throw error("MEMORY_NOT_READY", "Policy store is not initialized"); }
        return vo;
    }

    private long queryVersion(EntityManager em, String name) {
        MemoryQueryVersionVO version = em.find(MemoryQueryVersionVO.class, name);
        if (version == null) { throw error("MEMORY_NOT_READY", "Query version store is not initialized"); }
        return version.getRevision();
    }

    /** Caller owns the global admission lock; mutation and version commit together. */
    private void bumpQueryVersion(EntityManager em, String name) {
        MemoryQueryVersionVO version = em.find(MemoryQueryVersionVO.class, name);
        if (version == null) { throw error("MEMORY_NOT_READY", "Query version store is not initialized"); }
        version.setRevision(Math.addExact(version.getRevision(), 1L));
    }

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
        } else { throw error("MEMORY_INVALID_SCOPE", "Unknown policy scope"); }
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

    public List<String> connectedHosts() {
        return transaction(em -> em.createQuery("select h.uuid from HostVO h where h.hypervisorType = :type"
                        + " and h.status = :status order by h.uuid", String.class)
                .setParameter("type", "KVM").setParameter("status", HostStatus.Connected).getResultList());
    }

    public List<String> targets(String scope, String resource) {
        return transaction(em -> targets(em, scope, resource));
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
                if (row[0] instanceof String && row[1] instanceof String) {
                    result.put((String) row[0], (String) row[1]);
                }
            }
            return result;
        });
    }

    private MemoryPolicyVO policy(EntityManager em, String scope, String resource) {
        MemoryPolicyVO result = em.find(MemoryPolicyVO.class, scope + ":" + resource);
        if (result != null) { return result; }
        result = new MemoryPolicyVO();
        result.setUuid(scope + ":" + resource);
        result.setScope(scope);
        result.setResourceUuid(resource);
        result.setPolicy("{\"schemaVersion\":1}");
        return result;
    }

    /** Standard memory configuration is stored by GlobalConfig (no resource) or a Host/Cluster ResourceConfig. */
    private String standardScope(EntityManager em, String resourceUuid) {
        if (resourceUuid == null) { return "Global"; }
        // Only the registry discriminator is needed here. Materializing
        // JOINED ResourceVO loads every registered resource subtype and
        // exceeds MariaDB's 61-table join limit in the full product.
        String resourceType = em.createQuery("select r.resourceType from ResourceVO r where r.uuid = :uuid", String.class)
                .setParameter("uuid", resourceUuid).setMaxResults(1).getResultList().stream()
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (HostVO.class.getSimpleName().equals(resourceType)) { return "Host"; }
        if (org.zstack.header.cluster.ClusterVO.class.getSimpleName().equals(resourceType)) { return "Cluster"; }
        throw error("MEMORY_INVALID_SCOPE", "Memory configuration requires a KVM Host or Cluster");
    }

    /** Read-only cross-field validation of one standard value before GlobalConfig/ResourceConfig persists it. */
    public void validateStandardChange(MemoryStandardField field, String resourceUuid, String newValue) {
        transaction(em -> {
            String scope = standardScope(em, resourceUuid);
            String resource = resourceUuid == null ? "global" : resourceUuid;
            if (!field.allows(scopeOf(scope))) {
                throw error("MEMORY_INVALID_SCOPE", field.path() + " does not support " + scope);
            }
            validateResource(em, scope, resource);
            Object value = field.isFeatureSwitch() && newValue != null && "none".equalsIgnoreCase(newValue.trim())
                    ? null : MemoryStandardConfigCodec.decode(field.path(), newValue);
            try {
                String candidate = ownPolicy(em, scope, resource, Collections.singleton(field.path()));
                candidate = MemoryStandardPolicy.set(candidate, field.path(), value);
                validateConfigurationCandidate(em, scope, resource, candidate);
            } catch (IllegalArgumentException invalid) { throw translatePolicyRuleFailure(invalid); }
            return null;
        });
    }

    /** Shared by standard writes and backend preparation, before either persists values. */
    private void validateConfigurationCandidate(EntityManager em, String scope, String resource, String candidate) {
        mergeEffectiveLayer(em, parentEffective(em, scope, resource), scope, resource, candidate);
        // A parent's final candidate must not invalidate surviving child overrides.
        for (String host : targets(em, scope, resource)) {
            effectiveHostWithCandidate(em, host, scope, resource, candidate);
        }
    }

    /** Runs after a standard GlobalConfig/ResourceConfig change was committed; queues per-Host application. */
    public void standardConfigurationChanged(String resourceUuid) {
        transaction(em -> {
            String scope = standardScope(em, resourceUuid);
            String resource = resourceUuid == null ? "global" : resourceUuid;
            commitConfiguration(em, scope, resource, null, targets(em, scope, resource),
                    "system:standard-config", null, null, "apply", false);
            return null;
        });
    }

    /**
     * The common configuration commit for both public entry families. The caller
     * owns the same transaction as the already-written standard rows. Ordinary
     * APIs persist desired intent for unavailable Hosts; the legacy CAS/preview
     * entry retains its stricter pre-admission and full-payload behavior.
     */
    private MemoryTaskInventory commitConfiguration(EntityManager em, String scope, String resource,
            String specializedPolicy, List<String> hosts, String actor, String requestKey,
            String requestHash, String action, boolean compatibility) {
        lock(em);
        em.flush();
        bumpScopeRevision(em, scope, resource);
        MemoryPolicyVO desired = policy(em, scope, resource);
        if (specializedPolicy != null) { desired.setPolicy(specializedPolicy); }
        MemoryTaskVO parent = configurationTask(scope, resource, action, null, null,
                desired.getRevision(), "{}", actor);
        parent.setRequestKey(requestKey); parent.setRequestHash(requestHash);
        parent.setStatus("Succeeded");
        if (!compatibility) {
            parent.setReason("Standard configuration committed; inspect per-Host policy application status");
        }
        em.persist(parent); bumpQueryVersion(em, "task");
        boolean queued = false, blocked = false;
        for (String host : hosts) {
            MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
            if (state == null) {
                state = new MemoryStateVO(); state.setHostUuid(host); state.setStatus("Unknown");
                em.persist(state); bumpQueryVersion(em, "state");
            }
            if (compatibility && state.getActiveTaskUuid() != null) {
                throw error("MEMORY_HOST_BUSY", "Host has an unresolved memory operation: " + host);
            }
            String effectivePolicy = effective(em, "Host", host);
            HostPolicyPlan plan = planForHost(em, host, effectivePolicy, state);
            boolean legacyReview = "NeedsReview".equals(state.getPolicyPlanStatus());
            boolean compatibilityDelta = compatibility && "apply".equals(action) && !plan.replacesAppliedPolicy
                    && !plan.maintenanceActivation && !legacyReview;
            if (!compatibility && queueConfigurationReconcile(em, parent, state)) {
                queued = true;
                continue;
            }
            if ((!compatibility || compatibilityDelta) && plan.changedFields.isEmpty()) {
                // Reverting to the confirmed applied target must not retain
                // the previous target's blocker. Use the existing admission
                // fences; a no-op save is not a recovery or a new execution.
                if ("Succeeded".equals(state.getStatus())
                        && !"NeedsReview".equals(state.getPolicyPlanStatus())
                        && !plan.hasBlockers
                        && standardMutationDeferReason(em, host, state, System.currentTimeMillis(), false) == null) {
                    recordPolicyPlan(state, plan, "Applied");
                }
                continue;
            }
            long admissionTime = System.currentTimeMillis();
            Map<String, Object> coldKsm = !compatibility && isPureKsmPlan(plan)
                    && MemoryCapabilityRules.coldKsmAdmissionReason(state, admissionTime) == null
                    ? MemoryCapabilityRules.coldKsmObservation(state, admissionTime) : null;
            String defer = compatibility ? null : standardMutationDeferReason(em, host, state, admissionTime, coldKsm != null);
            if (!compatibility && defer == null && isConfirmedPausedState(state.getState())
                    && !plan.maintenanceActivation && state.getControlOperationUuid() != null) {
                MemoryTaskVO control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
                if (control != null && "drain".equals(control.getAction())) {
                    defer = "HOST_DRAIN_MAINTENANCE_REQUIRED";
                }
            }
            if (defer != null) { plan = deferredPlan(plan, defer); }
            // The standard batch is one candidate. Do not partially dispatch a
            // writeback/reclaim configuration when any field lacks admission.
            boolean canIssue = (compatibility && !compatibilityDelta)
                    || (compatibilityDelta && plan.hasPayload
                            && (!plan.hasBlockers || plan.maintenanceActivation || plan.writebackDisableOnly))
                    || (!compatibility && defer == null && plan.hasPayload
                            && (!plan.hasBlockers || plan.maintenanceActivation || plan.writebackDisableOnly));
            String status = canIssue ? "Queued" : "Blocked";
            // An accepted request is not evidence that a legacy operation was
            // applied. Keep NeedsReview until the matching fresh Agent result.
            if ((compatibilityDelta || !compatibility || plan.maintenanceActivation) && !legacyReview
                    && !isConfigurationReconcile(state.getActiveTaskUuid() == null ? null
                            : em.find(MemoryTaskVO.class, state.getActiveTaskUuid()))) {
                recordPolicyPlan(state, plan, status);
            }
            long revision = Math.addExact(state.getDesiredRevision(), 1L);
            String childAction = !compatibility && plan.replacesAppliedPolicy ? "clearOverride" : action;
            boolean automaticMaintenance = !compatibility && canIssue && plan.capacityMaintenance != null;
            MemoryTaskVO child = configurationTask(automaticMaintenance ? "Host" : scope,
                    automaticMaintenance ? host : resource, automaticMaintenance ? "drain" : childAction,
                    parent.getUuid(), host, revision, automaticMaintenance ? plan.capacityMaintenance
                            : compatibility && !compatibilityDelta && !plan.maintenanceActivation
                                    ? effectivePolicy : canIssue ? plan.payload : "{}", actor);
            if (automaticMaintenance) { child.setExpectedControlOperationUuid(state.getControlOperationUuid()); }
            if (!compatibility || compatibilityDelta || plan.maintenanceActivation) { child.setPolicyPlanHash(plan.planHash); }
            if (canIssue && coldKsm != null) {
                child.setRequiresKsmNativePartialFence(true);
                child.setKsmNativeObservationBootId((String) coldKsm.get("bootId"));
                child.setKsmNativeObservationSampleTime((Long) coldKsm.get("sampleTime"));
            }
            child.setStatus(status);
            if (!canIssue) {
                child.setReason("Policy not applied: " + plan.blockedFields);
                blocked = true;
            } else {
                state.setActiveTaskUuid(child.getUuid()); state.setDesiredRevision(revision);
                state.setControlOperationUuid(child.getUuid()); state.setPermitAuthorized(false);
                state.setStatus("Queued"); state.setReason(null); queued = true;
            }
            em.persist(child);
        }
        parent.setStatus(queued ? "Queued" : blocked ? "Blocked" : "Succeeded");
        em.flush();
        return parent.toInventory();
    }

    static boolean isConfigurationReconcile(MemoryTaskVO task) {
        return task != null && "Host".equals(task.getScope()) && "reconcile".equals(task.getAction())
                && task.getReconcileOperationUuid() != null
                && "system:memory-config-reconcile".equals(task.getActorUuid());
    }

    private boolean queueConfigurationReconcile(EntityManager em, MemoryTaskVO parent, MemoryStateVO state) {
        MemoryTaskVO active = state.getActiveTaskUuid() == null ? null
                : em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
        if (active == null || !"Unknown".equals(active.getStatus())
                || !Objects.equals(state.getHostUuid(), active.getHostUuid())
                || !hasFreshCapabilityObservation(state, System.currentTimeMillis())
                || em.createQuery("select h.status from HostVO h where h.uuid = :uuid", HostStatus.class)
                    .setParameter("uuid", state.getHostUuid()).getSingleResult() != HostStatus.Connected) { return false; }
        MemoryTaskVO original = active.getReconcileOperationUuid() == null ? active
                : em.find(MemoryTaskVO.class, active.getReconcileOperationUuid());
        if (original == null || !"Unknown".equals(original.getStatus()) || "VM".equals(original.getScope())
                || !Objects.equals(state.getHostUuid(), original.getHostUuid())
                || original.getDesiredRevision() != state.getDesiredRevision()) { return false; }
        MemoryTaskVO root = original.getParentUuid() == null ? null
                : em.find(MemoryTaskVO.class, original.getParentUuid());
        boolean preparation = isSelectedBackendMaintenance(original, root)
                && MemoryBackendPreparationRules.ACTION.equals(original.getAction());
        if (preparation) {
            JsonObject context = new JsonParser().parse(root.getPolicy()).getAsJsonObject();
            if (!Objects.equals(original.getExpectedControlOperationUuid(), state.getControlOperationUuid())
                    || !"none".equals(getString(context, "poolGeneration"))
                    || !isFirstPoolNonZramControl(em, state, getString(context, "bootId"))) { return false; }
        } else if (!Arrays.asList("apply", "clearOverride").contains(original.getAction())
                || !Objects.equals(original.getUuid(), state.getControlOperationUuid())
                || !em.createQuery("select t.uuid from MemoryTaskVO t where t.action = :action "
                        + "and t.expectedControlOperationUuid = :original and t.status not in ('Failed', 'Cancelled')", String.class)
                    .setParameter("action", MemoryUncertainRecoveryRules.ACTION)
                    .setParameter("original", original.getUuid()).setMaxResults(1).getResultList().isEmpty()) {
            return false;
        }
        // A save schedules one result query, not another apply. Retain both
        // the original revision and control fence until independent proof is
        // verified. Another save while this query is in flight only saves intent.
        MemoryTaskVO query = configurationTask("Host", state.getHostUuid(), "reconcile", parent.getUuid(),
                state.getHostUuid(), original.getDesiredRevision(), "{}", "system:memory-config-reconcile");
        query.setReconcileOperationUuid(original.getUuid());
        query.setExpectedControlOperationUuid(state.getControlOperationUuid());
        em.persist(query);
        state.setActiveTaskUuid(query.getUuid()); state.setPermitAuthorized(false);
        return true;
    }

    /**
     * The first ZRAM pool is created after the apply returns, so that apply ends
     * Blocked/NATIVE_POOL_UNQUALIFIED. Once a fresh observation verifies the pool
     * capacity, queue the same reconcile an operator would submit for that task.
     */
    public boolean reconcileVerifiedPoolBlock(String hostUuid, MemoryAgentResponse response) {
        Map<String, Object> actual = response == null || !response.isSuccess() || response.state == null
                ? null : objectMap(response.state.get("actual"));
        Map<String, Object> zram = actual == null ? null : objectMap(actual.get("zram"));
        Map<String, Object> capacity = zram == null ? null : objectMap(zram.get("capacity"));
        if (capacity == null || !"APPLIED".equals(capacity.get("apply_state"))) { return false; }
        APIUpdateMemoryPolicyMsg msg = transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid);
            MemoryTaskVO active = state == null || state.getActiveTaskUuid() == null ? null
                    : em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
            if (active == null || !"apply".equals(active.getAction()) || !"Blocked".equals(active.getStatus())
                    || !"NATIVE_POOL_UNQUALIFIED".equals(active.getReason())) { return null; }
            APIUpdateMemoryPolicyMsg reconcile = new APIUpdateMemoryPolicyMsg();
            reconcile.setScope("Host"); reconcile.setResourceUuid(hostUuid); reconcile.setAction("reconcile");
            reconcile.setExpectedRevision(policy(em, "Host", hostUuid).getRevision());
            reconcile.setExpectedControlOperationUuid(state.getControlOperationUuid());
            // One reconcile per blocked task; a repeated observation reuses the same request.
            reconcile.setClientRequestUuid(digest(active.getUuid()).substring(0, 32));
            return reconcile;
        });
        if (msg == null) { return false; }
        submit(msg, "system:memory-pool-ready-reconcile", null);
        return true;
    }

    private String standardMutationDeferReason(EntityManager em, String host, MemoryStateVO state,
            long now, boolean coldKsmAdmission) {
        HostStatus status = em.createQuery("select h.status from HostVO h where h.uuid = :uuid", HostStatus.class)
                .setParameter("uuid", host).getResultList().stream().findFirst().orElse(null);
        if (status != HostStatus.Connected) { return "HOST_NOT_CONNECTED"; }
        if (state.getActiveTaskUuid() != null || hasUnresolvedControl(em, state)) { return "HOST_OPERATION_UNRESOLVED"; }
        if (("Unknown".equals(state.getStatus()) || state.getStatus() == null) && !coldKsmAdmission) {
            return "HOST_STATE_UNKNOWN";
        }
        if (!hasFreshCapabilityObservation(state, now) && !coldKsmAdmission) {
            return "CAPABILITY_STALE";
        }
        return configurationControlDeferReason(em, state);
    }

    private static boolean isPureKsmPlan(HostPolicyPlan plan) {
        if (plan == null || !plan.hasPayload || plan.hasBlockers || plan.changedFields == null
                || plan.changedFields.isEmpty()) { return false; }
        for (String field : plan.changedFields.keySet()) {
            if (!field.startsWith("ksm.")) { return false; }
        }
        try {
            JsonObject payload = new JsonParser().parse(plan.payload).getAsJsonObject();
            return payload.has("ksm") && payload.get("ksm").isJsonObject()
                    && !payload.has("zram") && !payload.has("writeback");
        } catch (RuntimeException invalid) { return false; }
    }

    /** Identical pause/drain/resume evidence rules for both configuration entry families. */
    private String configurationControlDeferReason(EntityManager em, MemoryStateVO state) {
        try {
            MemoryTaskVO control = null;
            boolean drainedMaintenanceReady = false;
            if (state.getActiveTaskUuid() != null) {
                control = em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
                if (control != null && ("pause".equals(control.getAction()) || "drain".equals(control.getAction()))) {
                    return "HOST_POLICY_PAUSED";
                }
            }
            if (state.getControlOperationUuid() != null) {
                control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
                if (control != null && "reconcile".equals(control.getAction())
                        && "Succeeded".equals(control.getStatus()) && control.getReconcileOperationUuid() != null) {
                    // A legacy query alias is not an executor receipt. Finish
                    // its original result query before automatic or explicit
                    // configuration can replace that unresolved bookkeeping.
                    return "HOST_CONTROL_STATE_UNKNOWN";
                }
                if (control != null && "pause".equals(control.getAction())
                        && ("Succeeded".equals(control.getStatus()) || MemoryTaskRules.blocksHost(control.getStatus()))) {
                    return "HOST_POLICY_PAUSED";
                }
                if (control != null && "drain".equals(control.getAction())) {
                    if (!"Succeeded".equals(control.getStatus())
                            || !isMaintenanceRecoveryState(state.getState(), state.getControlOperationUuid())) {
                        return "HOST_DRAIN_MAINTENANCE_REQUIRED";
                    }
                    drainedMaintenanceReady = true;
                }
                if (!drainedMaintenanceReady) {
                    drainedMaintenanceReady = rejectedMaintenanceActivationReady(em, state, control);
                }
            }
            boolean resumeCompleted = control != null && "resume".equals(control.getAction())
                    && "Succeeded".equals(control.getStatus()) && state.getLastSampleTime() != null
                    && control.getLastOpDate() != null && state.getLastSampleTime() <= control.getLastOpDate().getTime();
            if (isConfirmedPausedState(state.getState()) && !resumeCompleted && !drainedMaintenanceReady) {
                return "HOST_POLICY_PAUSED";
            }
        } catch (RuntimeException invalid) { return "HOST_CONTROL_STATE_UNKNOWN"; }
        return null;
    }

    /** A typed pre-activation rejection leaves the Agent's successful drain
     * fence intact. Recognize that fresh proof without rewriting either task
     * or treating arbitrary Failed/Unknown operations as safe to retry. */
    private static final String MAINTENANCE_ACTIVATION_NATIVE_NOT_READY = "MAINTENANCE_ACTIVATION_NATIVE_NOT_READY";
    /** Typed pre-activation rejections: the Agent never started the activation. */
    private static final List<String> REJECTED_ACTIVATION_RETRY_REASONS = Arrays.asList(
            "MAINTENANCE_ACTIVATION_WRITEBACK_MUST_REMAIN_DISABLED", MAINTENANCE_ACTIVATION_NATIVE_NOT_READY);
    private static final long REJECTED_ACTIVATION_RETRY_DELAY_MS = 60_000L;

    private static boolean rejectedMaintenanceActivationReady(EntityManager em, MemoryStateVO state,
                                                               MemoryTaskVO rejected) {
        if (state.getActiveTaskUuid() != null || rejected == null || !"Host".equals(rejected.getScope())
                || !"apply".equals(rejected.getAction()) || !"Failed".equals(rejected.getStatus())
                || !Objects.equals(state.getHostUuid(), rejected.getHostUuid())
                || !REJECTED_ACTIVATION_RETRY_REASONS.contains(rejected.getReason())
                || rejected.getLastOpDate() == null || state.getLastSampleTime() == null
                || state.getLastSampleTime() < rejected.getLastOpDate().getTime()) { return false; }
        // The rejected activation itself may be the Host's latest failed result.
        if (!"Succeeded".equals(state.getStatus()) && !("Failed".equals(state.getStatus())
                && rejected.getUuid().equals(state.getControlOperationUuid()))) { return false; }
        try {
            JsonObject root = new JsonParser().parse(state.getState()).getAsJsonObject();
            // A service restart can reject activation before its first safe Host
            // sample. Retry only after a fresh safe sample and a short delay.
            if (MAINTENANCE_ACTIVATION_NATIVE_NOT_READY.equals(rejected.getReason())) {
                JsonObject host = root.getAsJsonObject("host");
                if (host == null || !getBoolean(host, "qualified") || !getBoolean(host, "safe")
                        || System.currentTimeMillis() - rejected.getLastOpDate().getTime() < REJECTED_ACTIVATION_RETRY_DELAY_MS) {
                    return false;
                }
            }
            JsonObject proof = root.getAsJsonObject("maintenanceProof");
            String drainUuid = getString(proof, "drainOperationUuid");
            MemoryTaskVO drain = drainUuid == null ? null : em.find(MemoryTaskVO.class, drainUuid);
            if (!isPauseOrDrain(drain, state.getHostUuid()) || !"drain".equals(drain.getAction())
                    || !isFreshAgentControl(state, drainUuid, System.currentTimeMillis())
                    || !isMaintenanceRecoveryState(state.getState(), drainUuid)) { return false; }
            // This exception is for a rejected initialization, not an active
            // replacement pool. Agent revalidates its current-boot journal,
            // drain archive, receipt and device again before a new activation.
            return Long.valueOf(0L).equals(getLong(proof, "activeOperations"))
                    && proof.has("poolOwnedByService") && !proof.get("poolOwnedByService").getAsBoolean()
                    && getBoolean(proof, "readyForInitialization")
                    && getBoolean(proof, "originalDeviceInactive")
                    && getBoolean(proof, "oldPoolOwnershipAbsent")
                    && getString(proof, "newPoolGeneration") == null;
        } catch (RuntimeException invalid) { return false; }
    }

    private MemoryTaskVO configurationTask(String scope, String resource, String action,
            String parent, String host, long revision, String payload, String actor) {
        MemoryTaskVO task = new MemoryTaskVO();
        task.setUuid(UUID.randomUUID().toString().replace("-", ""));
        task.setParentUuid(parent); task.setHostUuid(host); task.setScope(scope);
        task.setResourceUuid(resource); task.setAction(action);
        task.setDesiredRevision(revision); task.setPolicy(payload); task.setActorUuid(actor);
        task.setStatus("Queued"); return task;
    }

    private void bumpScopeRevision(EntityManager em, String scope, String resource) {
        String uuid = scope + ":" + resource;
        MemoryPolicyVO row = em.find(MemoryPolicyVO.class, uuid, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) {
            row = new MemoryPolicyVO();
            row.setUuid(uuid); row.setScope(scope); row.setResourceUuid(resource);
            row.setRevision(1); row.setPolicy("{\"schemaVersion\":1}");
            em.persist(row);
        } else {
            row.setRevision(Math.addExact(row.getRevision(), 1L));
        }
    }

    private String effective(EntityManager em, String scope, String resource) {
        MemoryPolicyVO own = policy(em, scope, resource);
        if ("VM".equals(scope)) {
            return retainedExclusion(em, resource) == null ? own.getPolicy()
                    : MemoryPolicyRules.merge(own.getPolicy(), "{\"participation\":\"deny\"}", "VM");
        }
        return mergeEffectiveLayer(em, parentEffective(em, scope, resource), scope, resource, ownPolicy(em, scope, resource));
    }

    /** Only explicit values override a parent; a stored none leaves its switch unmanaged, as host.ksm always did. */
    private String mergeEffectiveLayer(EntityManager em, String inherited, String scope,
            String resource, String candidateOwn) {
        if ("Global".equals(scope)) {
            // Standard GlobalConfig already supplies the installation's defaults.
            // A real none must not acquire a synthetic disabled policy fallback.
            for (MemoryStandardField field : MemoryStandardField.values()) {
                if (field.isFeatureSwitch()) { inherited = removePolicyPath(inherited, field.path()); }
            }
        } else {
            MemoryStandardField.Scope level = scopeOf(scope);
            if (level != null) {
                for (Map.Entry<MemoryStandardField, Object> own
                        : standardConfigs.readOwn(em, level, resource, Collections.<String>emptySet()).entrySet()) {
                    if (MemoryStandardConfigAdapter.isUnmanaged(own.getValue())
                            && MemoryStandardConfigCodec.get(candidateOwn, own.getKey().path()) == null) {
                        inherited = removePolicyPath(inherited, own.getKey().path());
                    }
                }
            }
        }
        return MemoryPolicyRules.merge(inherited, candidateOwn, scope);
    }

    private static String removePolicyPath(String json, String path) {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        String[] parts = path.split("\\.", -1);
        if (parts.length == 2 && root.has(parts[0]) && root.get(parts[0]).isJsonObject()) {
            JsonObject section = root.getAsJsonObject(parts[0]);
            section.remove(parts[1]);
            if (section.size() == 0) { root.remove(parts[0]); }
        }
        return root.toString();
    }

    private String ownPolicy(EntityManager em, String scope, String resource) {
        return ownPolicy(em, scope, resource, Collections.<String>emptySet());
    }

    private String ownPolicy(EntityManager em, String scope, String resource, Set<String> replacedFields) {
        if ("VM".equals(scope)) { return policy(em, scope, resource).getPolicy(); }
        String compatibility = policy(em, scope, resource).getPolicy();
        MemoryStandardField.Scope level = scopeOf(scope);
        Map<MemoryStandardField, Object> values = level == null ? Collections.emptyMap()
                : standardConfigs.readOwn(em, level, resource, replacedFields);
        if (level != null) {
            for (MemoryStandardField field : MemoryStandardField.values()) {
                if (field.allows(level) && (replacedFields.contains(field.path())
                        || ("Global".equals(scope) ? values.get(field) != null || field == MemoryStandardField.KSM_ENABLED
                                : values.containsKey(field)))) {
                    compatibility = MemoryStandardConfigCodec.put(compatibility, field.path(), null);
                }
            }
        }
        return MemoryStandardPolicy.combine(compatibility, standardOwnPolicy(values));
    }

    private boolean standardFieldAuthoritative(EntityManager em, String scope, String resource,
            MemoryStandardField field) {
        if ("Global".equals(scope)) {
            Object value = standardConfigs.readGlobalOwn(em, field.path());
            // The Global KSM row is authoritative even at its schema default,
            // so a default "none" masks stale compatibility JSON.
            return value != null || field == MemoryStandardField.KSM_ENABLED;
        }
        return standardConfigs.hasOverrideRow(em, field.path(), resource);
    }

    private String standardOwnPolicy(EntityManager em, String scope, String resource) {
        return standardOwnPolicy(em, scope, resource, Collections.<String>emptySet());
    }

    private String standardOwnPolicy(EntityManager em, String scope, String resource, Set<String> skipFields) {
        MemoryStandardField.Scope standardScope = scopeOf(scope);
        if (standardScope == null) { return "{\"schemaVersion\":1}"; }
        return standardOwnPolicy(standardConfigs.readOwn(em, standardScope, resource, skipFields));
    }

    private String standardOwnPolicy(Map<MemoryStandardField, Object> values) {
        JsonObject result = new JsonObject(); result.addProperty("schemaVersion", 1);
        for (MemoryStandardField field : MemoryStandardField.values()) {
            Object value = values.get(field);
            if (value == null) { continue; }
            if (MemoryStandardConfigAdapter.isUnmanaged(value)) { continue; }
            String[] parts = field.path().split("\\.", -1);
            JsonObject section = result.has(parts[0]) ? result.getAsJsonObject(parts[0]) : new JsonObject();
            section.add(parts[1], new Gson().toJsonTree(value)); result.add(parts[0], section);
        }
        return result.toString();
    }

    /** Capture policy intent before migration; transient qualification never becomes a sticky exclusion. */
    public void captureMigrationParticipation(String vm, String source, String target) {
        transaction(em -> {
            validateResource(em, "VM", vm);
            if (retainedExclusion(em, vm) != null) { return null; }
            MemoryPolicyConfig own = MemoryPolicyRules.decode(policy(em, "VM", vm).getPolicy());
            MemoryPolicyConfig host = MemoryPolicyRules.decode(effective(em, "Host", source));
            MemoryPolicyConfig.Zram zram = host.zram;
            boolean selected = zram != null && Boolean.TRUE.equals(zram.enabled)
                    && (!"list".equals(zram.selectionMode) || (zram.selectedVmUuids != null
                    && zram.selectedVmUuids.stream().anyMatch(id -> id.replace("-", "").equals(vm.replace("-", "")))));
            MemoryVmExclusionVO previous = em.find(MemoryVmExclusionVO.class, vm);
            if (selected && !"deny".equals(own.participation)) {
                if (previous != null && !previous.retained) { em.remove(previous); }
                return null;
            }
            MemoryVmExclusionVO capture = new MemoryVmExclusionVO();
            capture.vmUuid = vm; capture.sourceHostUuid = source; capture.targetHostUuid = target;
            capture.vmPolicyRevision = policy(em, "VM", vm).getRevision();
            Map<String, Long> revisions = new LinkedHashMap<>();
            for (MemoryPolicyVO item : sourceChain(em, "Host", source)) { revisions.put(item.getUuid(), item.getRevision()); }
            revisions.put("Host:" + source, policy(em, "Host", source).getRevision());
            capture.sourceRevisions = JSONObjectUtil.toJsonString(revisions);
            MemoryStateVO state = em.find(MemoryStateVO.class, source);
            if (state != null && state.getState() != null) {
                try {
                    MemoryAgentResponse response = new MemoryAgentResponse();
                    response.state = JSONObjectUtil.toObject(state.getState(), Map.class);
                    capture.sourceInstanceGeneration = MemoryLifecycleHooks.instanceGeneration(response, vm);
                } catch (RuntimeException ignored) { /* Host disabled: no native instance sample is required. */ }
            }
            em.merge(capture); return null;
        });
    }

    private MemoryVmExclusionVO retainedExclusion(EntityManager em, String vm) {
        MemoryVmExclusionVO exclusion = em.find(MemoryVmExclusionVO.class, vm);
        if (exclusion == null) { return null; }
        if (!exclusion.retained) {
            if (policy(em, "VM", vm).getRevision() != exclusion.vmPolicyRevision) {
                em.remove(exclusion); return null; // Explicit upper-layer change superseded the capture.
            }
            List<String> hosts = em.createQuery("select v.hostUuid from VmInstanceVO v where v.uuid = :uuid", String.class)
                    .setParameter("uuid", vm).getResultList();
            if (hosts.isEmpty() || !exclusion.targetHostUuid.equals(hosts.get(0))) { return null; }
            exclusion.retained = true;
        }
        return exclusion;
    }

    public void abortMigrationParticipation(String vm) {
        transaction(em -> {
            MemoryVmExclusionVO exclusion = em.find(MemoryVmExclusionVO.class, vm);
            if (exclusion != null && !exclusion.retained) { em.remove(exclusion); }
            return null;
        });
    }

    public String vmParticipation(String vm) {
        return transaction(em -> {
            if (retainedExclusion(em, vm) != null) { return "deny"; }
            String participation = MemoryPolicyRules.decode(policy(em, "VM", vm).getPolicy()).participation;
            return participation == null ? "inherit" : participation;
        });
    }

    private String parentEffective(EntityManager em, String scope, String resource) {
        if ("Global".equals(scope) || "VM".equals(scope)) { return MemoryPolicyRules.defaults(); }
        String inherited = effective(em, "Global", "global");
        if ("Host".equals(scope)) {
            String cluster = em.createQuery("select h.clusterUuid from HostVO h where h.uuid = :uuid", String.class)
                    .setParameter("uuid", resource).getSingleResult();
            if (cluster != null) {
                inherited = mergeEffectiveLayer(em, inherited, "Cluster", cluster, ownPolicy(em, "Cluster", cluster));
            }
        }
        return inherited;
    }

    private List<MemoryPolicyVO> sourceChain(EntityManager em, String scope, String resource) {
        List<MemoryPolicyVO> chain = new ArrayList<>();
        if (!"VM".equals(scope)) { chain.add(policy(em, "Global", "global")); }
        if ("Host".equals(scope)) {
            String cluster = em.createQuery("select h.clusterUuid from HostVO h where h.uuid = :uuid", String.class)
                    .setParameter("uuid", resource).getSingleResult();
            if (cluster != null) { chain.add(policy(em, "Cluster", cluster)); }
        }
        if (!"Global".equals(scope)) { chain.add(policy(em, scope, resource)); }
        return chain;
    }

    private void describeSources(EntityManager em, String scope, String resource, MemoryPolicyInventory result) {
        describeSources(em, scope, resource, result, Collections.<String>emptySet(), null);
    }

    private void describeSources(EntityManager em, String scope, String resource, MemoryPolicyInventory result,
            Set<String> previewClearedFields, String previewPatch) {
        Map<String, Long> revisions = new LinkedHashMap<>();
        Map<String, String> fields = new LinkedHashMap<>();
        Map<String, String> switchSources = new LinkedHashMap<>();
        if (!"VM".equals(scope)) { MemoryPolicyRules.addFieldSources(MemoryPolicyRules.defaults(), "Default", fields); }
        for (MemoryPolicyVO source : sourceChain(em, scope, resource)) {
            revisions.put(source.getUuid(), source.getRevision());
            Map<String, String> layerFields = new LinkedHashMap<>();
            String visiblePolicy = compatibilityPolicyForSource(em, source);
            MemoryPolicyRules.addFieldSources(visiblePolicy, source.getUuid(), layerFields);
            applyStandardFieldSources(em, source, layerFields);
            if (source.getUuid().equals(scope + ":" + resource)) {
                for (String field : previewClearedFields) { layerFields.remove(field); }
            }
            fields.putAll(layerFields);
            if (!"VM".equals(scope)) {
                boolean editedScope = source.getUuid().equals(scope + ":" + resource);
                String layerPolicy = ownPolicy(em, source.getScope(), source.getResourceUuid());
                for (MemoryStandardField field : MemoryStandardField.values()) {
                    if (!field.isFeatureSwitch() || editedScope && previewClearedFields.contains(field.path())) { continue; }
                    Object value = MemoryStandardConfigCodec.get(layerPolicy, field.path());
                    Object patched = editedScope && previewPatch != null
                            ? MemoryStandardConfigCodec.get(previewPatch, field.path()) : null;
                    if (patched != null) { value = patched; }
                    if (value instanceof Boolean) { switchSources.put(field.path(), source.getUuid()); }
                }
            }
        }
        // Preview patches belong to the edited scope, including explicit false.
        // A GET's ownPolicy includes schema defaults; they are not explicit writes.
        if (previewPatch != null) { MemoryPolicyRules.addFieldSources(previewPatch, scope + ":" + resource, fields); }
        fields.putAll(switchSources);
        result.setSourceRevisions(revisions);
        result.setFieldSources(fields);
        result.setFieldModes("VM".equals(scope) ? Collections.emptyMap() : unmanagedFieldModes(result.getEffectivePolicy()));
        for (MemoryStandardField field : MemoryStandardField.values()) {
            if (field.isFeatureSwitch() && MemoryStandardConfigCodec.get(result.getEffectivePolicy(), field.path()) == null) {
                fields.remove(field.path());
            }
        }
        result.setSourceRevision(policy(em, "Global", "global").getRevision());
    }

    private Map<String, String> unmanagedFieldModes(String effectivePolicy) {
        Map<String, String> modes = new LinkedHashMap<>();
        for (MemoryStandardField field : MemoryStandardField.values()) {
            if (field.isFeatureSwitch() && MemoryStandardConfigCodec.get(effectivePolicy, field.path()) == null) {
                modes.put(field.path(), "Unmanaged");
            }
        }
        return modes;
    }

    private String compatibilityPolicyForSource(EntityManager em, MemoryPolicyVO source) {
        String result = source.getPolicy();
        MemoryStandardField.Scope level = scopeOf(source.getScope());
        if (level == null) { return result; }
        for (MemoryStandardField field : MemoryStandardField.values()) {
            if (field.allows(level) && standardFieldAuthoritative(em, source.getScope(), source.getResourceUuid(), field)) {
                result = MemoryStandardConfigCodec.put(result, field.path(), null);
            }
        }
        return result;
    }

    private Object standardValue(EntityManager em, MemoryPolicyVO source, MemoryStandardField field) {
        return "Global".equals(source.getScope()) ? standardConfigs.readGlobalOwn(em, field.path())
                : standardConfigs.readOverrideOwn(em, field.path(), source.getResourceUuid());
    }

    private void applyStandardFieldSources(EntityManager em, MemoryPolicyVO source, Map<String, String> fields) {
        MemoryStandardField.Scope level = scopeOf(source.getScope());
        if (level == null) { return; }
        for (MemoryStandardField field : MemoryStandardField.values()) {
            if (!field.allows(level) || !standardFieldAuthoritative(em, source.getScope(), source.getResourceUuid(), field)) { continue; }
            Object value = standardValue(em, source, field);
            boolean schemaDefault = "Global".equals(source.getScope())
                    && standardConfigs.globalIsSchemaDefault(em, field.path());
            if (schemaDefault && (!field.isFeatureSwitch()
                    || MemoryStandardConfigAdapter.isUnmanaged(value))) { continue; }
            if (value != null && !MemoryStandardConfigAdapter.isUnmanaged(value)) {
                fields.put(field.path(), source.getUuid());
            }
        }
    }

    private void checkSources(EntityManager em, APIUpdateMemoryPolicyMsg msg) {
        Map<String, Long> current = new LinkedHashMap<>();
        for (MemoryPolicyVO source : sourceChain(em, msg.getScope(), msg.getResourceUuid())) {
            current.put(source.getUuid(), source.getRevision());
        }
        if (msg.getExpectedGlobalRevision() != null
                && !msg.getExpectedGlobalRevision().equals(current.get(GLOBAL))) {
            throw error("MEMORY_REVISION_CONFLICT", "Global policy changed; preview and confirm again");
        }
        if (msg.getExpectedSourceRevisions() != null) {
            if (!current.equals(msg.getExpectedSourceRevisions())) {
                throw error("MEMORY_REVISION_CONFLICT", "Inherited policy changed; preview and confirm again");
            }
            return;
        }
        // Pre-Cluster clients may still use the original Global snapshot, but
        // cannot unknowingly apply over a configured Cluster policy.
        if ("Host".equals(msg.getScope()) || "Cluster".equals(msg.getScope())) {
            boolean clusterConfigured = current.entrySet().stream().anyMatch(e ->
                    e.getKey().startsWith("Cluster:") && !e.getKey().equals(msg.getScope() + ":" + msg.getResourceUuid())
                            && e.getValue() != 0);
            if (clusterConfigured || msg.getExpectedGlobalRevision() == null
                    || !msg.getExpectedGlobalRevision().equals(current.get(GLOBAL))) {
                throw error("MEMORY_REVISION_CONFLICT", "Inherited policy changed; preview and confirm again");
            }
        }
    }

    /** Omitted compatibility scope is inferred from the platform resource registry,
     * never from a policy row or a client-supplied type hint. */
    private String resolvePolicyScope(EntityManager em, String requestedScope, String resource) {
        String scope = requestedScope;
        if (scope == null) {
            if ("global".equals(resource)) { scope = "Global"; }
            else {
                List<String> types = em.createQuery("select r.resourceType from ResourceVO r where r.uuid = :uuid", String.class)
                        .setParameter("uuid", resource).getResultList();
                String type = types.isEmpty() ? null : types.get(0);
                if ("HostVO".equals(type)) { scope = "Host"; }
                else if ("ClusterVO".equals(type)) { scope = "Cluster"; }
                else if ("VmInstanceVO".equals(type)) { scope = "VM"; }
                else { throw error("MEMORY_INVALID_SCOPE", "Expected global or an existing KVM Host, Cluster or VM resource UUID"); }
            }
        }
        validateResource(em, scope, resource);
        return scope;
    }

    /** Read existing policy/control facts only; this observation never admits an operation. */
    MemoryPolicyConfig serviceObservationPolicy(String host) {
        return transaction(em -> {
            HostStatus status = em.createQuery("select h.status from HostVO h where h.uuid = :uuid", HostStatus.class)
                    .setParameter("uuid", host).getResultList().stream().findFirst().orElse(null);
            MemoryStateVO state = em.find(MemoryStateVO.class, host);
            if (status != HostStatus.Connected || state == null || state.getStatus() == null
                    || "Unknown".equals(state.getStatus()) || state.getActiveTaskUuid() != null
                    || hasUnresolvedControl(em, state) || configurationControlDeferReason(em, state) != null) {
                return null;
            }
            return MemoryPolicyRules.decode(effective(em, "Host", host));
        });
    }

    public MemoryPolicyInventory getPolicy(String requestedScope, String resource) {
        return transaction(em -> {
            String scope = resolvePolicyScope(em, requestedScope, resource);
            MemoryPolicyInventory result = policy(em, scope, resource).toInventory();
            result.setPolicy(ownPolicy(em, scope, resource));
            result.setEffectivePolicy(effective(em, scope, resource));
            if ("VM".equals(scope)) {
                MemoryVmExclusionVO exclusion = retainedExclusion(em, resource);
                if (exclusion != null) { result.setMigrationExclusion(exclusion.toInventory()); }
            }
            describeSources(em, scope, resource, result);
            return result;
        });
    }

    private String candidateOwnPolicy(EntityManager em, String scope, String resource, String patch) {
        String specialized = policy(em, scope, resource).getPolicy();
        String standard = standardOwnPolicy(em, scope, resource);
        MemoryStandardPolicy.Parts parts = MemoryStandardPolicy.split(patch, scope);
        specialized = MemoryPolicyRules.mergeOverridesOnly(specialized, parts.specialized, scope);
        for (Map.Entry<String, Object> value : parts.standardValues.entrySet()) {
            standard = MemoryStandardPolicy.set(standard, value.getKey(), value.getValue());
            specialized = MemoryStandardConfigCodec.put(specialized, value.getKey(), null);
        }
        return MemoryStandardPolicy.combine(specialized, standard);
    }

    private String effectiveHostWithCandidate(EntityManager em, String host, String changedScope,
            String changedResource, String candidateOwn) {
        String global = "Global".equals(changedScope) && "global".equals(changedResource)
                ? candidateOwn : ownPolicy(em, "Global", "global");
        String result = mergeEffectiveLayer(em, MemoryPolicyRules.defaults(), "Global", "global", global);
        String cluster = em.createQuery("select h.clusterUuid from HostVO h where h.uuid = :uuid", String.class)
                .setParameter("uuid", host).getSingleResult();
        if (cluster != null) {
            String clusterPolicy = "Cluster".equals(changedScope) && cluster.equals(changedResource)
                    ? candidateOwn : ownPolicy(em, "Cluster", cluster);
            result = mergeEffectiveLayer(em, result, "Cluster", cluster, clusterPolicy);
        }
        String hostPolicy = "Host".equals(changedScope) && host.equals(changedResource)
                ? candidateOwn : ownPolicy(em, "Host", host);
        return mergeEffectiveLayer(em, result, "Host", host, hostPolicy);
    }

    public MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg) {
        return submit(msg, (MemoryAgentResponse) null);
    }

    MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg, MemoryAgentResponse backendObservation) {
        // Normalize before action validation and request hashing so omission and
        // the equivalent old explicit scope share one idempotency identity.
        msg.setScope(transaction(em -> resolvePolicyScope(em, msg.getScope(), msg.getResourceUuid())));
        return submit(msg, null, backendObservation);
    }

    private MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg,
                                       String internalActor, MemoryAgentResponse backendObservation) {
        MemoryBackendPreparationRules.validate(msg);
        MemoryUncertainRecoveryRules.validate(msg);
        MemoryPolicyActionParameters.validate(msg);
        boolean recovery = MemoryUncertainRecoveryRules.ACTION.equals(msg.getAction());
        boolean preparation = MemoryBackendPreparationRules.ACTION.equals(msg.getAction());
        if (!MemoryPolicyRules.validUuid(msg.getClientRequestUuid())) {
            throw error("MEMORY_INVALID_REQUEST", "clientRequestUuid must be a UUID");
        }
        String actor = internalActor == null
                ? msg.getSession().getAccountUuid() + ":" + String.valueOf(msg.getSession().getUserUuid())
                : internalActor;
        String key = actor + ":" + msg.getClientRequestUuid();
        List<Object> requestIdentity = new ArrayList<>(Arrays.asList(msg.getScope(), msg.getResourceUuid(),
                msg.getAction(), msg.getExpectedRevision(), msg.getExpectedGlobalRevision(), msg.getExpectedSourceRevisions(),
                msg.getExpectedInstanceGeneration(), msg.getTargetHostUuids(),
                JSONObjectUtil.toObject(msg.getPolicy(), Map.class)));
        if (preparation) {
            requestIdentity.add(msg.getBackendPreparation());
            requestIdentity.add(msg.getExpectedControlOperationUuid());
        }
        if (recovery) {
            requestIdentity.add(msg.getRecovery()); requestIdentity.add(msg.getExpectedControlOperationUuid());
        }
        if (!preparation && !recovery && msg.getExpectedControlOperationUuid() != null) {
            requestIdentity.add(msg.getExpectedControlOperationUuid());
        }
        String hash = digest(MemoryPolicyRules.canonicalRequest(requestIdentity));
        try {
        return transaction(em -> {
            lock(em);
            List<MemoryTaskVO> previous = em.createQuery("from MemoryTaskVO t where t.requestKey = :key", MemoryTaskVO.class)
                    .setParameter("key", key).getResultList();
            if (!previous.isEmpty()) {
                if (!hash.equals(previous.get(0).getRequestHash())) {
                    throw error("MEMORY_IDEMPOTENCY_CONFLICT", "Request UUID was already used with different parameters");
                }
                return previous.get(0).toInventory();
            }
            List<MemoryTaskIdempotencyReceiptVO> purged = em.createQuery(
                            "from MemoryTaskIdempotencyReceiptVO r where r.requestKey = :key",
                            MemoryTaskIdempotencyReceiptVO.class)
                    .setParameter("key", key).getResultList();
            if (!purged.isEmpty()) {
                MemoryTaskIdempotencyReceiptVO receipt = purged.get(0);
                if (!Objects.equals(hash, receipt.getRequestHash())) {
                    throw error("MEMORY_IDEMPOTENCY_CONFLICT", "Request UUID was already used with different parameters");
                }
                throw error("MEMORY_REQUEST_RESULT_PURGED", "Original task " + receipt.getTaskUuid()
                        + " finished as " + receipt.getOriginalStatus() + "; the request will not be replayed");
            }
            List<String> availableTargets = targets(em, msg.getScope(), msg.getResourceUuid());
            List<String> hosts = MemoryTargetRules.select(msg.getScope(), availableTargets, msg.getTargetHostUuids());
            if ("VM".equals(msg.getScope()) && !hosts.isEmpty() && msg.getExpectedInstanceGeneration() == null) {
                throw error("MEMORY_INSTANCE_REQUIRED", "Refresh the actual VM instance generation before changing participation");
            }
            if (!"VM".equals(msg.getScope()) && !"reconcile".equals(msg.getAction())) {
                List<String> unavailable = em.createQuery(
                                "select h.uuid from HostVO h where h.uuid in :hosts and h.status <> 'Connected' order by h.uuid",
                                String.class)
                        .setParameter("hosts", hosts).getResultList();
                if (!unavailable.isEmpty()) {
                    throw error("MEMORY_HOST_NOT_CONNECTED",
                            "Configuration requires Connected hosts: " + String.join(",", unavailable));
                }
            }
            if ("resume".equals(msg.getAction()) && msg.getExpectedControlOperationUuid() == null) {
                throw error("MEMORY_CONTROL_OPERATION_REQUIRED", "Resume requires the active control operation UUID");
            }
            MemoryPolicyVO desired = policy(em, msg.getScope(), msg.getResourceUuid());
            if (desired.getRevision() != msg.getExpectedRevision()) {
                throw error("MEMORY_REVISION_CONFLICT", "Policy has changed; refresh before applying");
            }
            if (preparation || msg.getExpectedSourceRevisions() != null
                    || msg.getExpectedGlobalRevision() != null) { checkSources(em, msg); }
            if (Arrays.asList("pause", "drain").contains(msg.getAction())
                    && msg.getExpectedControlOperationUuid() != null) {
                for (String host : hosts) {
                    MemoryStateVO control = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
                    if (control == null || !Objects.equals(control.getControlOperationUuid(), msg.getExpectedControlOperationUuid())) {
                        throw error("MEMORY_CONTROL_OPERATION_FENCED", "Refresh the Host control operation: " + host);
                    }
                }
            }
            if (!preparation && !recovery && !MemoryTaskRules.isSafetyAction(msg.getAction(), msg.getPolicy())) {
                throw error("MEMORY_INVALID_ACTION", "Safety actions cannot contain configuration");
            }
            MemoryTaskVO parent = task(msg, null, null, desired.getRevision(), "{}", actor);
            parent.setRequestKey(key);
            parent.setRequestHash(hash);
            parent.setStatus(hosts.isEmpty() ? "Succeeded" : "Queued");
            parent.setReason(null);
            bumpQueryVersion(em, "task");
            em.persist(parent);
            for (String host : hosts) {
                MemoryStateVO state = em.find(MemoryStateVO.class, host);
                if (state != null && state.getActiveTaskUuid() != null) {
                    if (!"reconcile".equals(msg.getAction()) && !recovery) {
                        throw error("MEMORY_HOST_BUSY", "Host has an unresolved memory operation: " + host);
                    }
                    MemoryTaskVO unresolved = em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
                    if (unresolved != null && !Arrays.asList("Unknown", "Blocked").contains(unresolved.getStatus())) {
                        throw error("MEMORY_HOST_BUSY", "Only an Unknown or Blocked result can be reconciled");
                    }
                }
                if (state == null) {
                    state = new MemoryStateVO(); state.setHostUuid(host); state.setStatus("Unknown"); em.persist(state);
                    bumpQueryVersion(em, "state");
                }
                if (recovery) { validateUncertainRecovery(em, state, msg); }
                if (preparation && !Objects.equals(state.getControlOperationUuid(), msg.getExpectedControlOperationUuid())) {
                    throw error("MEMORY_CONTROL_OPERATION_FENCED", "Refresh the Host control operation before backend maintenance: " + host);
                }
                JsonObject backendMaintenance = MemoryBackendPreparationRules.ACTION.equals(msg.getAction())
                        && backendObservation != null
                        ? backendMaintenanceRequest(em, msg, state, backendObservation) : null;
                if (backendMaintenance != null) { parent.setPolicy(backendMaintenance.toString()); }
                if ("resume".equals(msg.getAction())) {
                    MemoryTaskVO pause = msg.getExpectedControlOperationUuid() == null ? null
                            : em.find(MemoryTaskVO.class, msg.getExpectedControlOperationUuid());
                    if (state.getActiveTaskUuid() != null
                            || !"Succeeded".equals(state.getStatus())
                            || !Objects.equals(msg.getExpectedControlOperationUuid(), state.getControlOperationUuid())
                            || pause == null || !"pause".equals(pause.getAction())
                            || !"Succeeded".equals(pause.getStatus()) || !Objects.equals(host, pause.getHostUuid())) {
                        throw error("MEMORY_CONTROL_OPERATION_FENCED", "Resume control operation is stale or unresolved: " + host);
                    }
                    try {
                        if (!isConfirmedPausedState(state.getState())) {
                            throw error("MEMORY_RESUME_NOT_PAUSED", "Only a confirmed pause can be resumed: " + host);
                        }
                    } catch (MemoryOperationException e) { throw e; }
                    catch (RuntimeException e) { throw error("MEMORY_RESUME_STATE_UNKNOWN", "Paused state is not verifiable: " + host); }
                }
                boolean vmOnly = "VM".equals(msg.getScope());
                long revision = vmOnly ? desired.getRevision()
                        : state.getDesiredRevision();
                MemoryTaskVO child = task(msg, parent.getUuid(), host, revision,
                        recovery ? JSONObjectUtil.toJsonString(msg.getRecovery())
                                : preparation ? JSONObjectUtil.toJsonString(msg.getBackendPreparation())
                                : "VM".equals(msg.getScope()) ? desired.getPolicy() : effective(em, "Host", host), actor);
                if (Arrays.asList("pause", "drain").contains(child.getAction())
                        && child.getExpectedControlOperationUuid() == null) {
                    // The Agent fences pause/drain on its active control; callers need not echo it.
                    child.setExpectedControlOperationUuid(state.getControlOperationUuid());
                }
                boolean backendDrain = backendMaintenance != null && getBoolean(backendMaintenance, "drainRequired");
                if (backendMaintenance != null) {
                    HostPolicyPlan plan = planForHost(em, host, effective(em, "Host", host), state);
                    if (backendDrain) {
                        child.setAction("drain"); child.setPolicy(backendMaintenance.toString());
                    }
                    child.setPolicyPlanHash(plan.planHash);
                    recordPolicyPlan(state, plan, "Queued");
                    state.setStatus("Queued"); state.setReason(null);
                }
                boolean preserveMaintenanceControlFence = recovery;
                MemoryTaskVO rejectedResume = null;
                // Reconcile tracks the original operation; it never repeats destructive commands.
                if ("reconcile".equals(msg.getAction()) && state.getActiveTaskUuid() != null) {
                    MemoryTaskVO old = em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
                    String operationUuid = old != null && old.getReconcileOperationUuid() != null
                            ? old.getReconcileOperationUuid() : state.getActiveTaskUuid();
                    child.setReconcileOperationUuid(operationUuid);
                    MemoryTaskVO original = old != null && old.getReconcileOperationUuid() != null
                            ? em.find(MemoryTaskVO.class, old.getReconcileOperationUuid()) : old;
                    if (original != null && MemoryUncertainRecoveryRules.ACTION.equals(original.getAction())) {
                        if (!Objects.equals(original.getExpectedControlOperationUuid(), state.getControlOperationUuid())) {
                            throw error("MEMORY_CONTROL_OPERATION_FENCED", "Uncertain recovery control owner changed: " + host);
                        }
                        child.setExpectedControlOperationUuid(original.getExpectedControlOperationUuid());
                        preserveMaintenanceControlFence = true;
                    }
                    // Maintenance preparation keeps the original drain UUID as
                    // the Host control fence. A reconcile queries the original
                    // Agent journal; it never replaces that Agent-side fence
                    // with the reconcile task UUID.
                    if (original != null && isMaintenancePreparationAction(original.getAction())) {
                        String drainUuid = original.getExpectedControlOperationUuid();
                        MemoryTaskVO drain = drainUuid == null ? null : em.find(MemoryTaskVO.class, drainUuid);
                        if (drain == null || !"drain".equals(drain.getAction())
                                || !"Succeeded".equals(drain.getStatus())
                                || !Objects.equals(host, drain.getHostUuid())) {
                            throw error("MEMORY_CONTROL_OPERATION_FENCED",
                                    "The original successful Host drain cannot be verified: " + host);
                        }
                        if (!Objects.equals(drainUuid, state.getControlOperationUuid())) {
                            throw error("MEMORY_CONTROL_OPERATION_FENCED",
                                    "The Host control fence changed after maintenance was submitted: " + host);
                        }
                        preserveMaintenanceControlFence = true;
                    }
                } else if ("reconcile".equals(msg.getAction())) {
                    rejectedResume = rejectedResumeRecoverySource(em, state, msg, host, System.currentTimeMillis());
                    MemoryTaskVO currentControl = state.getControlOperationUuid() == null ? null
                            : em.find(MemoryTaskVO.class, state.getControlOperationUuid());
                    if (currentControl != null && "resume".equals(currentControl.getAction())
                            && "Failed".equals(currentControl.getStatus()) && rejectedResume == null) {
                        throw error("MEMORY_CONTROL_OPERATION_FENCED",
                                "Rejected resume recovery requires its original same-boot control proof: " + host);
                    }
                    if (rejectedResume != null) {
                        child.setReconcileOperationUuid(rejectedResume.getUuid());
                        child.setExpectedControlOperationUuid(rejectedResume.getExpectedControlOperationUuid());
                        preserveMaintenanceControlFence = true;
                    } else if (currentControl != null && "reconcile".equals(currentControl.getAction())
                            && "Succeeded".equals(currentControl.getStatus())
                            && currentControl.getReconcileOperationUuid() != null) {
                        // Older MNs closed the query but kept its UUID as the
                        // control owner. Query the same original journal, not
                        // this new read request, to finish the existing chain.
                        MemoryTaskVO original = em.find(MemoryTaskVO.class, currentControl.getReconcileOperationUuid());
                        if (original == null || !"Succeeded".equals(original.getStatus())
                                || !Arrays.asList("apply", "clearOverride", "pause", "resume", "drain").contains(original.getAction())
                                || "VM".equals(original.getScope()) || "VM".equals(msg.getScope())
                                || !Objects.equals(host, original.getHostUuid())
                                || !Objects.equals(host, currentControl.getHostUuid())
                                || currentControl.getDesiredRevision() != original.getDesiredRevision()
                                || original.getDesiredRevision() != state.getDesiredRevision()) {
                            throw error("MEMORY_CONTROL_OPERATION_FENCED", "Completed reconciliation no longer owns the Host control: " + host);
                        }
                        child.setReconcileOperationUuid(original.getUuid());
                        // A cancellable read must leave this durable alias
                        // available until its original receipt is verified.
                        preserveMaintenanceControlFence = true;
                    }
                }
                em.persist(child);
                state.setActiveTaskUuid(child.getUuid());
                if (rejectedResume != null) { state.setStatus("Queued"); }
                if (!vmOnly) {
                    state.setDesiredRevision(revision);
                    // Maintenance must retain the original drain fence for native proof.
                    if (backendDrain || !preparation && !preserveMaintenanceControlFence) {
                        state.setControlOperationUuid(child.getUuid());
                    }
                    // A stale apply/lease callback must not restart a later pause or exit.
                    state.setPermitAuthorized(false);
                }
            }
            em.flush();
            return parent.toInventory();
        });
        } catch (IllegalArgumentException invalid) {
            throw translatePolicyRuleFailure(invalid);
        }
    }

    static RuntimeException translatePolicyRuleFailure(IllegalArgumentException invalid) {
        // PolicyRules is intentionally usable without the repository and
        // reports stable MEMORY_* prefixes as IllegalArgumentException.
        // Crossing the API/repository boundary must preserve the platform
        // MemoryOperationException contract for those public errors.
        String message = invalid.getMessage();
        if (message != null && message.startsWith("MEMORY_")) {
            int separator = message.indexOf(':');
            if (separator > 0) {
                String code = message.substring(0, separator).trim();
                if (code.matches("MEMORY_[A-Z0-9_]+")) {
                    return error(code, message.substring(separator + 1).trim());
                }
            }
        }
        return invalid;
    }

    private void validateUncertainRecovery(EntityManager em, MemoryStateVO state, APIUpdateMemoryPolicyMsg msg) {
        MemoryTaskVO original = em.find(MemoryTaskVO.class, msg.getExpectedControlOperationUuid());
        MemoryTaskVO active = state.getActiveTaskUuid() == null ? null
                : em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
        boolean activeOriginal = Objects.equals(state.getActiveTaskUuid(), msg.getExpectedControlOperationUuid());
        boolean activeReadOnlyAlias = isConfigurationReconcile(active)
                && "Unknown".equals(active.getStatus())
                && Objects.equals(active.getHostUuid(), state.getHostUuid())
                && Objects.equals(active.getResourceUuid(), state.getHostUuid())
                && Objects.equals(active.getReconcileOperationUuid(), msg.getExpectedControlOperationUuid())
                && Objects.equals(active.getExpectedControlOperationUuid(), msg.getExpectedControlOperationUuid())
                && original != null && active.getDesiredRevision() == original.getDesiredRevision()
                && active.getDesiredRevision() == state.getDesiredRevision();
        String drainUuid = msg.getRecovery().getDrainControlOperationUuid();
        MemoryTaskVO drain = drainUuid == null ? null : em.find(MemoryTaskVO.class, drainUuid);
        String boot = null;
        try { boot = (String) JSONObjectUtil.toObject(state.getState(), Map.class).get("bootId"); }
        catch (RuntimeException ignored) { }
        long now = System.currentTimeMillis();
        if ((!activeOriginal && !activeReadOnlyAlias)
                || !Objects.equals(state.getControlOperationUuid(), msg.getExpectedControlOperationUuid())
                || original == null || !"Unknown".equals(original.getStatus())
                || !Arrays.asList("apply", "clearOverride").contains(original.getAction())
                || !"Host".equals(original.getScope()) || !Objects.equals(state.getHostUuid(), original.getHostUuid())
                || (drainUuid != null && (drain == null || !"drain".equals(drain.getAction())
                    || !"Succeeded".equals(drain.getStatus()) || !Objects.equals(state.getHostUuid(), drain.getHostUuid())))
                || (drainUuid == null && (!"apply".equals(original.getAction())
                    || original.getDesiredRevision() != state.getDesiredRevision()))
                || !Objects.equals(boot, msg.getRecovery().getExpectedHostBootId())
                || state.getLastSampleTime() == null || state.getLastSampleTime() > now + 5000
                || now - state.getLastSampleTime() > MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            throw error("MEMORY_UNCERTAIN_RECOVERY_FENCED",
                    "Refresh the same-boot Host state and its exact unresolved apply/drain before recovery");
        }
    }

    private static final class HostPolicyPlan {
        String targetHash;
        String planHash;
        String payload;
        String payloadHash;
        String blockedFields;
        Map<String, String> changedFields;
        boolean hasPayload;
        boolean hasBlockers;
        boolean replacesAppliedPolicy;
        boolean maintenanceActivation;
        boolean writebackDisableOnly;
        boolean confirmedKsmReconnect;
        boolean rebootReconfirmation;
        String expectedControlOperationUuid;
        String capacityMaintenance;
    }

    private JsonObject backendMaintenanceRequest(EntityManager em, APIUpdateMemoryPolicyMsg msg,
            MemoryStateVO state, MemoryAgentResponse observation) {
        MemoryBackendPreparation request = msg.getBackendPreparation();
        long now = System.currentTimeMillis();
        MemoryWritebackBackendInventory inventory;
        try {
            if (!observation.isSuccess() || !"Succeeded".equals(observation.status)) {
                throw new IllegalArgumentException("WRITEBACK_INVENTORY_UNAVAILABLE");
            }
            inventory = MemoryWritebackBackendInventory.fromAgentState(
                    state.getHostUuid(), observation.hostUuid, observation.state);
            long sampled = java.time.OffsetDateTime.parse(inventory.getObservedAt()).toInstant().toEpochMilli();
            if (sampled > now || now - sampled >= Math.min(90000L, MemoryOptimizationGlobalConfig.displayTtlMillis())
                    || !Arrays.asList("AVAILABLE", "PARTIAL").contains(inventory.getStatus())
                    || !Objects.equals(request.getExpectedHostBootId(), inventory.getBootId())
                    || !Objects.equals(request.getExpectedPoolGeneration(), inventory.getPoolGeneration())
                    || !Objects.equals(state.getControlOperationUuid(), inventory.getControlOperationUuid())
                    || hasUnresolvedControl(em, state)) {
                throw new IllegalArgumentException("WRITEBACK_INVENTORY_IDENTITY_OR_CONTROL_MISMATCH");
            }
            JsonObject selected = null;
            Object candidates = observation.state.get("candidates");
            if (candidates instanceof List) {
                for (Object entry : (List<?>) candidates) {
                    Map<?, ?> candidate = MemoryWritebackBackendInventory.map(entry);
                    if (candidate == null || !Objects.equals(request.getCandidateId(), candidate.get("identity"))) { continue; }
                    Map<?, ?> identity = MemoryWritebackBackendInventory.map(candidate.get("stableIdentity"));
                    if (selected != null || !"block".equals(candidate.get("type"))
                            || !Boolean.TRUE.equals(candidate.get("eligible"))
                            || !Boolean.FALSE.equals(candidate.get("writebackReady"))
                            || !"NEEDS_OFFLINE_PREPARATION".equals(candidate.get("state"))
                            || candidate.get("resourceUuid") != null
                            || !Objects.equals("candidate:" + candidate.get("fingerprint"), request.getCandidateId())
                            || !Objects.equals(inventory.getBootId(), candidate.get("hostBootId"))
                            || !numberEquals(candidate.get("capacityBytes"), request.getBackendCapacityBytes())
                            || identity == null || !Objects.equals(inventory.getBootId(), identity.get("bootId"))
                            || !Objects.equals(candidate.get("device"), identity.get("device"))
                            || !numberEquals(identity.get("capacityBytes"), request.getBackendCapacityBytes())) {
                        throw new IllegalArgumentException("WRITEBACK_CANDIDATE_NOT_PREPARABLE");
                    }
                    for (String field : Arrays.asList("device", "majorMinor", "sysfsPath", "sysfsInode", "rdev", "nodeInode")) {
                        if (identity.get(field) == null || "".equals(identity.get(field))) {
                            throw new IllegalArgumentException("WRITEBACK_CANDIDATE_IDENTITY_INCOMPLETE");
                        }
                    }
                    selected = new JsonParser().parse(JSONObjectUtil.toJsonString(candidate)).getAsJsonObject();
                }
            }
            if (selected == null) { throw new IllegalArgumentException("WRITEBACK_CANDIDATE_NOT_UNIQUE"); }
            if (inventory.getCanPrepare() == null) {
                throw new IllegalArgumentException("BACKEND_PREPARATION_ELIGIBILITY_UNKNOWN");
            }
            JsonObject observed = new JsonParser().parse(state.getState()).getAsJsonObject();
            if ("none".equals(inventory.getPoolGeneration())
                    && !isFirstPoolNonZramControl(em, state, inventory.getBootId())) {
                throw new IllegalArgumentException("FIRST_POOL_CONTROL_NOT_ELIGIBLE");
            }
            boolean firstUseServiceExit = Boolean.FALSE.equals(inventory.getCanPrepare())
                    && "none".equals(inventory.getPoolGeneration())
                    && Collections.singletonList("MEMORY_SERVICE_NOT_STOPPED").equals(inventory.getPreparationReasons());
            if (firstUseServiceExit) {
                String observedPool = getString(observed, "poolGeneration");
                JsonElement efficiency = jsonPath(observed, "actual.zram.efficiencyEnabled");
                if (standardMutationDeferReason(em, state.getHostUuid(), state, now, false) != null
                        || !Objects.equals(inventory.getBootId(), getString(observed, "bootId"))
                        || observedPool != null && !"none".equals(observedPool)
                        || efficiency == null || !efficiency.isJsonPrimitive()
                        || !efficiency.getAsJsonPrimitive().isBoolean() || efficiency.getAsBoolean()) {
                    throw new IllegalArgumentException("FIRST_USE_SERVICE_EXIT_NOT_ELIGIBLE");
                }
                // The Agent revalidates the empty first-use state and exits
                // its verified controller inside the same preparation operation.
                // There is no pool to drain and no drain proof to manufacture.
            }
            boolean drain = !Boolean.TRUE.equals(inventory.getCanPrepare()) && !firstUseServiceExit;
            if (drain && (!state.toInventory().getAllowedActions().contains("drain")
                    || standardMutationDeferReason(em, state.getHostUuid(), state, now, false) != null
                    || !Objects.equals(inventory.getBootId(), getString(observed, "bootId"))
                    || !Objects.equals(inventory.getPoolGeneration(), getString(observed, "poolGeneration"))
                    || inventory.getPreparationReasons() == null
                    || inventory.getPreparationReasons().isEmpty()
                    || !inventory.getPreparationReasons().stream().allMatch(reason -> Arrays.asList(
                            "MEMORY_SERVICE_NOT_STOPPED", "DRAIN_PROOF_UNAVAILABLE").contains(reason)))) {
                throw new IllegalArgumentException("BACKEND_MAINTENANCE_NOT_ELIGIBLE");
            }
            JsonObject context = new JsonObject();
            JsonObject target = explicitlyManagedPolicy(em, state.getHostUuid(), effective(em, "Host", state.getHostUuid()));
            context.add("target", target); context.addProperty("targetHash", digest(target.toString()));
            context.addProperty("bootId", inventory.getBootId());
            context.addProperty("poolGeneration", inventory.getPoolGeneration());
            JsonElement nativeDevice = jsonPath(observed, "host_zram.device");
            if (nativeDevice != null && nativeDevice.isJsonPrimitive()) {
                context.add("device", nativeDevice.deepCopy());
            }
            context.addProperty("drainRequired", drain); context.add("selectedBackend", selected);
            context.add("backendPreparation", new JsonParser().parse(JSONObjectUtil.toJsonString(request)));
            return context;
        } catch (RuntimeException invalid) {
            throw error("MEMORY_BACKEND_PREPARATION_INVALID", "Backend maintenance admission failed: " + invalid.getMessage());
        }
    }

    private HostPolicyPlan planForHost(EntityManager em, String hostUuid, String effectivePolicy,
            MemoryStateVO state) {
        return planForHost(em, hostUuid, state, explicitlyManagedPolicy(em, hostUuid, effectivePolicy), effectivePolicy);
    }

    private boolean isFirstPoolNonZramControl(EntityManager em, MemoryStateVO state, String expectedBootId) {
        if (state.getControlOperationUuid() == null) { return true; }
        MemoryTaskVO control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
        if (control == null || "VM".equals(control.getScope())
                || !Objects.equals(state.getHostUuid(), control.getHostUuid())
                || !"Succeeded".equals(control.getStatus())
                || !Arrays.asList("apply", "clearOverride").contains(control.getAction())
                || state.getAppliedRevision() == null || control.getDesiredRevision() != state.getAppliedRevision()) {
            return false;
        }
        try {
            JsonObject policy = new JsonParser().parse(control.getPolicy()).getAsJsonObject();
            if (policy.has("ksm") && policy.get("ksm").isJsonObject()
                    && policy.entrySet().stream().allMatch(field -> Arrays.asList("schemaVersion", "ksm").contains(field.getKey()))) {
                return true;
            }
            // A completed disabled-only service write also precedes a pool.
            // Unlike historical KSM-only ownership, it must still have a
            // fresh, exact current Agent acknowledgment and disabled values.
            // The Agent independently checks its durable CPU/tuning receipts
            // and physical no-pool proof before and after stopping the service.
            if (!"Host".equals(control.getScope())
                    || !policy.entrySet().stream().allMatch(field -> Arrays.asList(
                    "schemaVersion", "ksm", "zram", "writeback").contains(field.getKey()))
                    || policy.has("ksm") && !policy.get("ksm").isJsonObject()
                    || !hasFreshCapabilityObservation(state, System.currentTimeMillis())) { return false; }
            boolean disabledTarget = false;
            String boundBackendUuid = null;
            Long boundBackendCapacity = null;
            for (String section : Arrays.asList("zram", "writeback")) {
                if (!policy.has(section)) { continue; }
                JsonObject fields = policy.getAsJsonObject(section);
                if (fields.entrySet().isEmpty()) { continue; }
                JsonElement enabled = fields.get("enabled");
                if (enabled == null || !enabled.isJsonPrimitive()
                        || !enabled.getAsJsonPrimitive().isBoolean() || enabled.getAsBoolean()) { return false; }
                if ("writeback".equals(section)
                        && (fields.has("backendResourceUuid") || fields.has("backendCapacityBytes"))) {
                    JsonElement backendUuid = fields.get("backendResourceUuid");
                    boundBackendCapacity = exactJsonLong(fields.get("backendCapacityBytes"));
                    if (fields.entrySet().size() != 3 || backendUuid == null || !backendUuid.isJsonPrimitive()
                            || !backendUuid.getAsJsonPrimitive().isString()
                            || !MemoryPolicyRules.validUuid(backendUuid.getAsString())
                            || boundBackendCapacity == null || boundBackendCapacity <= 0) { return false; }
                    boundBackendUuid = backendUuid.getAsString();
                } else if (fields.entrySet().size() != 1) {
                    return false;
                }
                disabledTarget = true;
            }
            JsonObject current = new JsonParser().parse(state.getState()).getAsJsonObject();
            if (!disabledTarget || getString(current, "bootId") == null
                    || getString(current, "bootId").trim().isEmpty()
                    || !Objects.equals(expectedBootId, getString(current, "bootId"))
                    || !Objects.equals(control.getUuid(), getString(current, "lastConfirmedOperationUuid"))
                    || !Objects.equals(state.getAppliedRevision(), exactJsonLong(current.get("lastConfirmedAppliedRevision")))) {
                return false;
            }
            for (String field : Arrays.asList("actual.zram.efficiencyEnabled", "actual.writeback.tuning.enabled")) {
                JsonElement enabled = jsonPath(current, field);
                if (enabled == null || !enabled.isJsonPrimitive()
                        || !enabled.getAsJsonPrimitive().isBoolean() || enabled.getAsBoolean()) { return false; }
            }
            if (boundBackendUuid != null) {
                JsonElement ksmOnly = current.get("ksmOnly");
                if (ksmOnly == null || !ksmOnly.isJsonPrimitive()
                        || !ksmOnly.getAsJsonPrimitive().isBoolean() || !ksmOnly.getAsBoolean()) { return false; }
                JsonElement stateBackend = current.get("backend_resource");
                JsonElement actualBackend = jsonPath(current, "actual.writeback.backend.backend_resource");
                for (JsonElement backend : Arrays.asList(stateBackend, actualBackend)) {
                    if (backend == null || !backend.isJsonObject()) { return false; }
                    JsonObject resource = backend.getAsJsonObject();
                    JsonElement active = resource.get("active");
                    if (!Objects.equals(boundBackendUuid, getString(resource, "resource_uuid"))
                            || !Objects.equals(boundBackendCapacity, exactJsonLong(resource.get("capacity_bytes")))
                            || !"PREPARED".equals(getString(resource, "state"))
                            || active == null || !active.isJsonPrimitive()
                            || !active.getAsJsonPrimitive().isBoolean() || active.getAsBoolean()) { return false; }
                }
            }
            return true;
        } catch (RuntimeException invalid) { return false; }
    }

    private HostPolicyPlan planForHost(EntityManager em, String hostUuid, MemoryStateVO state, JsonObject target, String effectivePolicy) {
        return planForHost(em, hostUuid, state, target, effectivePolicy, false);
    }

    /** A drained Host is reactivated only by an explicit recovery, which passes allowDrainActivation. */
    private HostPolicyPlan planForHost(EntityManager em, String hostUuid, MemoryStateVO state, JsonObject target,
            String effectivePolicy, boolean allowDrainActivation) {
        HostPolicyPlan plan = new HostPolicyPlan();
        String targetText = target.toString();
        plan.targetHash = digest(targetText);
        String applied = state.getAppliedPolicy() == null ? "{}" : state.getAppliedPolicy();
        plan.changedFields = MemoryPolicyAdmissionRules.changedFields(applied, targetText);
        // schemaVersion is wire metadata, not a Host policy field. It must not
        // make an otherwise KSM-only desired change fail capability admission.
        plan.changedFields.remove("schemaVersion");
        ignoreAbsentDisabledMechanismDefaults(applied, target, effectivePolicy, plan.changedFields);
        long now = System.currentTimeMillis();
        if (plan.changedFields.isEmpty()) {
            plan.confirmedKsmReconnect = addTrustedAppliedKsmReconnectDrift(
                    em, hostUuid, state, target, now, plan.changedFields);
            if (!plan.confirmedKsmReconnect) {
                addTrustedDisabledKsmActualDrift(em, hostUuid, state, target, now, plan.changedFields);
            } else {
                plan.expectedControlOperationUuid = state.getControlOperationUuid();
            }
        }
        if (plan.changedFields.isEmpty()) {
            plan.rebootReconfirmation = addRebootReconfirmation(state, target, now, plan.changedFields);
        }
        // A successful, same-boot offline drain permits only the existing
        // initialization contract. Its first activation must keep writeback
        // off. This changes the execution payload, never the desired rows.
        boolean activation = false;
        JsonElement desiredZramEnabled = jsonPath(target, "zram.enabled");
        if (state.getActiveTaskUuid() == null && state.getControlOperationUuid() != null
                && desiredZramEnabled != null && desiredZramEnabled.getAsBoolean()
                && hasFreshCapabilityObservation(state, now)
                && MemoryPolicyAdmissionRules.capabilityReason(state, "zram", now) == null) {
            MemoryTaskVO control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
            // After a reboot the Agent no longer confirms the previous boot's drain
            // journal; its maintenance proof is bound to the drain instead.
            boolean drainReady = allowDrainActivation && isPauseOrDrain(control, hostUuid) && "drain".equals(control.getAction())
                    && isFreshPausedSample(state, now)
                    && isMaintenanceRecoveryState(state.getState(), control.getUuid());
            if (drainReady || rejectedMaintenanceActivationReady(em, state, control)) {
                try {
                    JsonObject proof = new JsonParser().parse(state.getState()).getAsJsonObject()
                            .getAsJsonObject("maintenanceProof");
                    activation = getBoolean(proof, "readyForInitialization")
                            && getBoolean(proof, "originalDeviceInactive")
                            && getBoolean(proof, "oldPoolOwnershipAbsent")
                            && Long.valueOf(0L).equals(getLong(proof, "activeOperations"))
                            && proof.has("poolOwnedByService") && !getBoolean(proof, "poolOwnedByService")
                            && getString(proof, "newPoolGeneration") == null;
                } catch (RuntimeException invalid) { activation = false; }
            }
        }
        if (activation) {
            // Restoration can write unchanged retained settings as well.
            // Validate those fields against the current capabilities too.
            plan.changedFields.putAll(MemoryPolicyAdmissionRules.changedFields("{}", targetText));
            plan.changedFields.remove("schemaVersion");
            plan.changedFields.put("zram.enabled", "true");
        }
        // An absent field cannot be represented by a delta: omission means
        // leave it untouched. Persist a complete sparse target for a feature
        // clear and use the existing effective/clear Agent proof contract.
        plan.replacesAppliedPolicy = plan.changedFields.keySet().stream()
                .anyMatch(path -> (path.startsWith("ksm.") || "zram.enabled".equals(path)
                        || "writeback.enabled".equals(path)) && jsonPath(target, path) == null);
        if (plan.replacesAppliedPolicy && !target.has("ksm")) { target.add("ksm", new JsonObject()); }
        boolean writebackChange = plan.changedFields.keySet().stream()
                .anyMatch(path -> path.startsWith("writeback."));
        Map<String, String> blocked = MemoryPolicyAdmissionRules.blockedFields(
                plan.changedFields, state, now,
                (plan.replacesAppliedPolicy || writebackChange) ? target.toString() : null);

        JsonObject payload = plan.replacesAppliedPolicy ? target.deepCopy() : new JsonObject();
        payload.addProperty("schemaVersion", 1);
        JsonObject blockers = new JsonObject();
        Set<String> inspectedFields = new TreeSet<>(plan.changedFields.keySet());
        inspectedFields.addAll(blocked.keySet());
        for (String field : inspectedFields) {
            JsonElement value = jsonPath(target, field);
            String reason = blocked.get(field);
            if (reason != null) {
                JsonObject evidence = new JsonObject();
                evidence.add("desired", value == null ? JsonNull.INSTANCE : value.deepCopy());
                evidence.addProperty("reason", reason);
                blockers.add(field, evidence);
            } else if (value != null && !plan.replacesAppliedPolicy) {
                setJsonPath(payload, field, value.deepCopy());
            }
        }
        JsonObject desiredKsm = target.getAsJsonObject("ksm");
        if (!plan.replacesAppliedPolicy && !payload.has("ksm") && payload.entrySet().size() > 1
                && !(payload.size() == 2 && payload.has("writeback")
                    && payload.getAsJsonObject("writeback").size() == 1
                    && !jsonPath(payload, "writeback.enabled").getAsBoolean())
                && desiredKsm != null && !desiredKsm.entrySet().isEmpty()
                && desiredKsm.equals(new JsonParser().parse(applied).getAsJsonObject().get("ksm"))
                && blocked.keySet().stream().noneMatch(field -> field.startsWith("ksm."))) {
            // Agent pause/drain stops KSM only through the latest applied
            // operation's KSM section. Carry the unchanged applied KSM target
            // so a ZRAM-only update keeps KSM under that control.
            payload.add("ksm", desiredKsm.deepCopy());
        }
        if (!plan.replacesAppliedPolicy && payload.has("ksm")) {
            // Applying any KSM field also writes its scan settings on the
            // Agent. Carry their effective values, as for writeback below,
            // so a switch-only delta cannot reset an existing non-default.
            // Keep run/zero switches sparse and never reintroduce a blocked
            // value or persist an inherited setting as a Host override.
            for (String field : Arrays.asList("ksm.pagesToScan", "ksm.sleepMillis")) {
                JsonElement value = jsonPath(target, field);
                if (value != null && !blocked.containsKey(field)) {
                    setJsonPath(payload, field, value.deepCopy());
                }
            }
        }
        if (activation && blocked.keySet().stream().allMatch(field -> field.startsWith("writeback."))) {
            // Drain also paused KSM. Restore its retained target even when
            // its desired values did not change relative to appliedPolicy.
            payload = target.deepCopy();
            payload.addProperty("schemaVersion", 1);
            JsonObject disabled = new JsonObject(); disabled.addProperty("enabled", false);
            payload.add("writeback", disabled);
            plan.maintenanceActivation = true;
            JsonElement requested = jsonPath(target, "writeback.enabled");
            if (requested != null && requested.getAsBoolean()) {
                JsonObject evidence = new JsonObject(); evidence.add("desired", requested.deepCopy());
                evidence.addProperty("reason", "MAINTENANCE_ACTIVATION_WRITEBACK_MUST_REMAIN_DISABLED");
                blockers.add("writeback.enabled", evidence);
            }
        } else if (payload.has("writeback") && !blocked.containsKey("writeback.enabled")) {
            // Agent validates the complete backend even when only the parent
            // switch/tuning changed. Do not materialize inherited values as
            // Host overrides merely to satisfy that execution contract.
            JsonElement enabled = jsonPath(target, "writeback.enabled");
            if (enabled != null && enabled.getAsBoolean()) {
                payload.add("writeback", target.getAsJsonObject("writeback").deepCopy());
            }
        }
        plan.hasPayload = payload.entrySet().size() > 1;
        JsonObject writebackPayload = payload.has("writeback") ? payload.getAsJsonObject("writeback") : null;
        plan.writebackDisableOnly = payload.size() == 2 && writebackPayload != null
                && writebackPayload.size() == 1 && writebackPayload.has("enabled")
                && !writebackPayload.get("enabled").getAsBoolean();
        plan.payload = plan.hasPayload ? payload.toString() : "{}";
        plan.payloadHash = plan.hasPayload ? digest(plan.payload) : null;
        plan.blockedFields = blockers.toString();
        plan.hasBlockers = blockers.size() > 0;
        plan.planHash = digest(plan.targetHash + "|" + String.valueOf(plan.payloadHash) + "|" + plan.blockedFields);
        if (!plan.hasBlockers && !plan.maintenanceActivation) {
            plan.capacityMaintenance = capacityMaintenanceRequest(state, target, plan.targetHash, plan.changedFields);
        }
        return plan;
    }

    /** Keep the admitted target and original native identity on the drain task.
     * The existing drain wire command remains policy={}; later preparation
     * must prove this same device before the normal target can be restored. */
    private static String capacityMaintenanceRequest(MemoryStateVO state, JsonObject target, String targetHash,
            Map<String, String> changed) {
        if (state == null || (!changed.containsKey("zram.logicalCapacityBytes")
                && !changed.containsKey("zram.ramLimitBytes"))
                || !Boolean.TRUE.equals(MemoryStandardConfigCodec.get(target.toString(), "zram.enabled"))
                || !state.toInventory().getAllowedActions().contains("drain")) { return null; }
        try {
            JsonObject observed = new JsonParser().parse(state.getState()).getAsJsonObject();
            if (getBoolean(observed.getAsJsonObject("lifecycle"), "paused")) { return null; }
            String boot = getString(observed, "bootId"), generation = getString(observed, "poolGeneration");
            if (boot == null || !boot.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")
                    || generation == null || !generation.matches("[0-9a-fA-F]{64}")) { return null; }
            JsonObject actual = observed.getAsJsonObject("actual");
            JsonObject zram = actual.getAsJsonObject("zram"), capacity = zram.getAsJsonObject("capacity");
            if (!getBoolean(zram, "efficiencyEnabled") || capacity == null) { return null; }
            Long logical = exactJsonLong(capacity.get("effective_logical_bytes"));
            Long physical = exactJsonLong(capacity.get("effective_physical_limit_bytes"));
            if (logical == null || logical <= 0 || physical == null || physical <= 0) { return null; }
            Long desiredLogical = exactJsonLong(jsonPath(target, "zram.logicalCapacityBytes"));
            Long desiredPhysical = exactJsonLong(jsonPath(target, "zram.ramLimitBytes"));
            boolean resize = changed.containsKey("zram.logicalCapacityBytes") && desiredLogical != null
                    && !desiredLogical.equals(logical)
                    || changed.containsKey("zram.ramLimitBytes") && desiredPhysical != null
                    && !desiredPhysical.equals(physical);
            if (!resize) { return null; }
            JsonObject request = new JsonObject();
            request.add("target", target.deepCopy());
            request.addProperty("targetHash", targetHash);
            request.addProperty("bootId", boot); request.addProperty("poolGeneration", generation);
            request.addProperty("device", getString(observed.getAsJsonObject("host_zram"), "device"));
            // The Agent reports the service backend view; keep its resource, as admission does.
            JsonElement backend = jsonPath(actual, "writeback.backend.backend_resource");
            if (backend == null) { backend = jsonPath(actual, "writeback.backend"); }
            if (backend != null) { request.add("backend", backend.deepCopy()); }
            return request.toString();
        } catch (RuntimeException unavailable) { return null; }
    }

    /**
     * A durable applied policy can outlive an out-of-band KSM parameter reset.
     * Reconcile only a fresh, successful same-boot observation anchored to the
     * exact applied control revision. This path deliberately handles only a
     * disabled KSM target: it may restore scan/zero-page settings, but never
     * starts KSM as a side effect of reconnect reconciliation.
     */
    private static void addTrustedDisabledKsmActualDrift(EntityManager em, String hostUuid,
            MemoryStateVO state, JsonObject target, long now, Map<String, String> changedFields) {
        JsonElement enabled = jsonPath(target, "ksm.enabled");
        if (enabled == null || !enabled.isJsonPrimitive() || !enabled.getAsJsonPrimitive().isBoolean()
                || enabled.getAsBoolean() || state == null || !"Succeeded".equals(state.getStatus())
                || state.getActiveTaskUuid() != null || state.getAppliedRevision() == null
                || state.getAppliedRevision() != state.getDesiredRevision()
                || !hasFreshCapabilityObservation(state, now)
                || MemoryCapabilityRules.hasPartialKsmMarker(state)
                || MemoryPolicyAdmissionRules.capabilityReason(state, "ksm", now) != null
                || state.getControlOperationUuid() == null) { return; }

        try {
            JsonObject root = new JsonParser().parse(state.getState()).getAsJsonObject();
            JsonElement boot = root.get("bootId");
            JsonElement confirmed = root.get("lastConfirmedOperationUuid");
            JsonElement confirmedRevision = root.get("lastConfirmedAppliedRevision");
            JsonObject actual = root.getAsJsonObject("actual");
            JsonObject ksm = actual == null ? null : actual.getAsJsonObject("ksm");
            if (boot == null || !boot.isJsonPrimitive() || !boot.getAsJsonPrimitive().isString()
                    || boot.getAsString().trim().isEmpty()
                    || confirmed == null || !confirmed.isJsonPrimitive()
                    || !confirmed.getAsString().equals(state.getControlOperationUuid())
                    || !Objects.equals(exactJsonLong(confirmedRevision), state.getAppliedRevision()) || ksm == null
                    || !ksm.has("enabled") || !ksm.get("enabled").isJsonPrimitive()
                    || !ksm.get("enabled").getAsJsonPrimitive().isBoolean()
                    || ksm.get("enabled").getAsBoolean()) { return; }

            MemoryTaskVO control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
            if (control == null || !"Host".equals(control.getScope())
                    || !hostUuid.equals(control.getHostUuid()) || !"Succeeded".equals(control.getStatus())
                    || !Arrays.asList("apply", "clearOverride").contains(control.getAction())
                    || control.getDesiredRevision() != state.getAppliedRevision()) { return; }

            Map<String, String> actualDrift = new LinkedHashMap<>();
            JsonElement pages = jsonPath(target, "ksm.pagesToScan");
            JsonElement sleep = jsonPath(target, "ksm.sleepMillis");
            if (pages != null) {
                Long expected = exactJsonLong(pages), observed = exactJsonLong(ksm.get("pagesToScan"));
                if (expected == null || observed == null) { return; }
                if (!expected.equals(observed)) { actualDrift.put("ksm.pagesToScan", pages.toString()); }
            }
            if (sleep != null) {
                Long expected = exactJsonLong(sleep), observed = exactJsonLong(ksm.get("sleepMillis"));
                if (expected == null || observed == null) { return; }
                if (!expected.equals(observed)) { actualDrift.put("ksm.sleepMillis", sleep.toString()); }
            }
            JsonElement zero = jsonPath(target, "ksm.zeroPagesEnabled");
            if (zero != null) {
                if (!zero.isJsonPrimitive() || !zero.getAsJsonPrimitive().isBoolean()
                        || MemoryPolicyAdmissionRules.capabilityReason(state, "ksmZeroPages", now) != null
                        || !ksm.has("zeroPagesEnabled") || !ksm.get("zeroPagesEnabled").isJsonPrimitive()
                        || !ksm.get("zeroPagesEnabled").getAsJsonPrimitive().isBoolean()) { return; }
                if (zero.getAsBoolean() != ksm.get("zeroPagesEnabled").getAsBoolean()) {
                    actualDrift.put("ksm.zeroPagesEnabled", zero.toString());
                }
            }
            changedFields.putAll(actualDrift);
        } catch (RuntimeException invalidOrIncompleteObservation) {
            // A malformed or incomplete read is not evidence that a field was
            // reset. Keep it out of the write plan and preserve existing state.
        }
    }

    /**
     * After a Host reboot the Agent reports no same-boot confirmation. Re-issue
     * the unchanged applied ZRAM settings (the service rebuilt the pool) and the
     * applied KSM settings the kernel reset, so the Agent confirms them for this
     * boot; the recorded plan hash keeps it to once.
     */
    private static boolean addRebootReconfirmation(MemoryStateVO state, JsonObject target, long now,
            Map<String, String> changedFields) {
        if (!"Succeeded".equals(state.getStatus()) || state.getActiveTaskUuid() != null
                || state.getAppliedRevision() == null || state.getAppliedRevision() != state.getDesiredRevision()
                || state.getAppliedPolicy() == null || !hasFreshCapabilityObservation(state, now)) { return false; }
        try {
            JsonObject observed = new JsonParser().parse(state.getState()).getAsJsonObject();
            JsonElement bootId = observed.get("bootId");
            if (bootId == null || !bootId.isJsonPrimitive() || !bootId.getAsJsonPrimitive().isString()
                    || bootId.getAsString().trim().isEmpty()
                    || observed.has("lastConfirmedOperationUuid")
                    || observed.has("lastConfirmedAppliedRevision")) { return false; }
            JsonObject applied = new JsonParser().parse(state.getAppliedPolicy()).getAsJsonObject();
            JsonObject reissue = new JsonObject();
            JsonObject appliedZram = applied.getAsJsonObject("zram");
            JsonObject desiredZram = target.getAsJsonObject("zram");
            JsonElement enabled = appliedZram == null ? null : appliedZram.get("enabled");
            if (desiredZram != null && desiredZram.equals(appliedZram) && enabled != null
                    && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean() && enabled.getAsBoolean()
                    && MemoryPolicyAdmissionRules.capabilityReason(state, "zram", now) == null) {
                reissue.add("zram", desiredZram);
            }
            JsonObject appliedKsm = applied.getAsJsonObject("ksm");
            JsonObject desiredKsm = target.getAsJsonObject("ksm");
            JsonObject actualRoot = observed.getAsJsonObject("actual");
            JsonObject actualKsm = actualRoot == null ? null : actualRoot.getAsJsonObject("ksm");
            if (desiredKsm != null && !desiredKsm.entrySet().isEmpty() && desiredKsm.equals(appliedKsm)
                    && actualKsm != null && MemoryPolicyAdmissionRules.capabilityReason(state, "ksm", now) == null
                    && (!desiredKsm.has("zeroPagesEnabled")
                        || MemoryPolicyAdmissionRules.capabilityReason(state, "ksmZeroPages", now) == null)) {
                for (Map.Entry<String, JsonElement> entry : desiredKsm.entrySet()) {
                    JsonElement actual = actualKsm.get(entry.getKey());
                    if (actual != null && !actual.equals(entry.getValue())) {
                        reissue.add("ksm", desiredKsm);
                        break;
                    }
                }
            }
            changedFields.putAll(MemoryPolicyAdmissionRules.changedFields("{}", reissue.toString()));
            return !changedFields.isEmpty();
        } catch (RuntimeException invalid) { return false; }
    }

    /**
     * A reconnect restores the applied KSM target only when the last confirmed
     * Host delta agrees. The Agent independently proves the complete target
     * from that operation's native readback while holding the Host writer lock.
     */
    private static boolean addTrustedAppliedKsmReconnectDrift(EntityManager em, String hostUuid,
            MemoryStateVO state, JsonObject target, long now, Map<String, String> changedFields) {
        if (state == null || state.getStatus() == null || !"Succeeded".equals(state.getStatus())
                || state.getActiveTaskUuid() != null || state.getAppliedRevision() == null
                || state.getAppliedRevision() != state.getDesiredRevision()
                || state.getControlOperationUuid() == null || state.getAppliedPolicyHash() == null
                || state.getAppliedPolicy() == null || !hasFreshCapabilityObservation(state, now)
                || MemoryCapabilityRules.hasPartialKsmMarker(state)
                || MemoryPolicyAdmissionRules.capabilityReason(state, "ksm", now) != null) { return false; }

        try {
            JsonObject observed = new JsonParser().parse(state.getState()).getAsJsonObject();
            JsonElement bootId = observed.get("bootId");
            // The current Agent reports lastConfirmed* only when its durable
            // operation belongs to this boot. Reconnect restoration is only
            // considered when a fresh observation has no same-boot anchor;
            // a conflicting/partial anchor is not equivalent to absence.
            if (bootId == null || !bootId.isJsonPrimitive() || !bootId.getAsJsonPrimitive().isString()
                    || bootId.getAsString().trim().isEmpty()
                    || observed.has("lastConfirmedOperationUuid")
                    || observed.has("lastConfirmedAppliedRevision")) { return false; }
            JsonObject actualRoot = observed.getAsJsonObject("actual");
            JsonObject actualKsm = actualRoot == null ? null : actualRoot.getAsJsonObject("ksm");
            if (actualKsm == null) { return false; }

            MemoryTaskVO control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
            if (control == null || !"Host".equals(control.getScope()) || !hostUuid.equals(control.getHostUuid())
                    || !"Succeeded".equals(control.getStatus())
                    || !Arrays.asList("apply", "clearOverride").contains(control.getAction())
                    || control.getDesiredRevision() != state.getAppliedRevision()
                    || !Objects.equals(state.getAppliedPolicyHash(), digest(control.getPolicy()))) { return false; }

            JsonObject savedApplied = new JsonParser().parse(state.getAppliedPolicy()).getAsJsonObject();
            JsonObject appliedKsm = savedApplied.getAsJsonObject("ksm");
            JsonObject desiredKsm = target.getAsJsonObject("ksm");
            JsonObject confirmedTaskPolicy = new JsonParser().parse(control.getPolicy()).getAsJsonObject();
            JsonObject confirmedTaskKsm = confirmedTaskPolicy.getAsJsonObject("ksm");
            if (appliedKsm == null || appliedKsm.entrySet().isEmpty() || desiredKsm == null
                    || !appliedKsm.equals(desiredKsm) || confirmedTaskKsm == null
                    || confirmedTaskKsm.entrySet().isEmpty() || !appliedKsm.has("enabled")
                    || !appliedKsm.get("enabled").isJsonPrimitive()
                    || !appliedKsm.get("enabled").getAsJsonPrimitive().isBoolean()
                    || exactJsonLong(appliedKsm.get("pagesToScan")) == null
                    || exactJsonLong(appliedKsm.get("sleepMillis")) == null) {
                return false;
            }
            for (Map.Entry<String, JsonElement> entry : confirmedTaskKsm.entrySet()) {
                if (!Arrays.asList("enabled", "pagesToScan", "sleepMillis", "zeroPagesEnabled").contains(entry.getKey())
                        || !entry.getValue().equals(appliedKsm.get(entry.getKey()))) { return false; }
            }
            JsonObject restoreKsm = new JsonObject();
            for (String field : Arrays.asList("enabled", "pagesToScan", "sleepMillis")) {
                restoreKsm.add(field, appliedKsm.get(field));
            }
            if (confirmedTaskKsm.has("zeroPagesEnabled")) {
                restoreKsm.add("zeroPagesEnabled", confirmedTaskKsm.get("zeroPagesEnabled"));
            }
            boolean anyActualDrift = false;
            for (Map.Entry<String, JsonElement> entry : restoreKsm.entrySet()) {
                String field = entry.getKey();
                JsonElement saved = entry.getValue();
                if ("zeroPagesEnabled".equals(field)) {
                    if (!saved.isJsonPrimitive() || !saved.getAsJsonPrimitive().isBoolean()
                            || MemoryPolicyAdmissionRules.capabilityReason(state, "ksmZeroPages", now) != null
                            || !actualKsm.has(field) || !actualKsm.get(field).isJsonPrimitive()
                            || !actualKsm.get(field).getAsJsonPrimitive().isBoolean()) { return false; }
                    if (saved.getAsBoolean() != actualKsm.get(field).getAsBoolean()) {
                        anyActualDrift = true;
                    }
                } else if ("enabled".equals(field)) {
                    if (!saved.isJsonPrimitive() || !saved.getAsJsonPrimitive().isBoolean()
                            || !actualKsm.has(field) || !actualKsm.get(field).isJsonPrimitive()
                            || !actualKsm.get(field).getAsJsonPrimitive().isBoolean()) { return false; }
                    if (saved.getAsBoolean() != actualKsm.get(field).getAsBoolean()) {
                        anyActualDrift = true;
                    }
                } else if ("pagesToScan".equals(field) || "sleepMillis".equals(field)) {
                    Long expected = exactJsonLong(saved), actual = exactJsonLong(actualKsm.get(field));
                    if (expected == null || actual == null) { return false; }
                    if (!expected.equals(actual)) { anyActualDrift = true; }
                } else {
                    // Do not turn fields unknown to this native-restore
                    // contract into implicit write authority.
                    return false;
                }
            }
            if (!anyActualDrift) { return false; }
            // The native writer uses defaults for omitted scan values. Restore
            // the complete applied target; the Agent must prove every sent
            // value from the old readback. Do not infer zero-page authority
            // from a merged default absent from the original task.
            for (Map.Entry<String, JsonElement> entry : restoreKsm.entrySet()) {
                changedFields.put("ksm." + entry.getKey(), entry.getValue().toString());
            }
            return true;
        } catch (RuntimeException invalidOrIncompleteEvidence) {
            return false;
        }
    }

    private static Long exactJsonLong(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) { return null; }
        try { return value.getAsBigDecimal().longValueExact(); }
        catch (ArithmeticException | NumberFormatException invalid) { return null; }
    }

    private static void ignoreAbsentDisabledMechanismDefaults(String appliedPolicy, JsonObject target,
            String effectivePolicy, Map<String, String> changedFields) {
        JsonObject applied = new JsonParser().parse(appliedPolicy).getAsJsonObject();
        for (String field : Arrays.asList("zram.enabled", "writeback.enabled")) {
            if (!changedFields.containsKey(field) || jsonPath(target, field) != null
                    || !Boolean.FALSE.equals(MemoryStandardConfigCodec.get(effectivePolicy, field))) { continue; }
            JsonElement previous = jsonPath(applied, field);
            if (previous != null && previous.isJsonPrimitive() &&
                    previous.getAsJsonPrimitive().isBoolean() && !previous.getAsBoolean()) {
                // These optional mechanisms default off. Their false default
                // disappearing from an explicitly managed sparse target is
                // not a capability-dependent enable/disable request. Keep
                // true -> null and all malformed/unknown values in admission.
                changedFields.remove(field);
            }
        }
    }

    private JsonObject explicitlyManagedPolicy(EntityManager em, String hostUuid, String effectivePolicy) {
        Map<String, String> sources = new LinkedHashMap<>();
        for (MemoryPolicyVO source : sourceChain(em, "Host", hostUuid)) {
            MemoryPolicyRules.addFieldSources(compatibilityPolicyForSource(em, source), source.getUuid(), sources);
            applyStandardFieldSources(em, source, sources);
        }
        JsonObject effective = new JsonParser().parse(effectivePolicy).getAsJsonObject();
        JsonObject target = new JsonObject();
        target.addProperty("schemaVersion", 1);
        for (String path : new TreeSet<>(sources.keySet())) {
            JsonElement value = jsonPath(effective, path);
            if (value != null) { setJsonPath(target, path, value.deepCopy()); }
        }
        return target;
    }

    private static JsonElement jsonPath(JsonObject root, String path) {
        JsonObject current = root;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) { return null; }
            current = current.getAsJsonObject(parts[i]);
        }
        return current.get(parts[parts.length - 1]);
    }

    private static void setJsonPath(JsonObject root, String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonObject current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                current.add(parts[i], new JsonObject());
            }
            current = current.getAsJsonObject(parts[i]);
        }
        current.add(parts[parts.length - 1], value);
    }

    private void recordPolicyPlan(MemoryStateVO state, HostPolicyPlan plan, String status) {
        state.setPolicyTargetHash(plan.targetHash);
        state.setPolicyPlanHash(plan.planHash);
        state.setPolicyPlanStatus(status);
        state.setPolicyBlockedFields(plan.blockedFields);
    }

    private HostPolicyPlan deferredPlan(HostPolicyPlan source, String reason) {
        HostPolicyPlan plan = new HostPolicyPlan();
        plan.targetHash = source.targetHash;
        plan.payload = "{}";
        plan.payloadHash = null;
        plan.hasPayload = false;
        JsonObject blockers = new JsonObject();
        for (String field : new TreeSet<>(source.changedFields.keySet())) {
            JsonObject evidence = new JsonObject();
            evidence.addProperty("desired", source.changedFields.get(field));
            evidence.addProperty("reason", reason);
            blockers.add(field, evidence);
        }
        if (blockers.size() == 0) { blockers.addProperty("policy", reason); }
        plan.blockedFields = blockers.toString();
        plan.hasBlockers = true;
        plan.changedFields = source.changedFields;
        plan.replacesAppliedPolicy = source.replacesAppliedPolicy;
        plan.confirmedKsmReconnect = source.confirmedKsmReconnect;
        plan.expectedControlOperationUuid = source.expectedControlOperationUuid;
        plan.planHash = digest(plan.targetHash + "|deferred|" + plan.blockedFields);
        return plan;
    }

    /** Apply the current inherited effective policy to a connected Host without creating a Host override. */
    public boolean applyEffectivePolicyAfterConnect(String hostUuid) {
        return transaction(em -> {
            lock(em);
            validateResource(em, "Host", hostUuid);
            List<HostStatus> hostStatuses = em.createQuery("select h.status from HostVO h where h.uuid = :uuid", HostStatus.class)
                    .setParameter("uuid", hostUuid).getResultList();
            if (hostStatuses.isEmpty() || hostStatuses.get(0) != HostStatus.Connected) { return false; }
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
            if (state == null) {
                state = new MemoryStateVO(); state.setHostUuid(hostUuid); state.setStatus("Unknown"); em.persist(state);
                bumpQueryVersion(em, "state");
            }
            if (state.getActiveTaskUuid() != null) {
                MemoryTaskVO active = em.find(MemoryTaskVO.class, state.getActiveTaskUuid());
                // A new MN may save intent while an older Agent cannot safely
                // perform the read-only completion query. Once observation
                // proves support, reuse that saved request for one upgrade
                // recheck. Other Unknown outcomes are not periodic retries.
                if (isConfigurationReconcile(active) && "Unknown".equals(active.getStatus())
                        && "READ_ONLY_COMPLETION_UNSUPPORTED".equals(active.getReason())
                        && active.getParentUuid() != null
                        && hasFreshCapabilityObservation(state, System.currentTimeMillis())) {
                    JsonObject capabilities = new JsonParser().parse(state.getCapabilities()).getAsJsonObject();
                    JsonElement supported = capabilities.get("readOnlyCompletedReconcile");
                    JsonObject observed = new JsonParser().parse(state.getState()).getAsJsonObject();
                    if (supported != null && supported.isJsonPrimitive()
                            && supported.getAsJsonPrimitive().isBoolean() && supported.getAsBoolean()
                            && hostUuid.equals(getString(observed, "hostUuid")) && getString(observed, "bootId") != null
                            && em.createQuery("select t.uuid from MemoryTaskVO t where t.parentUuid = :parent "
                                    + "and t.hostUuid = :host and t.actorUuid = :actor and t.action = 'reconcile' "
                                    + "and t.reconcileOperationUuid = :original", String.class)
                                .setParameter("parent", active.getParentUuid()).setParameter("host", hostUuid)
                                .setParameter("actor", active.getActorUuid())
                                .setParameter("original", active.getReconcileOperationUuid())
                                .setMaxResults(2).getResultList().size() == 1) {
                        MemoryTaskVO parent = em.find(MemoryTaskVO.class, active.getParentUuid());
                        if (parent != null && queueConfigurationReconcile(em, parent, state)) {
                            bumpQueryVersion(em, "task");
                            return true;
                        }
                    }
                }
                return false;
            }
            if (!hasFreshCapabilityObservation(state, System.currentTimeMillis())) { return false; }
            if ("Unknown".equals(state.getStatus()) && hasUnresolvedControl(em, state)) { return false; }
            String effectivePolicy = effective(em, "Host", hostUuid);
            // An unmanaged value has no authority over native state. Managed defaults
            // and explicit false are intent too; a previously applied mechanism
            // must complete the existing clear path before releasing ownership.
            if (!hasExplicitMechanismPolicy(em, hostUuid) && !hasAppliedMechanismPolicy(state)) { return false; }
            if (state.getAppliedPolicyHash() == null && hasHistoricalControl(em, state)) {
                recordLegacyPolicyNeedsReview(state, effectivePolicy);
                return false;
            }
            // Observation success is not proof that an older controller has
            // completed. Keep the legacy NeedsReview diagnosis above, but
            // never issue a new policy across a durable control fence, even
            // when a newer observation says Succeeded.
            if (hasUnresolvedControl(em, state) || configurationControlDeferReason(em, state) != null) {
                return false;
            }
            HostPolicyPlan plan = planForHost(em, hostUuid, effectivePolicy, state);
            // Automatic reconciliation may cross a paused lifecycle only for
            // the exact maintenance-initialization proof checked by the plan.
            // Ordinary pause and sparse/old drain observations keep their fence.
            if (isConfirmedPausedState(state.getState()) && !plan.maintenanceActivation) {
                if (plan.hasBlockers) { recordPolicyPlan(state, plan, "Blocked"); }
                return false;
            }
            // A rejected maintenance activation re-issues the same plan once its proof is ready again.
            if (plan.planHash.equals(state.getPolicyPlanHash())
                    && !(plan.maintenanceActivation && "Failed".equals(state.getPolicyPlanStatus()))) { return false; }
            if (plan.replacesAppliedPolicy && plan.hasBlockers && !plan.maintenanceActivation) {
                recordPolicyPlan(state, plan, "Blocked");
                return false;
            }
            if (!plan.hasPayload || (!plan.rebootReconfirmation
                    && Objects.equals(plan.payloadHash, state.getAppliedPolicyHash()))) {
                recordPolicyPlan(state, plan, plan.hasBlockers ? "Blocked" : "Applied");
                return false;
            }

            String actor = "system:memory-host-connect-policy";
            long revision = state.getDesiredRevision() + 1;
            APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
            msg.setScope("Host"); msg.setResourceUuid(hostUuid);
            msg.setAction(plan.replacesAppliedPolicy ? "clearOverride" : "apply");
            MemoryTaskVO parent = task(msg, null, null, revision, "{}", actor);
            parent.setStatus("Queued"); em.persist(parent);
            bumpQueryVersion(em, "task");
            MemoryTaskVO child = task(msg, parent.getUuid(), hostUuid, revision, plan.payload, actor);
            if (plan.capacityMaintenance != null && !plan.hasBlockers) {
                child.setAction("drain"); child.setPolicy(plan.capacityMaintenance);
                child.setExpectedControlOperationUuid(state.getControlOperationUuid());
            }
            child.setPolicyPlanHash(plan.planHash);
            if (plan.confirmedKsmReconnect) {
                child.setExpectedControlOperationUuid(plan.expectedControlOperationUuid);
            }
            em.persist(child);
            state.setDesiredRevision(revision); state.setActiveTaskUuid(child.getUuid());
            state.setControlOperationUuid(child.getUuid()); state.setPermitAuthorized(false);
            state.setStatus("Queued"); state.setReason(null);
            recordPolicyPlan(state, plan, "Queued");
            return true;
        });
    }

    /**
     * Dispatch-time proof for the narrow license-expiry exception. The
     * Agent repeats the journal/boot checks under its Host writer lock; this
     * MN check prevents ordinary or stale tasks from borrowing that path.
     */
    boolean isConfirmedKsmReconnectTask(String taskUuid) {
        if (taskUuid == null) { return false; }
        return transaction(em -> {
            MemoryTaskVO task = em.find(MemoryTaskVO.class, taskUuid);
            if (task == null || !"Host".equals(task.getScope()) || task.getHostUuid() == null
                    || !"apply".equals(task.getAction())
                    || !"system:memory-host-connect-policy".equals(task.getActorUuid())
                    || !"Applying".equals(task.getStatus())
                    || task.getExpectedControlOperationUuid() == null
                    || task.getPolicyPlanHash() == null || task.getPolicy() == null) { return false; }
            MemoryTaskVO original = em.find(MemoryTaskVO.class, task.getExpectedControlOperationUuid());
            MemoryStateVO state = em.find(MemoryStateVO.class, task.getHostUuid());
            if (state == null || !("Queued".equals(state.getStatus()) || "Applying".equals(state.getStatus()))
                    || !Objects.equals(taskUuid, state.getActiveTaskUuid())
                    || !Objects.equals(taskUuid, state.getControlOperationUuid())
                    || task.getDesiredRevision() != state.getDesiredRevision()
                    || state.getAppliedRevision() == null || task.getDesiredRevision() != state.getAppliedRevision() + 1
                    || original == null || !state.getAppliedRevision().equals(original.getDesiredRevision())
                    || state.getAppliedPolicyHash() == null || state.getAppliedPolicy() == null
                    || !hasFreshCapabilityObservation(state, System.currentTimeMillis())
                    || MemoryCapabilityRules.hasPartialKsmMarker(state)
                    || MemoryPolicyAdmissionRules.capabilityReason(state, "ksm", System.currentTimeMillis()) != null) {
                return false;
            }
            if (original == null || !"Host".equals(original.getScope())
                    || !task.getHostUuid().equals(original.getHostUuid())
                    || !"Succeeded".equals(original.getStatus())
                    || !Arrays.asList("apply", "clearOverride").contains(original.getAction())
                    || !Objects.equals(state.getControlOperationUuid(), task.getUuid())
                    || !Objects.equals(state.getAppliedPolicyHash(), digest(original.getPolicy()))) { return false; }

            try {
                JsonObject observation = new JsonParser().parse(state.getState()).getAsJsonObject();
                JsonElement boot = observation.get("bootId");
                if (boot == null || !boot.isJsonPrimitive() || !boot.getAsJsonPrimitive().isString()
                        || boot.getAsString().trim().isEmpty() || observation.has("lastConfirmedOperationUuid")
                        || observation.has("lastConfirmedAppliedRevision") || isConfirmedPausedState(state.getState())) {
                    return false;
                }
                JsonObject actualRoot = observation.getAsJsonObject("actual");
                JsonObject actualKsm = actualRoot == null ? null : actualRoot.getAsJsonObject("ksm");
                JsonObject payload = new JsonParser().parse(task.getPolicy()).getAsJsonObject();
                JsonObject payloadKsm = payload.getAsJsonObject("ksm");
                JsonObject originalPolicy = new JsonParser().parse(original.getPolicy()).getAsJsonObject();
                JsonObject originalKsm = originalPolicy.getAsJsonObject("ksm");
                JsonObject applied = new JsonParser().parse(state.getAppliedPolicy()).getAsJsonObject();
                JsonObject appliedKsm = applied.getAsJsonObject("ksm");
                JsonObject effectiveKsm = new JsonParser().parse(effective(em, "Host", task.getHostUuid()))
                        .getAsJsonObject().getAsJsonObject("ksm");
                if (payloadKsm == null || payloadKsm.entrySet().isEmpty() || originalKsm == null
                        || originalKsm.entrySet().isEmpty()
                        || appliedKsm == null || effectiveKsm == null || actualKsm == null
                        || payload.has("zram") || payload.has("writeback") || payload.entrySet().size() != 2
                        || !payloadKsm.has("enabled") || !payloadKsm.get("enabled").isJsonPrimitive()
                        || !payloadKsm.get("enabled").getAsJsonPrimitive().isBoolean()
                        || !payloadKsm.has("pagesToScan") || exactJsonLong(payloadKsm.get("pagesToScan")) == null
                        || !payloadKsm.has("sleepMillis") || exactJsonLong(payloadKsm.get("sleepMillis")) == null
                        || payloadKsm.has("zeroPagesEnabled") != originalKsm.has("zeroPagesEnabled")
                        || !appliedKsm.equals(effectiveKsm)) { return false; }
                for (Map.Entry<String, JsonElement> entry : originalKsm.entrySet()) {
                    if (!entry.getValue().equals(payloadKsm.get(entry.getKey()))) { return false; }
                }
                boolean actualMismatch = false;
                for (Map.Entry<String, JsonElement> entry : payloadKsm.entrySet()) {
                    String field = entry.getKey(); JsonElement value = entry.getValue();
                    if (!appliedKsm.has(field) || !value.equals(appliedKsm.get(field))
                            || !actualKsm.has(field)) { return false; }
                    if ("enabled".equals(field) || "zeroPagesEnabled".equals(field)) {
                        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()
                                || !actualKsm.get(field).isJsonPrimitive()
                                || !actualKsm.get(field).getAsJsonPrimitive().isBoolean()) { return false; }
                        if (value.getAsBoolean() != actualKsm.get(field).getAsBoolean()) { actualMismatch = true; }
                        if ("zeroPagesEnabled".equals(field)
                                && MemoryPolicyAdmissionRules.capabilityReason(state, "ksmZeroPages", System.currentTimeMillis()) != null) {
                            return false;
                        }
                    } else if ("pagesToScan".equals(field) || "sleepMillis".equals(field)) {
                        Long expected = exactJsonLong(value), observed = exactJsonLong(actualKsm.get(field));
                        if (expected == null || observed == null) { return false; }
                        if (!expected.equals(observed)) { actualMismatch = true; }
                    } else { return false; }
                }
                return actualMismatch;
            } catch (RuntimeException invalidOrIncompleteEvidence) { return false; }
        });
    }

    private static boolean hasFreshCapabilityObservation(MemoryStateVO state, long now) {
        if (state == null || state.getLastSampleTime() == null || state.getCapabilities() == null
                || state.getState() == null) { return false; }
        long age = now - state.getLastSampleTime();
        if (state.getLastSampleTime() > now + 5000 || age < 0
                || age >= MemoryOptimizationGlobalConfig.displayTtlMillis()) { return false; }
        try {
            JsonElement parsed = new JsonParser().parse(state.getCapabilities());
            if (!parsed.isJsonObject()) { return false; }
            JsonElement supported = parsed.getAsJsonObject().get("supported");
            return supported != null && supported.isJsonPrimitive() && supported.getAsJsonPrimitive().isBoolean();
        } catch (RuntimeException ignored) { return false; }
    }

    private boolean hasUnresolvedControl(EntityManager em, MemoryStateVO state) {
        String uuid = state.getControlOperationUuid();
        if (uuid == null) { return false; }
        MemoryTaskVO control = em.find(MemoryTaskVO.class, uuid);
        return control == null || MemoryTaskRules.blocksHost(control.getStatus());
    }

    /** Legacy history is a confirmed application whose policy is unknown, not rejected attempts. */
    private static boolean hasHistoricalControl(EntityManager em, MemoryStateVO state) {
        if (state.getAppliedRevision() != null || state.getAppliedPolicy() != null) { return true; }
        if (state.getControlOperationUuid() == null) { return false; }
        MemoryTaskVO control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
        return control == null || !"Failed".equals(control.getStatus());
    }

    private static void recordLegacyPolicyNeedsReview(MemoryStateVO state, String effectivePolicy) {
        JsonObject blocker = new JsonObject();
        blocker.addProperty("reason", "LEGACY_APPLIED_POLICY_UNKNOWN");
        JsonObject fields = new JsonObject(); fields.add("policy", blocker);
        String targetHash = digest(effectivePolicy);
        state.setPolicyTargetHash(targetHash);
        state.setPolicyPlanHash(digest(targetHash + "|legacy-applied-policy-unknown"));
        state.setPolicyPlanStatus("NeedsReview");
        state.setPolicyBlockedFields(fields.toString());
        state.setReason("Legacy Host policy application cannot be reconstructed safely; explicit review is required");
    }

    private static boolean hasAppliedMechanismPolicy(MemoryStateVO state) {
        if (state.getAppliedPolicyHash() == null || state.getAppliedPolicy() == null) { return false; }
        try {
            JsonObject applied = new JsonParser().parse(state.getAppliedPolicy()).getAsJsonObject();
            for (String mechanism : Arrays.asList("ksm", "zram", "writeback")) {
                if (applied.has(mechanism) && applied.get(mechanism).isJsonObject()
                        && !applied.getAsJsonObject(mechanism).entrySet().isEmpty()) { return true; }
            }
            return false;
        } catch (RuntimeException invalid) { return false; }
    }

    private boolean hasExplicitMechanismPolicy(EntityManager em, String hostUuid) {
        for (MemoryPolicyVO source : sourceChain(em, "Host", hostUuid)) {
            try {
                JsonObject policy = new JsonParser().parse(source.getPolicy()).getAsJsonObject();
                for (String mechanism : Arrays.asList("ksm", "zram", "writeback")) {
                    JsonElement value = policy.get(mechanism);
                    if (value != null && value.isJsonObject() && !value.getAsJsonObject().entrySet().isEmpty()) {
                        return true;
                    }
                }
            } catch (RuntimeException ignored) {
                // Invalid or unknown policy bytes are not permission to take
                // control of a legacy Host on reconnect.
                return false;
            }
        }
        for (MemoryPolicyVO source : sourceChain(em, "Host", hostUuid)) {
            MemoryStandardField.Scope level = scopeOf(source.getScope());
            if (level == null) { continue; }
            for (MemoryStandardField field : MemoryStandardField.values()) {
                if (!field.allows(level)) { continue; }
                Object value = "Global".equals(source.getScope()) ? standardConfigs.readGlobalOwn(em, field.path())
                        : standardConfigs.readOverrideOwn(em, field.path(), source.getResourceUuid());
                if (value == null || MemoryStandardConfigAdapter.isUnmanaged(value)) { continue; }
                // A default-off feature switch is not intent to take over a Host;
                // a default-on switch (new cloud) is.
                if ("Global".equals(source.getScope()) && standardConfigs.globalIsSchemaDefault(em, field.path())
                        && (!field.isFeatureSwitch() || Boolean.FALSE.equals(value))) { continue; }
                return true;
            }
        }
        return false;
    }

    private static boolean isMaintenancePreparationAction(String action) {
        return "prepareWritebackBackend".equals(action) || "prepareZramPool".equals(action);
    }

    /** Accept both the immediate Agent acknowledgement and the periodic
     * RuntimeSnapshot shape; never infer paused from a missing/unknown state. */
    static boolean isConfirmedPausedState(String raw) {
        try {
            JsonObject observed = new JsonParser().parse(raw).getAsJsonObject();
            if (observed.has("phase") && "PAUSED".equalsIgnoreCase(observed.get("phase").getAsString())) {
                return true;
            }
            if (!observed.has("lifecycle") || !observed.get("lifecycle").isJsonObject()) { return false; }
            JsonObject lifecycle = observed.getAsJsonObject("lifecycle");
            return lifecycle.has("paused") && lifecycle.get("paused").getAsBoolean()
                    && lifecycle.has("activeState") && "paused".equalsIgnoreCase(lifecycle.get("activeState").getAsString());
        } catch (RuntimeException ignored) { return false; }
    }

    /**
     * A completed offline drain is not itself permission to recreate a pool.
     * The Agent must publish a fresh, durable maintenance proof for the same
     * drain fence.  There are two deliberately explicit recovery outcomes:
     * an already-created service-owned pool with a new generation, or a clean
     * post-release device which is ready for the next normal enable operation.
     * The latter must not be mistaken for a missing/unknown snapshot.
     */
    static boolean isMaintenanceRecoveryState(String raw, String drainOperationUuid) {
        try {
            JsonObject root = new JsonParser().parse(raw).getAsJsonObject();
            JsonObject proof = root.has("maintenanceProof") && root.get("maintenanceProof").isJsonObject()
                    ? root.getAsJsonObject("maintenanceProof") : root;
            if (!getBoolean(proof, "maintenanceReady")
                    || !optionalBoolean(proof, "maintenanceArchived", "maintenance_archived")
                    || !getBoolean(proof, "executorExited", "executor_exited", "noInflightOperations", "no_inflight_operations")) {
                return false;
            }
            String operation = getString(proof, "drainOperationUuid", "drain_operation_uuid");
            String oldGeneration = getString(proof, "oldPoolGeneration", "old_pool_generation");
            String newGeneration = getString(proof, "newPoolGeneration", "new_pool_generation");
            Long active = getLong(proof, "activeOperations", "active_operations");
            if (drainOperationUuid == null || !drainOperationUuid.equals(operation)) { return false; }
            if (active != null && active != 0L) { return false; }

            // Normal apply after maintenance may observe a newly-created pool.
            if (oldGeneration != null && newGeneration != null
                    && !oldGeneration.equals(newGeneration)
                    && optionalBoolean(proof, "poolOwnedByService", "pool_owned_by_service")) {
                String current = getString(root, "poolGeneration", "pool_generation",
                        "newPoolGeneration", "new_pool_generation");
                return current == null || newGeneration.equals(current);
            }

            // release -> reset leaves the device intentionally uninitialized;
            // the next ordinary enable creates the pool.  Accept only a
            // durable, explicit proof of that state, never an absent field.
            return getBoolean(proof, "readyForInitialization", "ready_for_initialization")
                    && getBoolean(proof, "originalDeviceInactive", "original_device_inactive")
                    && getBoolean(proof, "oldPoolOwnershipAbsent", "old_pool_ownership_absent")
                    && newGeneration == null;
        } catch (RuntimeException ignored) { return false; }
    }

    private static String getString(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name) && object.get(name).isJsonPrimitive()) {
                String value = object.get(name).getAsString(); if (!value.trim().isEmpty()) { return value; }
            }
        }
        return null;
    }

    private static boolean getBoolean(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name) && object.get(name).isJsonPrimitive() && object.get(name).getAsBoolean()) {
                return true;
            }
        }
        return false;
    }

    private static boolean optionalBoolean(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name)) {
                if (!object.get(name).isJsonPrimitive() || !object.get(name).getAsBoolean()) { return false; }
            }
        }
        return true;
    }

    private static Long getLong(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name) && object.get(name).isJsonPrimitive()) {
                try { return object.get(name).getAsLong(); } catch (RuntimeException ignored) { return null; }
            }
        }
        return null;
    }

    private MemoryTaskVO task(APIUpdateMemoryPolicyMsg msg, String parent, String host, long revision,
                              String policy, String actor) {
        MemoryTaskVO result = new MemoryTaskVO();
        result.setUuid(UUID.randomUUID().toString().replace("-", "")); result.setParentUuid(parent); result.setHostUuid(host);
        result.setScope(msg.getScope()); result.setResourceUuid(msg.getResourceUuid());
        result.setAction(msg.getAction()); result.setDesiredRevision(revision); result.setPolicy(policy);
        result.setExpectedInstanceGeneration(msg.getExpectedInstanceGeneration());
        result.setExpectedControlOperationUuid(msg.getExpectedControlOperationUuid());
        result.setActorUuid(actor); result.setStatus("Queued");
        return result;
    }

    public List<MemoryTaskVO> claim(String owner) {
        return transaction(em -> {
            lock(em);
            long active = em.createQuery("select count(t) from MemoryTaskVO t where t.hostUuid is not null " +
                    "and t.status in ('Applying', 'Draining')", Long.class).getSingleResult();
            int concurrency = MemoryOptimizationGlobalConfig.positive(MemoryOptimizationGlobalConfig.DISPATCH_CONCURRENCY, 10);
            if (active >= concurrency) { return Collections.emptyList(); }
            List<MemoryTaskVO> list = em.createQuery("from MemoryTaskVO t where t.hostUuid is not null " +
                    "and t.status = 'Queued' and exists (select h.uuid from HostVO h where h.uuid = t.hostUuid) order by t.createDate", MemoryTaskVO.class)
                    .setMaxResults((int) (concurrency - active)).getResultList();
            List<MemoryTaskVO> stale = new ArrayList<>();
            boolean stateChanged = false;
            if (!list.isEmpty()) { bumpQueryVersion(em, "task"); }
            for (MemoryTaskVO task : list) {
                if (!"VM".equals(task.getScope())
                        && (Arrays.asList("apply", "clearOverride").contains(task.getAction())
                        || task.getPolicyPlanHash() != null && Arrays.asList("drain",
                                MemoryZramPoolPreparationRules.ACTION,
                                MemoryBackendPreparationRules.ACTION).contains(task.getAction()))) {
                    String currentPolicy = effective(em, "Host", task.getHostUuid());
                    MemoryStateVO state = em.find(MemoryStateVO.class, task.getHostUuid());
                    if (task.getPolicyPlanHash() != null && state != null && state.getPolicyTargetHash() != null) {
                        // Compare the admitted desired target, not the delta payload:
                        // maintenance initialization may legitimately send writeback=false.
                        if (!Objects.equals(task.getPolicyPlanHash(), state.getPolicyPlanHash())
                                || !Objects.equals(state.getPolicyTargetHash(), digest(
                                        explicitlyManagedPolicy(em, task.getHostUuid(), currentPolicy).toString()))) {
                            stale.add(task);
                        }
                    } else {
                        // Legacy full-policy tasks may predate target hashes. Never
                        // dispatch a boolean that no longer matches the resolved target.
                        for (MemoryStandardField field : MemoryStandardField.values()) {
                            if (field.isFeatureSwitch()
                                    && MemoryStandardConfigCodec.get(task.getPolicy(), field.path()) instanceof Boolean
                                    && !Objects.equals(MemoryStandardConfigCodec.get(task.getPolicy(), field.path()),
                                            MemoryStandardConfigCodec.get(currentPolicy, field.path()))) {
                                stale.add(task);
                                break;
                            }
                        }
                    }
                }
                if (stale.contains(task)) {
                    // No Agent request has been sent. result() joins this transaction,
                    // so failure, parent aggregation and outbox commit with the claim.
                    result(task.getUuid(), "Failed", "MEMORY_REVISION_CONFLICT: effective policy changed before dispatch; request was not sent", null);
                    continue;
                }
                task.setStatus("Applying"); task.setOwnerManagementNodeUuid(owner);
                MemoryStateVO state = "VM".equals(task.getScope()) ? null
                        : em.find(MemoryStateVO.class, task.getHostUuid());
                if (state != null && task.getUuid().equals(state.getActiveTaskUuid())
                        && task.getDesiredRevision() == state.getDesiredRevision()) {
                    // Claiming an old task must not replace a newer target or
                    // lifecycle fence. Project only the admitted queued work;
                    // actual drain/completion stages still come from its result.
                    if ("Queued".equals(state.getStatus())) {
                        state.setStatus("Applying"); stateChanged = true;
                    }
                    if (task.getPolicyPlanHash() != null
                            && task.getPolicyPlanHash().equals(state.getPolicyPlanHash())
                            && "Queued".equals(state.getPolicyPlanStatus())) {
                        state.setPolicyPlanStatus("Applying"); stateChanged = true;
                    }
                }
                MemoryTaskVO parent = em.find(MemoryTaskVO.class, task.getParentUuid());
                if (parent != null) { parent.setStatus("Applying"); }
            }
            if (stateChanged) { bumpQueryVersion(em, "state"); }
            list.removeAll(stale);
            em.flush();
            return list;
        });
    }

    /** Claim committed failure intents for at-least-once CloudBus delivery. */
    public List<MemoryTaskFailureOutboxVO> claimFailureNotifications(String owner) {
        long now = System.currentTimeMillis();
        return transaction(em -> {
            lock(em);
            List<MemoryTaskFailureOutboxVO> rows = em.createQuery(
                            "from MemoryTaskFailureOutboxVO o where o.delivered = false "
                                    + "and (o.leaseUntil is null or o.leaseUntil < :now) order by o.createDate",
                            MemoryTaskFailureOutboxVO.class)
                    .setParameter("now", now).setMaxResults(32).getResultList();
            for (MemoryTaskFailureOutboxVO row : rows) {
                row.setLeaseOwner(owner);
                row.setLeaseUntil(now + FAILURE_NOTIFICATION_LEASE_MS);
                row.setAttempts(row.getAttempts() + 1);
            }
            em.flush();
            return rows;
        });
    }

    /** Mark delivered only after the ZWatch consumer confirms its receipt/event transaction. */
    public void completeFailureNotification(String taskUuid, String owner, boolean delivered, String error) {
        transaction(em -> {
            MemoryTaskFailureOutboxVO row = em.find(MemoryTaskFailureOutboxVO.class, taskUuid, LockModeType.PESSIMISTIC_WRITE);
            if (row == null || row.isDelivered() || !Objects.equals(owner, row.getLeaseOwner())) { return null; }
            row.setLastError(delivered || error == null ? null
                    : error.substring(0, Math.min(512, error.length())));
            if (delivered) {
                row.setDelivered(true);
                row.setLeaseOwner(null);
                row.setLeaseUntil(null);
            } else {
                long backoff = Math.min(300_000L, 5_000L * (1L << Math.min(6, Math.max(0, row.getAttempts() - 1))));
                row.setLeaseOwner(null);
                row.setLeaseUntil(System.currentTimeMillis() + backoff);
            }
            return null;
        });
    }

    public void releaseFailureNotifications(String owner) {
        transaction(em -> {
            lock(em);
            List<MemoryTaskFailureOutboxVO> rows = em.createQuery(
                            "from MemoryTaskFailureOutboxVO o where o.delivered = false and o.leaseOwner = :owner",
                            MemoryTaskFailureOutboxVO.class)
                    .setParameter("owner", owner).getResultList();
            for (MemoryTaskFailureOutboxVO row : rows) {
                row.setLeaseOwner(null);
                row.setLeaseUntil(null);
            }
            return null;
        });
    }

    public void result(String uuid, String status, String reason, MemoryAgentResponse response) {
        result(uuid, status, reason, response, null);
    }

    public void result(String uuid, String status, String reason, MemoryAgentResponse response,
                       MemoryAgentResponse observation) {
        transaction(em -> {
            lock(em);
            String resultStatus = status;
            String resultReason = reason;
            MemoryTaskVO task = em.find(MemoryTaskVO.class, uuid);
            MemoryStateVO host = task == null ? null : em.find(MemoryStateVO.class, task.getHostUuid());
            if (isConfigurationReconcile(task)
                    && (host == null || !Objects.equals(host.getActiveTaskUuid(), task.getUuid())
                        || !Objects.equals(host.getControlOperationUuid(), task.getExpectedControlOperationUuid())
                        || host.getDesiredRevision() != task.getDesiredRevision()
                        || !Objects.equals(task.getHostUuid(), task.getResourceUuid()))) {
                // A delayed read-only configuration query no longer owns this
                // Host fence. Its receipt must not settle the original task or
                // overwrite the result of the newer operation that replaced it.
                return null;
            }
            MemoryTaskVO reconciledControl = task != null && "reconcile".equals(task.getAction())
                    && task.getReconcileOperationUuid() != null
                    ? em.find(MemoryTaskVO.class, task.getReconcileOperationUuid()) : null;
            MemoryTaskVO maintenanceTask = reconciledControl == null ? task : reconciledControl;
            MemoryTaskVO maintenanceRoot = maintenanceTask == null || maintenanceTask.getParentUuid() == null
                    ? null : em.find(MemoryTaskVO.class, maintenanceTask.getParentUuid());
            boolean selectedBackendMaintenance = isSelectedBackendMaintenance(maintenanceTask, maintenanceRoot);
            boolean drainRecovery = isDrainRecoveryMaintenance(maintenanceTask, maintenanceRoot);
            MemoryTaskVO nextMaintenance = null;
            Map<String, Object> preparedBackendConfig = new LinkedHashMap<>();
            MemoryTaskVO preparationDrain = maintenanceTask != null
                    && (isMaintenancePreparationAction(maintenanceTask.getAction())
                        || Arrays.asList("apply", "clearOverride").contains(maintenanceTask.getAction()))
                    && maintenanceTask.getExpectedControlOperationUuid() != null
                    ? em.find(MemoryTaskVO.class, maintenanceTask.getExpectedControlOperationUuid()) : null;
            boolean automaticRestore = preparationDrain != null
                    && isAutomaticCapacityDrain(preparationDrain, preparationDrain.getParentUuid() == null ? null
                            : em.find(MemoryTaskVO.class, preparationDrain.getParentUuid()))
                    && Objects.equals(maintenanceTask.getHostUuid(), preparationDrain.getHostUuid())
                    && Objects.equals(maintenanceTask.getParentUuid(), preparationDrain.getParentUuid());
            boolean recoveredDrain = drainRecovery && preparationDrain != null
                    && "drain".equals(preparationDrain.getAction()) && "Succeeded".equals(preparationDrain.getStatus())
                    && Objects.equals(maintenanceTask.getHostUuid(), preparationDrain.getHostUuid());
            boolean automaticPreparation = selectedBackendMaintenance
                    && MemoryBackendPreparationRules.ACTION.equals(maintenanceTask.getAction())
                    || automaticRestore && isMaintenancePreparationAction(maintenanceTask.getAction())
                        && Objects.equals(maintenanceTask.getPolicyPlanHash(), preparationDrain.getPolicyPlanHash())
                    || recoveredDrain && MemoryZramPoolPreparationRules.ACTION.equals(maintenanceTask.getAction());
            automaticRestore = automaticRestore || recoveredDrain;
            boolean selectedRestoration = selectedBackendMaintenance && preparationDrain != null
                    && "drain".equals(preparationDrain.getAction()) && "Succeeded".equals(preparationDrain.getStatus())
                    && Objects.equals(maintenanceTask.getHostUuid(), preparationDrain.getHostUuid());
            boolean automaticActivation = (automaticRestore || selectedRestoration)
                    && Arrays.asList("apply", "clearOverride").contains(maintenanceTask.getAction());
            boolean automaticDrain = isAutomaticCapacityDrain(maintenanceTask,
                    maintenanceTask == null || maintenanceTask.getParentUuid() == null ? null
                            : em.find(MemoryTaskVO.class, maintenanceTask.getParentUuid()));
            boolean automaticResult = automaticDrain || automaticPreparation || automaticActivation;
            if (automaticDrain && "Succeeded".equals(resultStatus)
                    && MemoryTaskRules.canTransition(task.getStatus(), resultStatus)) {
                try {
                    nextMaintenance = nextCapacityPreparation(em, maintenanceTask, task, host, response, observation);
                } catch (IllegalArgumentException unconfirmed) {
                    resultStatus = "Unknown";
                    resultReason = "Drain receipt requires current maintenance proof: " + unconfirmed.getMessage();
                }
            }
            if (automaticPreparation && "Succeeded".equals(resultStatus)
                    && MemoryTaskRules.canTransition(task.getStatus(), resultStatus)) {
                try {
                    nextMaintenance = nextCapacityActivation(em, maintenanceTask, task, preparationDrain, host,
                            response, observation, preparedBackendConfig);
                } catch (IllegalArgumentException unconfirmed) {
                    resultStatus = "Unknown";
                    resultReason = "Pool preparation requires current activation proof: " + unconfirmed.getMessage();
                }
            }
            if (automaticActivation && "Succeeded".equals(resultStatus)
                    && MemoryTaskRules.canTransition(task.getStatus(), resultStatus)) {
                try {
                    nextMaintenance = nextCapacityWriteback(em, maintenanceTask, task, preparationDrain, host, response, observation);
                } catch (IllegalArgumentException unconfirmed) {
                    resultStatus = "Unknown";
                    resultReason = "Capacity restoration requires current activation proof: " + unconfirmed.getMessage();
                }
            }
            // Retiring the right to execute does not retrospectively decide an
            // old missing-receipt outcome. Even a late callback cannot rewrite it.
            if (task != null && Arrays.asList("apply", "clearOverride").contains(task.getAction())
                    && !em.createQuery("select t.uuid from MemoryTaskVO t where t.action = :action "
                            + "and t.expectedControlOperationUuid = :original and t.status not in ('Failed', 'Cancelled')", String.class)
                        .setParameter("action", MemoryUncertainRecoveryRules.ACTION)
                        .setParameter("original", uuid).setMaxResults(1).getResultList().isEmpty()) { return null; }
            MemoryTaskVO rejectedResume = rejectedResumeForReconcile(em, task);
            String restoredControlFence = null;
            MemoryTaskVO restoredUncertainOwner = null;
            MemoryTaskVO appliedTask = task;
            MemoryAgentResponse stateResponse = automaticActivation || automaticPreparation
                    ? observation : response;
            if (automaticResult && reconciledControl != null && "Succeeded".equals(resultStatus)) {
                // Query only acknowledges the original executor; continuation
                // stays under its original root and uses its native fence.
                appliedTask = reconciledControl;
                restoredControlFence = automaticPreparation ? maintenanceTask.getExpectedControlOperationUuid()
                        : reconciledControl.getUuid();
            }
            boolean ordinaryReconcile = !automaticResult && reconciledControl != null && !"VM".equals(task.getScope())
                    && !"VM".equals(reconciledControl.getScope())
                    && Arrays.asList("apply", "clearOverride", "pause", "resume", "drain").contains(reconciledControl.getAction())
                    && rejectedResume == null;
            if (ordinaryReconcile && "Succeeded".equals(resultStatus)) {
                // A successful read acknowledges the original executor. It
                // cannot establish a new control identity or borrow the
                // current effective policy as proof of what was applied.
                boolean confirmed = false;
                try {
                    // A journal receipt proves the original outcome. The
                    // separately collected state proves current ownership;
                    // never refresh the receipt's historical sample time.
                    MemoryAgentResponse current = observation == null ? response : observation;
                    Map<?, ?> before = JSONObjectUtil.toObject(host.getState(), Map.class);
                    Object boot = before.get("bootId");
                    MemoryTaskVO currentControl = host.getControlOperationUuid() == null ? null
                            : em.find(MemoryTaskVO.class, host.getControlOperationUuid());
                    boolean completedAlias = currentControl != null && "reconcile".equals(currentControl.getAction())
                            && "Succeeded".equals(currentControl.getStatus())
                            && Objects.equals(task.getHostUuid(), currentControl.getHostUuid())
                            && Objects.equals(reconciledControl.getUuid(), currentControl.getReconcileOperationUuid())
                            && currentControl.getDesiredRevision() == task.getDesiredRevision();
                    confirmed = Objects.equals(uuid, host.getActiveTaskUuid())
                            && (Objects.equals(uuid, host.getControlOperationUuid()) || completedAlias
                                || isConfigurationReconcile(task)
                                    && Objects.equals(task.getExpectedControlOperationUuid(), host.getControlOperationUuid())
                                    && Objects.equals(reconciledControl.getUuid(), host.getControlOperationUuid()))
                            && Objects.equals(task.getHostUuid(), reconciledControl.getHostUuid())
                            && Objects.equals(task.getHostUuid(), current.hostUuid)
                            && (response.hostUuid == null || Objects.equals(task.getHostUuid(), response.hostUuid))
                            && Arrays.asList("Unknown", "Blocked", "Succeeded").contains(reconciledControl.getStatus())
                            && task.getDesiredRevision() == reconciledControl.getDesiredRevision()
                            && task.getDesiredRevision() == host.getDesiredRevision()
                            && (host.getAppliedRevision() == null || host.getAppliedRevision() <= reconciledControl.getDesiredRevision())
                            && "Succeeded".equals(MemoryResultRules.status(reconciledControl.getUuid(),
                                    -1, response))
                            && (response.appliedRevision == null
                                || response.appliedRevision == reconciledControl.getDesiredRevision())
                            && current.isSuccess() && "Succeeded".equals(current.status)
                            && current.appliedRevision != null
                            && current.appliedRevision == reconciledControl.getDesiredRevision()
                            && boot instanceof String && !((String) boot).trim().isEmpty()
                            && (response.bootId == null || Objects.equals(boot, response.bootId))
                            && (response.state == null || !response.state.containsKey("bootId")
                                || Objects.equals(boot, response.state.get("bootId")))
                            && Objects.equals(boot, current.bootId) && Objects.equals(boot, current.state.get("bootId"))
                            && Objects.equals(reconciledControl.getUuid(), current.state.get("lastConfirmedOperationUuid"))
                            && numberEquals(current.state.get("lastConfirmedAppliedRevision"), reconciledControl.getDesiredRevision())
                            && current.sampleTime != null && current.sampleTime <= System.currentTimeMillis()
                            && System.currentTimeMillis() - current.sampleTime < MemoryOptimizationGlobalConfig.displayTtlMillis()
                            && (host.getLastSampleTime() == null || current.sampleTime >= host.getLastSampleTime());
                    if (confirmed) { stateResponse = current; }
                } catch (RuntimeException invalid) { confirmed = false; }
                if (confirmed) {
                    restoredControlFence = reconciledControl.getUuid();
                    appliedTask = reconciledControl;
                } else {
                    resultStatus = "Unknown";
                    resultReason = "Original operation control identity or completion proof is not confirmed";
                }
            } else if (ordinaryReconcile && "Succeeded".equals(reconciledControl.getStatus())
                    && "Failed".equals(resultStatus)) {
                resultStatus = "Unknown";
                resultReason = "Query contradicts the confirmed original operation result";
            }
            if (ordinaryReconcile && host != null && "Failed".equals(resultStatus)
                    && Arrays.asList("apply", "clearOverride").contains(reconciledControl.getAction())
                    && Arrays.asList("Unknown", "Blocked").contains(reconciledControl.getStatus())
                    && "Host".equals(reconciledControl.getScope())
                    && Objects.equals(reconciledControl.getHostUuid(), reconciledControl.getResourceUuid())
                    && Objects.equals(task.getHostUuid(), reconciledControl.getHostUuid())
                    && Objects.equals(task.getResourceUuid(), reconciledControl.getHostUuid())
                    && Objects.equals(task.getUuid(), host.getActiveTaskUuid())
                    && (isConfigurationReconcile(task)
                        ? Objects.equals(task.getExpectedControlOperationUuid(), reconciledControl.getUuid())
                            && Objects.equals(reconciledControl.getUuid(), host.getControlOperationUuid())
                        : (task.getExpectedControlOperationUuid() == null
                            || Objects.equals(task.getExpectedControlOperationUuid(), reconciledControl.getUuid()))
                            && Objects.equals(task.getUuid(), host.getControlOperationUuid()))
                    && task.getDesiredRevision() == reconciledControl.getDesiredRevision()
                    && task.getDesiredRevision() == host.getDesiredRevision()
                    && reconciledControl.getPolicyPlanHash() != null
                    && Objects.equals(reconciledControl.getPolicyPlanHash(), host.getPolicyPlanHash())
                    && response != null && !response.isSuccess() && "Failed".equals(response.status)
                    && Objects.equals(reconciledControl.getUuid(), response.operationUuid)
                    && (response.hostUuid == null || Objects.equals(task.getHostUuid(), response.hostUuid))
                    && ("FIRST_ZRAM_ADMISSION_NOT_ISSUED".equals(response.reasonCode)
                        || "KSM_CLEAR_NOT_ISSUED".equals(response.reasonCode))
                    && response.appliedRevision == null && response.state != null
                    && "NOT_ISSUED".equals(response.state.get("phase"))
                    && Boolean.FALSE.equals(response.state.get("policyWritesIssued"))) {
                try {
                    boolean eligiblePolicy = true;
                    if ("KSM_CLEAR_NOT_ISSUED".equals(response.reasonCode)) {
                        Map<?, ?> target = JSONObjectUtil.toObject(reconciledControl.getPolicy(), Map.class);
                        eligiblePolicy = target.size() == 2 && numberEquals(target.get("schemaVersion"), 1)
                                && target.get("ksm") instanceof Map && ((Map<?, ?>) target.get("ksm")).isEmpty();
                    }
                    Map<?, ?> current = JSONObjectUtil.toObject(host.getState(), Map.class);
                    Object boot = current.get("bootId");
                    Object proofBoot = response.state.get("bootId");
                    Object requestHash = response.state.get("requestHash");
                    if (eligiblePolicy && boot instanceof String && !((String) boot).trim().isEmpty()
                            && Objects.equals(boot, proofBoot)
                            && (response.bootId == null || Objects.equals(boot, response.bootId))
                            && requestHash instanceof String
                            && ((String) requestHash).matches("[0-9a-f]{64}")) {
                        // The Agent's immutable NOT_ISSUED receipt settles only
                        // this still-current plan. It does not prove application
                        // or replace the Host's last applied/native observation.
                        appliedTask = reconciledControl;
                    }
                } catch (RuntimeException invalidState) {
                    // Without the current boot binding, keep the plan unresolved.
                }
            }
            if (ordinaryReconcile && "Failed".equals(resultStatus) && response != null
                    && "KSM_CLEAR_NOT_ISSUED".equals(response.reasonCode)
                    && appliedTask != reconciledControl) {
                // An unproven receipt must not release the Host fence or settle
                // the original task and its historical reconcile aliases.
                resultStatus = "Unknown";
                resultReason = "KSM clear not-issued proof is not confirmed";
            }
            MemoryTaskVO recovery = uncertainRecoveryForResult(em, task);
            if (recovery != null) {
                Map<?, ?> request = JSONObjectUtil.toObject(recovery.getPolicy(), Map.class);
                MemoryTaskVO original = em.find(MemoryTaskVO.class, recovery.getExpectedControlOperationUuid());
                MemoryTaskVO drain = request.get("drainControlOperationUuid") == null ? null
                        : em.find(MemoryTaskVO.class, request.get("drainControlOperationUuid"));
                Map<?, ?> state = response == null ? null : response.state;
                Object rawRecoveryProof = state == null ? null : state.get("recoveryProof");
                Map<?, ?> recoveryProof = rawRecoveryProof instanceof Map ? (Map<?, ?>) rawRecoveryProof : null;
                Object anchorUuid = recoveryProof == null ? null : recoveryProof.get("anchorOperationUuid");
                MemoryTaskVO anchor = anchorUuid instanceof String
                        && ((String) anchorUuid).matches("[a-f0-9]{32}")
                        ? em.find(MemoryTaskVO.class, anchorUuid) : null;
                if (host != null && Objects.equals(uuid, host.getActiveTaskUuid())
                        && MemoryUncertainRecoveryRules.confirms(recovery, original, drain, host, response)) {
                    resultStatus = "Succeeded";
                    resultReason = "Uncertain activation retired; original outcome remains Unknown";
                    restoredControlFence = drain.getUuid();
                } else if (host != null && Objects.equals(uuid, host.getActiveTaskUuid())
                        && MemoryUncertainRecoveryRules.confirmsPolicy(recovery, original, anchor, host, response)) {
                    resultStatus = "Succeeded";
                    resultReason = "Original tuning verified and final CPU gate restored; original outcome remains Unknown";
                    restoredControlFence = recovery.getUuid();
                    appliedTask = original;
                } else if (host != null && Objects.equals(uuid, host.getActiveTaskUuid())
                        && MemoryUncertainRecoveryRules.confirmsNotIssued(recovery, original, drain, host, response)) {
                    resultStatus = "Failed";
                    resultReason = "Recovery rejected before dispatch; original outcome remains Unknown: "
                            + (reason == null ? response.reasonCode : reason);
                    restoredUncertainOwner = original;
                } else {
                    resultStatus = "Unknown";
                    resultReason = "Uncertain recovery is not proven: " + (reason == null ? "exact retirement evidence required" : reason);
                    // An unverified or contradictory recovery ACK must not
                    // replace trusted lifecycle facts or advance appliedRevision.
                }
            }
            if (rejectedResume != null) {
                MemoryTaskVO prior = em.find(MemoryTaskVO.class, rejectedResume.getExpectedControlOperationUuid());
                if (isRejectedResumeNotIssuedProof(em, task, rejectedResume, prior, host, response)) {
                    resultStatus = "Succeeded"; // The reconciliation succeeded; the rejected resume remains Failed.
                    resultReason = "Agent confirmed rejected resume was never issued; prior control fence retained";
                    restoredControlFence = prior.getUuid();
                } else {
                    resultStatus = "Unknown";
                    resultReason = "Agent did not provide the exact rejected-resume not-issued control proof";
                }
            }
            if (task != null && host != null && Arrays.asList("pause", "drain").contains(task.getAction())
                    && "Failed".equals(resultStatus) && response != null
                    && "KSM_CONTROL_OPERATION_FENCED".equals(response.reason)
                    && "INVALID_CONTROL_ACTION".equals(response.reasonCode)
                    && task.getExpectedControlOperationUuid() != null
                    && uuid.equals(host.getControlOperationUuid())) {
                // The Agent fenced the request before any effect; its active control is unchanged.
                restoredControlFence = task.getExpectedControlOperationUuid();
            }
            MemoryTaskVO activation = task != null && "reconcile".equals(task.getAction())
                    && task.getReconcileOperationUuid() != null
                    ? em.find(MemoryTaskVO.class, task.getReconcileOperationUuid()) : task;
            if (host != null && activation != null && Arrays.asList("apply", "clearOverride").contains(activation.getAction())
                    && "Failed".equals(resultStatus) && response != null && response.state != null
                    && Boolean.TRUE.equals(response.state.get("activationRejected"))
                    && uuid.equals(host.getControlOperationUuid()) && activation.getExpectedControlOperationUuid() != null
                    && drainControl(em, host, em.find(MemoryTaskVO.class, activation.getExpectedControlOperationUuid()))) {
                // The service rejected the activation before its intent; the Agent stays under the drain.
                restoredControlFence = activation.getExpectedControlOperationUuid();
            }
            if (task == null || !MemoryTaskRules.canTransition(task.getStatus(), resultStatus)) { return null; }
            if (!preparedBackendConfig.isEmpty() && "Succeeded".equals(resultStatus)) {
                // Preparation proves the new owner, not the caller. Persist its
                // standard values with this receipt and the next task. Keep
                // adapter failures outside the proof-to-Unknown catch so the
                // whole transaction rolls back instead of saving a partial bind.
                standardConfigs.writeOverrides(
                        em, host.getHostUuid(), HostVO.class.getSimpleName(), preparedBackendConfig);
                if (Boolean.FALSE.equals(MemoryStandardConfigCodec.get(
                        effective(em, "Host", host.getHostUuid()), "zram.enabled"))) {
                    // The preparation receipt and fresh PREPARED identity
                    // already prove these two values. Registering an offline
                    // backend must not create a redundant Host apply or
                    // replace a retained drain/first-use control fence.
                    String applied = host.getAppliedPolicy() == null ? "{}" : host.getAppliedPolicy();
                    for (Map.Entry<String, Object> field : preparedBackendConfig.entrySet()) {
                        applied = MemoryStandardConfigCodec.put(applied, field.getKey(), field.getValue());
                    }
                    host.setAppliedPolicy(applied);
                }
                bumpScopeRevision(em, "Host", host.getHostUuid());
            }
            bumpQueryVersion(em, "task");
            // A proof is only actionable for the currently admitted transition.
            // Duplicate or late callbacks must not mutate a newer fence.
            if (restoredControlFence != null) { host.setControlOperationUuid(restoredControlFence); }
            task.setStatus(resultStatus); task.setReason(trim(resultReason));
            if (host != null && uuid.equals(host.getActiveTaskUuid())) {
                if (response != null && Arrays.asList("Succeeded", "Blocked").contains(resultStatus)
                        && !"VM".equals(task.getScope())
                        && (recovery == null || restoredControlFence != null)) {
                    if ("Blocked".equals(resultStatus)) {
                        // Keep the verified partial application evidence, not
                        // unconfirmed lifecycle/boot/maintenance assertions.
                        MemoryAgentResponse blocked = new MemoryAgentResponse();
                        blocked.state = new LinkedHashMap<>();
                        if (response.state != null) {
                            for (String key : Arrays.asList("phase", "knownBlocked", "blockers", "sections",
                                    "sectionComplete", "applyEvidence")) {
                                if (response.state.containsKey(key)) { blocked.state.put(key, response.state.get(key)); }
                            }
                        }
                        updateState(host, blocked, false);
                    } else { updateState(host, stateResponse, isMonitoringSample(stateResponse)); }
                }
                if ("Succeeded".equals(resultStatus) && !"VM".equals(task.getScope())
                        && Arrays.asList("apply", "clearOverride").contains(appliedTask.getAction())) {
                    host.setAppliedPolicyHash(digest(appliedTask.getPolicy()));
                    String previousApplied = host.getAppliedPolicy() == null ? "{}" : host.getAppliedPolicy();
                    // A clear carries the complete desired snapshot. Merging
                    // its empty/omitted fields resurrects the removed intent
                    // and causes every later observation to plan another clear.
                    host.setAppliedPolicy("clearOverride".equals(appliedTask.getAction())
                            ? appliedTask.getPolicy() : MemoryPolicyRules.merge(previousApplied, appliedTask.getPolicy(), "Host"));
                    resolveLegacyPolicyReviewAfterApply(em, appliedTask, host);
                }
                if (appliedTask.getPolicyPlanHash() != null
                        && appliedTask.getPolicyPlanHash().equals(host.getPolicyPlanHash())) {
                    String planStatus = "Succeeded".equals(resultStatus)
                            ? (host.getPolicyBlockedFields() != null
                                && !"{}".equals(host.getPolicyBlockedFields()) ? "Blocked" : "Applied")
                            : resultStatus;
                    host.setPolicyPlanStatus(planStatus);
                }
                host.setStatus(resultStatus); host.setReason(trim(resultReason));
                if (!MemoryTaskRules.blocksHost(resultStatus)) { host.setActiveTaskUuid(null); }
                if (ordinaryReconcile && "Succeeded".equals(resultStatus)
                        && Arrays.asList("apply", "clearOverride").contains(appliedTask.getAction())) {
                    // Re-evaluate the retained desired intent against the
                    // confirmed original payload and fresh native facts. Do
                    // not dispatch writes from this result callback.
                    HostPolicyPlan plan = planForHost(em, host.getHostUuid(), effective(em, "Host", host.getHostUuid()), host);
                    if (!plan.hasPayload || plan.hasBlockers) {
                        recordPolicyPlan(host, plan, plan.hasBlockers ? "Blocked" : "Applied");
                    }
                }
                if (restoredUncertainOwner != null) {
                    // A known rejection of the NEW recovery does not release
                    // the OLD unresolved execution or refresh its native facts.
                    host.setActiveTaskUuid(restoredUncertainOwner.getUuid());
                    host.setStatus("Unknown"); host.setReason(restoredUncertainOwner.getReason());
                }
                if (nextMaintenance != null && "Succeeded".equals(resultStatus)) {
                    // Completion and the next sibling are one durable commit.
                    // A crash cannot expose a successful root with lost work.
                    em.persist(nextMaintenance);
                    host.setDesiredRevision(nextMaintenance.getDesiredRevision());
                    host.setActiveTaskUuid(nextMaintenance.getUuid());
                    host.setPermitAuthorized(false); host.setStatus("Queued");
                    if (automaticPreparation || automaticActivation) {
                        host.setControlOperationUuid(nextMaintenance.getUuid());
                        host.setReason(automaticActivation ? "Pool activated; retained writeback restoration is queued"
                                : "Pool preparation completed; retained target restoration is queued");
                    } else { host.setReason("Drain completed; pool preparation is queued"); }
                    host.setPolicyPlanStatus("Queued");
                    bumpQueryVersion(em, "state");
                }
            }
            MemoryTaskVO failedTask = task;
            if ("reconcile".equals(task.getAction()) && !MemoryTaskRules.blocksHost(resultStatus)
                    && task.getReconcileOperationUuid() != null) {
                MemoryTaskVO original = em.find(MemoryTaskVO.class, task.getReconcileOperationUuid());
                if (original != null && Arrays.asList("Unknown", "Blocked").contains(original.getStatus())) {
                    original.setStatus(resultStatus); original.setReason("Resolved by " + uuid);
                    if ("Failed".equals(resultStatus)) { failedTask = original; }
                    em.flush(); aggregateParent(em, original.getParentUuid());
                }
                // A retry may replace an older reconcile child in the Host
                // active slot.  Resolve those historical aliases as well;
                // they all refer to the same original operation and must not
                // leave a stale Unknown parent visible after the root closes.
                List<MemoryTaskVO> aliases = em.createQuery(
                                "from MemoryTaskVO t where t.action = 'reconcile' "
                                        + "and t.reconcileOperationUuid = :operation and t.status = 'Unknown'",
                                MemoryTaskVO.class)
                        .setParameter("operation", task.getReconcileOperationUuid()).getResultList();
                for (MemoryTaskVO alias : aliases) {
                    alias.setStatus(resultStatus); alias.setReason("Resolved by " + uuid);
                    aggregateParent(em, alias.getParentUuid());
                }
            }
            em.flush();
            aggregateParent(em, task.getParentUuid());
            // Reconcile closes the original unknown operation. Keep its identity/action
            // for correlation but report the concrete reconcile failure reason.
            MemoryTaskFailureEvent failureEvent = MemoryTaskFailureEvent.fromTask(
                    failedTask, failedTask == task ? null : task.getReason());
            queueFailure(em, failureEvent);
            return null;
        });
    }

    static boolean isSelectedBackendMaintenance(MemoryTaskVO task, MemoryTaskVO parent) {
        if (task == null || parent == null || !"Host".equals(task.getScope())
                || !Arrays.asList(MemoryBackendPreparationRules.ACTION, "apply", "clearOverride").contains(task.getAction())
                || task.getPolicyPlanHash() == null || parent.getParentUuid() != null || parent.getHostUuid() != null
                || !MemoryBackendPreparationRules.ACTION.equals(parent.getAction())
                || !"Host".equals(parent.getScope()) || !Objects.equals(task.getParentUuid(), parent.getUuid())
                || !Objects.equals(task.getHostUuid(), parent.getResourceUuid())) { return false; }
        try {
            JsonObject context = new JsonParser().parse(parent.getPolicy()).getAsJsonObject();
            return context.has("selectedBackend") && context.get("selectedBackend").isJsonObject()
                    && context.has("target") && context.get("target").isJsonObject()
                    && getString(context, "targetHash") != null && getString(context, "bootId") != null
                    && getString(context, "poolGeneration") != null;
        } catch (RuntimeException invalid) { return false; }
    }

    /** A user resume of a drained Host: its root records the drain-time pool and target like a capacity drain. */
    static boolean isDrainRecoveryMaintenance(MemoryTaskVO task, MemoryTaskVO parent) {
        if (task == null || parent == null || !"Host".equals(task.getScope())
                || !Arrays.asList(MemoryZramPoolPreparationRules.ACTION, "apply", "clearOverride").contains(task.getAction())
                || task.getPolicyPlanHash() == null || parent.getParentUuid() != null || parent.getHostUuid() != null
                || !"resume".equals(parent.getAction()) || !"Host".equals(parent.getScope())
                || !Objects.equals(task.getParentUuid(), parent.getUuid())
                || !Objects.equals(task.getHostUuid(), parent.getResourceUuid())) { return false; }
        try {
            JsonObject context = new JsonParser().parse(parent.getPolicy()).getAsJsonObject();
            return getBoolean(context, "drainRecovery")
                    && context.has("target") && context.get("target").isJsonObject()
                    && getString(context, "targetHash") != null && getString(context, "bootId") != null
                    && getString(context, "poolGeneration") != null;
        } catch (RuntimeException invalid) { return false; }
    }

    static boolean isAutomaticCapacityDrain(MemoryTaskVO task, MemoryTaskVO parent) {
        if (task == null || !"Host".equals(task.getScope()) || !"drain".equals(task.getAction())
                || task.getParentUuid() == null || task.getPolicyPlanHash() == null) { return false; }
        if (parent == null || parent.getParentUuid() != null || parent.getHostUuid() != null
                || !Objects.equals(task.getParentUuid(), parent.getUuid())
                || !Arrays.asList("apply", "clearOverride", MemoryBackendPreparationRules.ACTION)
                        .contains(parent.getAction())) { return false; }
        try {
            JsonObject policy = new JsonParser().parse(task.getPolicy()).getAsJsonObject();
            return policy.has("target") && policy.get("target").isJsonObject()
                    && getString(policy, "bootId") != null && getString(policy, "poolGeneration") != null;
        } catch (RuntimeException invalid) { return false; }
    }

    private static boolean ownsCapacityResult(MemoryTaskVO operation, MemoryTaskVO resultTask,
            MemoryStateVO host, String control) {
        if (host == null || !Objects.equals(resultTask.getUuid(), host.getActiveTaskUuid())
                || !Objects.equals(control, host.getControlOperationUuid())
                || !Objects.equals(operation.getHostUuid(), resultTask.getHostUuid())
                || !"Host".equals(resultTask.getScope())
                || operation.getDesiredRevision() != host.getDesiredRevision()
                || operation.getDesiredRevision() != resultTask.getDesiredRevision()
                || !Objects.equals(operation.getPolicyPlanHash(), host.getPolicyPlanHash())) { return false; }
        return Objects.equals(operation.getUuid(), resultTask.getUuid())
                || "reconcile".equals(resultTask.getAction())
                    && Objects.equals(operation.getUuid(), resultTask.getReconcileOperationUuid())
                    && Arrays.asList("Unknown", "Blocked").contains(operation.getStatus());
    }

    private MemoryTaskVO nextCapacityActivation(EntityManager em, MemoryTaskVO preparation, MemoryTaskVO resultTask, MemoryTaskVO drain,
            MemoryStateVO host, MemoryAgentResponse receipt, MemoryAgentResponse observation,
            Map<String, Object> preparedBackendConfig) {
        MemoryTaskVO root = preparation.getParentUuid() == null ? null
                : em.find(MemoryTaskVO.class, preparation.getParentUuid());
        boolean selectedBackend = isSelectedBackendMaintenance(preparation, root);
        String control = preparation.getExpectedControlOperationUuid();
        if (!ownsCapacityResult(preparation, resultTask, host, control)
                || !"Succeeded".equals(MemoryResultRules.status(preparation.getUuid(), -1, receipt))) {
            throw new IllegalArgumentException("CONTROL_OR_RESULT_MISMATCH");
        }
        JsonObject request = new JsonParser().parse(selectedBackend || isDrainRecoveryMaintenance(preparation, root)
                ? root.getPolicy() : drain.getPolicy()).getAsJsonObject();
        String boot = getString(request, "bootId"), pool = getString(request, "poolGeneration");
        boolean firstPool = selectedBackend && "none".equals(pool);
        String resourceUuid = null;
        Long capacityBytes = null;
        Map<?, ?> nativeReceipt = receipt.state == null ? null
                : MemoryWritebackBackendInventory.map(receipt.state.get("maintenance"));
        MemoryWritebackMaintenanceInventory maintenance = MemoryWritebackMaintenanceInventory.fromMap(nativeReceipt);
        if (maintenance == null || !"APPLIED".equals(maintenance.getStage())
                || !Objects.equals(preparation.getUuid(), maintenance.getRequestId())
                || !Objects.equals(boot, maintenance.getHostBootId())
                || !(firstPool ? maintenance.getPoolGeneration() == null || maintenance.getPoolGeneration().isEmpty()
                        : Objects.equals(pool, maintenance.getPoolGeneration()))) {
            throw new IllegalArgumentException("POOL_PREPARATION_RECEIPT_MISMATCH");
        }
        if (MemoryZramPoolPreparationRules.ACTION.equals(preparation.getAction())) {
            if (!Objects.equals(drain.getUuid(), nativeReceipt.get("expected_control_operation_uuid"))) {
                throw new IllegalArgumentException("POOL_PREPARATION_CONTROL_MISMATCH");
            }
        } else {
            JsonObject backendRequest = new JsonParser().parse(preparation.getPolicy()).getAsJsonObject();
            JsonElement owner = jsonPath(request, "target.writeback.backendResourceUuid");
            MemoryWritebackMaintenanceTargetIdentityInventory identity = maintenance.getTargetIdentity();
            if ((!selectedBackend && (owner == null || !Objects.equals(owner.getAsString(), maintenance.getBackendResourceUuid())))
                    || !Objects.equals(getString(backendRequest, "candidateId"), maintenance.getCandidateId())
                    || identity == null || !Objects.equals(boot, identity.getBootId())
                    || !Objects.equals(exactJsonLong(backendRequest.get("backendCapacityBytes")), identity.getCapacityBytes())) {
                throw new IllegalArgumentException("BACKEND_PREPARATION_RECEIPT_MISMATCH");
            }
            if (selectedBackend) {
                JsonObject stable = request.getAsJsonObject("selectedBackend").getAsJsonObject("stableIdentity");
                String inode = getString(stable, "sysfsInode");
                String stack = getString(stable, "majorMinor") + ":" + getString(stable, "sysfsPath") + ":" + inode + "\n";
                resourceUuid = maintenance.getBackendResourceUuid();
                capacityBytes = exactJsonLong(backendRequest.get("backendCapacityBytes"));
                if (!MemoryPolicyRules.validUuid(resourceUuid)
                        || !Objects.equals(getString(stable, "device"), identity.getCanonicalDevice())
                        || !Objects.equals(exactJsonLong(stable.get("rdev")), identity.getRdev())
                        || !Objects.equals(exactJsonLong(stable.get("nodeInode")), identity.getNodeInode())
                        || inode == null || !inode.matches("[0-9]+")
                        || !Objects.equals(Long.valueOf(inode), identity.getSysfsInode())
                        || !Objects.equals(digest(stack), identity.getStackSha256())
                        || !Boolean.FALSE.equals(identity.getDirectIo())) {
                    throw new IllegalArgumentException("SELECTED_BACKEND_RECEIPT_IDENTITY_MISMATCH");
                }
            }
        }
        long now = System.currentTimeMillis();
        if (observation == null || !observation.isSuccess() || !"Succeeded".equals(observation.status)
                || !Objects.equals(preparation.getHostUuid(), observation.hostUuid)
                || !Objects.equals(boot, observation.bootId) || observation.state == null
                || !Objects.equals(boot, observation.state.get("bootId"))
                || observation.sampleTime == null || observation.sampleTime > now
                || now - observation.sampleTime >= MemoryOptimizationGlobalConfig.displayTtlMillis()
                || host.getLastSampleTime() != null && observation.sampleTime < host.getLastSampleTime()) {
            throw new IllegalArgumentException("CURRENT_POOL_PREPARATION_NOT_PROVEN");
        }
        Map<?, ?> proof = MemoryWritebackBackendInventory.map(observation.state.get("maintenanceProof"));
        String state = JSONObjectUtil.toJsonString(observation.state);
        if (firstPool) {
            JsonObject current = new JsonParser().parse(state).getAsJsonObject();
            String currentPool = getString(current, "poolGeneration");
            JsonElement enabled = jsonPath(current, "actual.zram.efficiencyEnabled");
            // The original KSM-only control (including an effective empty KSM
            // policy) is not proof of today's native settings. Current-boot
            // preparation receipt plus fresh inactive/no-pool observation
            // prove this device operation; never promote an older KSM ACK.
            boolean firstControl = isFirstPoolNonZramControl(em, host, boot);
            if (!firstControl || currentPool != null && !"none".equals(currentPool)
                    || enabled == null || !enabled.isJsonPrimitive()
                    || !enabled.getAsJsonPrimitive().isBoolean() || enabled.getAsBoolean()) {
                throw new IllegalArgumentException("FIRST_POOL_PREPARATION_NOT_PROVEN");
            }
        } else if (drain == null || !"drain".equals(drain.getAction()) || !"Succeeded".equals(drain.getStatus())
                || !Objects.equals(preparation.getHostUuid(), drain.getHostUuid())
                || proof == null || !Objects.equals(pool, proof.get("oldPoolGeneration"))
                || !isMaintenanceRecoveryState(state, control)) {
            throw new IllegalArgumentException("POOL_PREPARATION_ARCHIVE_NOT_PROVEN");
        }
        // Reuse the normal activation planner on the newly observed facts;
        // do not rewrite the managed Host until all proof and planning pass.
        MemoryStateVO observed = new MemoryStateVO();
        observed.setHostUuid(host.getHostUuid()); observed.setStatus("Succeeded");
        observed.setDesiredRevision(host.getDesiredRevision()); observed.setAppliedRevision(host.getAppliedRevision());
        observed.setAppliedPolicy(host.getAppliedPolicy()); observed.setAppliedPolicyHash(host.getAppliedPolicyHash());
        observed.setControlOperationUuid(control); observed.setState(state);
        observed.setLastSampleTime(observation.sampleTime);
        observed.setCapabilities(observation.capabilities == null ? null : JSONObjectUtil.toJsonString(observation.capabilities));
        JsonObject target = explicitlyManagedPolicy(em, host.getHostUuid(), effective(em, "Host", host.getHostUuid()));
        boolean savedConfiguration = firstPool && isConfigurationReconcile(resultTask);
        if (savedConfiguration) {
            // A newer ordinary save owns desired policy. Device selection is
            // still the original immutable request; it cannot overwrite a
            // separately changed backend binding while completing its receipt.
            for (String field : Arrays.asList("writeback.backendResourceUuid", "writeback.backendCapacityBytes")) {
                if (!Objects.equals(jsonPath(target, field), jsonPath(request, "target." + field))) {
                    throw new IllegalArgumentException("MAINTENANCE_BACKEND_TARGET_CHANGED");
                }
            }
        } else if (!Objects.equals(digest(target.toString()), getString(request, "targetHash"))) {
            throw new IllegalArgumentException("MAINTENANCE_TARGET_CHANGED");
        }
        boolean retainDisabled = selectedBackend && Boolean.FALSE.equals(
                MemoryStandardConfigCodec.get(effective(em, "Host", host.getHostUuid()), "zram.enabled"));
        if (selectedBackend) {
            JsonElement backend = jsonPath(new JsonParser().parse(state).getAsJsonObject(),
                    "actual.writeback.backend.backend_resource");
            if (backend == null || !backend.isJsonObject()
                    || !Objects.equals(resourceUuid, getString(backend.getAsJsonObject(), "resource_uuid"))
                    || !Objects.equals(capacityBytes, exactJsonLong(backend.getAsJsonObject().get("capacity_bytes")))
                    || !"PREPARED".equals(getString(backend.getAsJsonObject(), "state"))
                    || !Boolean.FALSE.equals(MemoryWritebackBackendInventory.bool(
                            JSONObjectUtil.toObject(backend.toString(), Map.class).get("active")))) {
                throw new IllegalArgumentException("CURRENT_SELECTED_BACKEND_NOT_PROVEN");
            }
            JsonObject binding = new JsonObject();
            setJsonPath(binding, "writeback.backendResourceUuid", new com.google.gson.JsonPrimitive(resourceUuid));
            setJsonPath(binding, "writeback.backendCapacityBytes", new com.google.gson.JsonPrimitive(capacityBytes));
            validateConfigurationCandidate(em, "Host", host.getHostUuid(), candidateOwnPolicy(
                    em, "Host", host.getHostUuid(), binding.toString()));
            setJsonPath(target, "writeback.backendResourceUuid", jsonPath(binding, "writeback.backendResourceUuid"));
            setJsonPath(target, "writeback.backendCapacityBytes", jsonPath(binding, "writeback.backendCapacityBytes"));
            if (retainDisabled) {
                observed.setAppliedPolicy(MemoryPolicyRules.merge(
                        observed.getAppliedPolicy() == null ? "{}" : observed.getAppliedPolicy(), binding.toString(), "Host"));
            }
            // Match explicitlyManagedPolicy's schema-first, sorted field order.
            // Appending a new mechanism must not give the same saved target a
            // different hash at the next claim.
            JsonObject sorted = new JsonParser().parse(MemoryPolicyRules.canonicalRequest(target)).getAsJsonObject();
            target = new JsonObject(); target.addProperty("schemaVersion", 1);
            for (Map.Entry<String, JsonElement> field : sorted.entrySet()) {
                target.add(field.getKey(), field.getValue());
            }
        }
        HostPolicyPlan plan = planForHost(em, host.getHostUuid(), observed, target,
                effective(em, "Host", host.getHostUuid()), true);
        if (firstPool ? plan.hasBlockers || plan.capacityMaintenance != null
                : retainDisabled ? plan.hasBlockers || plan.hasPayload || plan.capacityMaintenance != null
                : !plan.maintenanceActivation || !plan.hasPayload) {
            throw new IllegalArgumentException("MAINTENANCE_ACTIVATION_NOT_ADMITTED");
        }
        recordPolicyPlan(host, plan, plan.hasPayload ? "Queued" : "Applied");
        if (selectedBackend) {
            preparedBackendConfig.put("writeback.backendResourceUuid", resourceUuid);
            preparedBackendConfig.put("writeback.backendCapacityBytes", capacityBytes);
        }
        if (!plan.hasPayload) { return null; }
        MemoryTaskVO next = configurationTask("Host", host.getHostUuid(),
                plan.replacesAppliedPolicy ? "clearOverride" : "apply", preparation.getParentUuid(), host.getHostUuid(),
                Math.addExact(preparation.getDesiredRevision(), 1L), plan.payload, preparation.getActorUuid());
        next.setExpectedControlOperationUuid(plan.maintenanceActivation ? control : plan.expectedControlOperationUuid);
        next.setPolicyPlanHash(plan.planHash);
        return next;
    }

    private MemoryTaskVO nextCapacityWriteback(EntityManager em, MemoryTaskVO activation, MemoryTaskVO resultTask, MemoryTaskVO drain,
            MemoryStateVO host, MemoryAgentResponse receipt, MemoryAgentResponse observation) {
        if (!ownsCapacityResult(activation, resultTask, host, resultTask.getUuid())
                || !"Succeeded".equals(MemoryResultRules.status(activation.getUuid(), activation.getDesiredRevision(), receipt))) {
            throw new IllegalArgumentException("CONTROL_OR_RESULT_MISMATCH");
        }
        MemoryTaskVO root = activation.getParentUuid() == null ? null
                : em.find(MemoryTaskVO.class, activation.getParentUuid());
        JsonObject request = new JsonParser().parse(isSelectedBackendMaintenance(activation, root)
                || isDrainRecoveryMaintenance(activation, root) ? root.getPolicy() : drain.getPolicy()).getAsJsonObject();
        String boot = getString(request, "bootId");
        long now = System.currentTimeMillis();
        if (observation == null || !observation.isSuccess() || !"Succeeded".equals(observation.status)
                || !Objects.equals(activation.getHostUuid(), observation.hostUuid)
                || !Objects.equals(boot, observation.bootId) || observation.state == null
                || !Objects.equals(boot, observation.state.get("bootId"))
                || !Objects.equals(activation.getUuid(), observation.state.get("lastConfirmedOperationUuid"))
                || !numberEquals(observation.state.get("lastConfirmedAppliedRevision"), activation.getDesiredRevision())
                || observation.appliedRevision == null || observation.appliedRevision != activation.getDesiredRevision()
                || observation.sampleTime == null || observation.sampleTime > now
                || now - observation.sampleTime >= MemoryOptimizationGlobalConfig.displayTtlMillis()
                || host.getLastSampleTime() != null && observation.sampleTime < host.getLastSampleTime()) {
            throw new IllegalArgumentException("CURRENT_ACTIVATION_NOT_PROVEN");
        }
        JsonObject current = new JsonParser().parse(JSONObjectUtil.toJsonString(observation.state)).getAsJsonObject();
        String generation = getString(current, "poolGeneration");
        JsonElement efficiency = jsonPath(current, "actual.zram.efficiencyEnabled");
        if (generation == null || !generation.matches("[a-fA-F0-9]{64}")
                || generation.equals(getString(request, "poolGeneration"))
                || efficiency == null || !efficiency.isJsonPrimitive()
                || !efficiency.getAsJsonPrimitive().isBoolean() || !efficiency.getAsBoolean()) {
            throw new IllegalArgumentException("NEW_POOL_NOT_PROVEN");
        }
        for (String[] field : Arrays.asList(new String[]{"logicalCapacityBytes", "effective_logical_bytes"},
                new String[]{"ramLimitBytes", "effective_physical_limit_bytes"})) {
            Long desired = exactJsonLong(jsonPath(request, "target.zram." + field[0]));
            if (desired != null && !desired.equals(exactJsonLong(jsonPath(current, "actual.zram.capacity." + field[1])))) {
                throw new IllegalArgumentException("NEW_POOL_CAPACITY_MISMATCH");
            }
        }
        MemoryStateVO observed = new MemoryStateVO();
        observed.setHostUuid(host.getHostUuid()); observed.setStatus("Succeeded");
        observed.setDesiredRevision(activation.getDesiredRevision()); observed.setAppliedRevision(activation.getDesiredRevision());
        observed.setAppliedPolicy("clearOverride".equals(activation.getAction()) ? activation.getPolicy()
                : MemoryPolicyRules.merge(host.getAppliedPolicy() == null ? "{}" : host.getAppliedPolicy(), activation.getPolicy(), "Host"));
        observed.setControlOperationUuid(activation.getUuid()); observed.setState(current.toString());
        observed.setLastSampleTime(observation.sampleTime);
        observed.setCapabilities(observation.capabilities == null ? null : JSONObjectUtil.toJsonString(observation.capabilities));
        HostPolicyPlan plan = planForHost(em, host.getHostUuid(), effective(em, "Host", host.getHostUuid()), observed);
        String targetHash = request.has("selectedBackend") ? host.getPolicyTargetHash() : getString(request, "targetHash");
        if (!Objects.equals(plan.targetHash, targetHash) || plan.hasBlockers
                || plan.maintenanceActivation || plan.capacityMaintenance != null) {
            throw new IllegalArgumentException("RETAINED_TARGET_NOT_ADMITTED");
        }
        recordPolicyPlan(host, plan, plan.hasPayload ? "Queued" : "Applied");
        if (!plan.hasPayload) { return null; }
        MemoryTaskVO next = configurationTask("Host", host.getHostUuid(),
                plan.replacesAppliedPolicy ? "clearOverride" : "apply", activation.getParentUuid(), host.getHostUuid(),
                Math.addExact(activation.getDesiredRevision(), 1L), plan.payload, activation.getActorUuid());
        next.setPolicyPlanHash(plan.planHash);
        return next;
    }

    /** The Host's current control is a completed drain and nothing is in flight: only a recovery can move it on. */
    public boolean isDrainedHost(String hostUuid) {
        return transaction(em -> drainControl(em, em.find(MemoryStateVO.class, hostUuid)) != null);
    }

    /** Drained Hosts under the changed resource whose effective target enables ZRAM again. */
    public List<String> drainedHostsWithZramEnabled(String resourceUuid) {
        return transaction(em -> {
            List<String> hosts = new ArrayList<>();
            for (String host : resourceUuid == null ? targets(em, "Global", "global")
                    : targets(em, resourceScope(em, resourceUuid), resourceUuid)) {
                if (drainControl(em, em.find(MemoryStateVO.class, host)) != null
                        && Boolean.TRUE.equals(MemoryStandardConfigCodec.get(effective(em, "Host", host), "zram.enabled"))) {
                    hosts.add(host);
                }
            }
            return hosts;
        });
    }

    private static MemoryTaskVO drainControl(EntityManager em, MemoryStateVO state) {
        if (state == null || state.getActiveTaskUuid() != null || state.getControlOperationUuid() == null) { return null; }
        MemoryTaskVO control = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
        return drainControl(em, state, control) ? control : null;
    }

    private static boolean drainControl(EntityManager em, MemoryStateVO state, MemoryTaskVO control) {
        return control != null && "drain".equals(control.getAction()) && "Succeeded".equals(control.getStatus())
                && Objects.equals(state.getHostUuid(), control.getHostUuid());
    }

    private String resourceScope(EntityManager em, String resourceUuid) {
        String type = em.createQuery("select r.resourceType from ResourceVO r where r.uuid = :uuid", String.class)
                .setParameter("uuid", resourceUuid).getResultList().stream().findFirst().orElse(null);
        return "ClusterVO".equals(type) ? "Cluster" : "Host";
    }

    /**
     * Resume a drained Host from a fresh Agent inventory. A device the Agent proves
     * blank (including after a reboot) is activated directly; a withdrawn pool still
     * held in the drain's boot is reset through the existing preparation chain first.
     */
    public MemoryTaskInventory recoverDrain(String hostUuid, String actor, String requestKey,
            MemoryAgentResponse observation) {
        return transaction(em -> {
            lock(em);
            if (requestKey != null) {
                List<MemoryTaskVO> previous = em.createQuery("from MemoryTaskVO t where t.requestKey = :key",
                        MemoryTaskVO.class).setParameter("key", requestKey).getResultList();
                if (!previous.isEmpty()) { return previous.get(0).toInventory(); }
            }
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
            MemoryTaskVO drain = drainControl(em, state);
            if (drain == null) {
                throw error("MEMORY_CONTROL_OPERATION_FENCED", "Host is not in a completed drain: " + hostUuid);
            }
            if (observation == null || !observation.isSuccess() || !"Succeeded".equals(observation.status)) {
                throw error("MEMORY_DRAIN_RECOVERY_UNAVAILABLE", "Host maintenance inventory is unavailable: " + hostUuid);
            }
            MemoryWritebackBackendInventory inventory = MemoryWritebackBackendInventory.fromAgentState(
                    hostUuid, observation.hostUuid, observation.state);
            long now = System.currentTimeMillis(), observedAt;
            try { observedAt = java.time.OffsetDateTime.parse(inventory.getObservedAt()).toInstant().toEpochMilli(); }
            catch (RuntimeException invalid) { observedAt = -1; }
            String boot = getString(new JsonParser().parse(state.getState()).getAsJsonObject(), "bootId");
            if (observedAt < 0 || observedAt > now || now - observedAt >= MemoryOptimizationGlobalConfig.displayTtlMillis()
                    || boot == null || !boot.equals(inventory.getBootId())
                    || !Objects.equals(drain.getUuid(), inventory.getControlOperationUuid())) {
                throw error("MEMORY_DRAIN_RECOVERY_UNAVAILABLE", "Host maintenance inventory is not current: " + hostUuid);
            }
            String effective = effective(em, "Host", hostUuid);
            JsonObject target = explicitlyManagedPolicy(em, hostUuid, effective);
            String pool = inventory.getPoolGeneration();
            boolean zram = Boolean.TRUE.equals(MemoryStandardConfigCodec.get(effective, "zram.enabled"));
            boolean preservedPool = zram && pool != null && pool.matches("[a-f0-9]{64}")
                    && Boolean.TRUE.equals(inventory.getCanPrepareZramPool())
                    && "Fresh".equals(inventory.getZramPoolPreparationQuality());
            boolean readyForActivation = zram && isMaintenanceRecoveryState(state.getState(), drain.getUuid());
            if (!readyForActivation && !preservedPool) {
                throw error("MEMORY_DRAIN_RECOVERY_UNAVAILABLE", "Drained Host cannot be recovered now: "
                        + inventory.getZramPoolPreparationReasons());
            }
            JsonObject context = new JsonObject();
            if (preservedPool) {
                context.addProperty("drainRecovery", true);
                context.add("target", target.deepCopy());
                context.addProperty("targetHash", digest(target.toString()));
                context.addProperty("bootId", boot); context.addProperty("poolGeneration", pool);
            }
            MemoryTaskVO root = configurationTask("Host", hostUuid, "resume", null, null,
                    state.getDesiredRevision(), context.toString(), actor);
            root.setRequestKey(requestKey); root.setRequestHash(requestKey == null ? null : digest(requestKey));
            em.persist(root);
            long revision = Math.addExact(state.getDesiredRevision(), 1L);
            MemoryTaskVO child;
            if (readyForActivation) {
                HostPolicyPlan plan = planForHost(em, hostUuid, state, target, effective, true);
                if (!plan.maintenanceActivation || !plan.hasPayload) {
                    throw error("MEMORY_DRAIN_RECOVERY_UNAVAILABLE", "Host activation is not admitted: " + plan.blockedFields);
                }
                child = configurationTask("Host", hostUuid, plan.replacesAppliedPolicy ? "clearOverride" : "apply",
                        root.getUuid(), hostUuid, revision, plan.payload, actor);
                child.setPolicyPlanHash(plan.planHash);
                recordPolicyPlan(state, plan, "Queued");
                state.setControlOperationUuid(child.getUuid());
            } else {
                HostPolicyPlan plan = planForHost(em, hostUuid, effective, state);
                MemoryZramPoolPreparation preparation = new MemoryZramPoolPreparation();
                preparation.setExpectedHostBootId(boot); preparation.setExpectedPoolGeneration(pool);
                preparation.setResetConfirmed(true);
                MemoryZramPoolPreparationRules.validate(preparation, drain.getUuid());
                child = configurationTask("Host", hostUuid, MemoryZramPoolPreparationRules.ACTION, root.getUuid(),
                        hostUuid, revision, JSONObjectUtil.toJsonString(preparation), actor);
                child.setPolicyPlanHash(plan.planHash);
                recordPolicyPlan(state, plan, "Queued");
            }
            child.setExpectedControlOperationUuid(drain.getUuid());
            em.persist(child);
            state.setActiveTaskUuid(child.getUuid()); state.setStatus("Queued"); state.setReason(null);
            state.setDesiredRevision(revision); state.setPermitAuthorized(false);
            bumpQueryVersion(em, "task"); bumpQueryVersion(em, "state");
            em.flush();
            return root.toInventory();
        });
    }

    private MemoryTaskVO nextCapacityPreparation(EntityManager em, MemoryTaskVO drain, MemoryTaskVO resultTask, MemoryStateVO host,
            MemoryAgentResponse receipt, MemoryAgentResponse observation) {
        if (!ownsCapacityResult(drain, resultTask, host, resultTask.getUuid())
                || !"Succeeded".equals(MemoryResultRules.status(drain.getUuid(), -1, receipt))) {
            throw new IllegalArgumentException("CONTROL_OR_RESULT_MISMATCH");
        }
        JsonObject request = new JsonParser().parse(drain.getPolicy()).getAsJsonObject();
        String boot = getString(request, "bootId"), pool = getString(request, "poolGeneration");
        String device = getString(request, "device");
        for (String field : Arrays.asList("receipt", "nativeReadback")) {
            Object value = receipt.state == null ? null : receipt.state.get(field);
            if (!(value instanceof Map)) { throw new IllegalArgumentException("DRAIN_NATIVE_PROOF_MISSING"); }
            Map<?, ?> nativeProof = (Map<?, ?>) value;
            if (device == null || !device.matches("/dev/zram[0-9]+")
                    || !Objects.equals(device, nativeProof.get("device"))
                    || !Objects.equals(pool, nativeProof.get("pool_generation"))
                    || !"WITHDRAWN_SWAP_INACTIVE_POOL_PRESERVED".equals(nativeProof.get("state"))
                    || !Boolean.FALSE.equals(nativeProof.get("active"))
                    || !Boolean.FALSE.equals(nativeProof.get("reset_performed"))) {
                throw new IllegalArgumentException("DRAIN_NATIVE_PROOF_MISMATCH");
            }
        }
        String current = explicitlyManagedPolicy(em, drain.getHostUuid(),
                effective(em, "Host", drain.getHostUuid())).toString();
        if (!Objects.equals(digest(current), getString(request, "targetHash"))
                || !Objects.equals(boot, getString(new JsonParser().parse(host.getState()).getAsJsonObject(), "bootId"))) {
            throw new IllegalArgumentException("MAINTENANCE_TARGET_OR_BOOT_CHANGED");
        }
        if (observation == null || !observation.isSuccess() || !"Succeeded".equals(observation.status)) {
            throw new IllegalArgumentException("MAINTENANCE_OBSERVATION_UNAVAILABLE");
        }
        MemoryWritebackBackendInventory inventory = MemoryWritebackBackendInventory.fromAgentState(
                drain.getHostUuid(), observation.hostUuid, observation.state);
        long now = System.currentTimeMillis();
        long observedAt;
        try { observedAt = java.time.OffsetDateTime.parse(inventory.getObservedAt()).toInstant().toEpochMilli(); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("MAINTENANCE_OBSERVATION_TIME_UNKNOWN"); }
        if (!Objects.equals(boot, inventory.getBootId()) || !Objects.equals(pool, inventory.getPoolGeneration())
                || !Objects.equals(drain.getUuid(), inventory.getControlOperationUuid())
                || observedAt > now || now - observedAt >= MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            throw new IllegalArgumentException("CURRENT_MAINTENANCE_IDENTITY_NOT_PROVEN");
        }
        APIUpdateMemoryPolicyMsg validation = new APIUpdateMemoryPolicyMsg();
        validation.setScope("Host");
        validation.setPolicy("{}"); validation.setExpectedControlOperationUuid(drain.getUuid());
        String preparation;
        JsonElement backendUuid = jsonPath(request, "target.writeback.backendResourceUuid");
        if (request.has("selectedBackend")) {
            JsonObject selected = request.getAsJsonObject("selectedBackend");
            JsonObject original = request.getAsJsonObject("backendPreparation");
            Long bytes = exactJsonLong(original.get("backendCapacityBytes"));
            String candidateId = null;
            Object candidates = observation.state.get("candidates");
            if (Boolean.TRUE.equals(inventory.getCanPrepare()) && candidates instanceof List) {
                for (Object value : (List<?>) candidates) {
                    Map<?, ?> candidate = MemoryWritebackBackendInventory.map(value);
                    if (candidate == null || !"block".equals(candidate.get("type"))
                            || !Boolean.TRUE.equals(candidate.get("eligible"))
                            || !Boolean.FALSE.equals(candidate.get("writebackReady"))
                            || !"NEEDS_OFFLINE_PREPARATION".equals(candidate.get("state"))
                            || candidate.get("resourceUuid") != null
                            || !Objects.equals(boot, candidate.get("hostBootId"))
                            || !numberEquals(candidate.get("capacityBytes"), bytes)
                            || !Objects.equals(getString(selected, "device"), candidate.get("device"))
                            || !selected.get("stableIdentity").equals(new JsonParser().parse(
                                    JSONObjectUtil.toJsonString(candidate.get("stableIdentity"))))) { continue; }
                    String id = MemoryWritebackBackendInventory.string(candidate.get("identity"));
                    if (id == null || !id.matches("candidate:[a-f0-9]{64}")
                            || !id.equals("candidate:" + candidate.get("fingerprint"))) { continue; }
                    if (candidateId != null) { throw new IllegalArgumentException("BACKEND_CANDIDATE_AMBIGUOUS"); }
                    candidateId = id;
                }
            }
            if (candidateId == null || !getBoolean(original, "resetConfirmed")) {
                throw new IllegalArgumentException("SELECTED_BACKEND_CANDIDATE_NOT_PROVEN");
            }
            MemoryBackendPreparation selectedPreparation = new MemoryBackendPreparation();
            selectedPreparation.setCandidateId(candidateId); selectedPreparation.setExpectedHostBootId(boot);
            selectedPreparation.setExpectedPoolGeneration(pool); selectedPreparation.setBackendCapacityBytes(bytes);
            selectedPreparation.setResetConfirmed(true);
            validation.setAction(MemoryBackendPreparationRules.ACTION);
            validation.setBackendPreparation(selectedPreparation); MemoryBackendPreparationRules.validate(validation);
            preparation = JSONObjectUtil.toJsonString(selectedPreparation);
        } else if (request.has("backend") || backendUuid != null) {
            // A service-owned backend is not a blank RAM-only pool. Bind the
            // original owner proof to the fresh local probe's exact identity;
            // service and probe fingerprints are deliberately not interchangeable.
            JsonObject backend = request.has("backend") && request.get("backend").isJsonObject()
                    ? request.getAsJsonObject("backend") : null;
            if (backend != null && backend.has("backend_resource") && backend.get("backend_resource").isJsonObject()) {
                // Drain requests persisted by earlier versions carry the whole backend view.
                backend = backend.getAsJsonObject("backend_resource");
            }
            Map<?, ?> owner = MemoryWritebackBackendInventory.map(receipt.state.get("backendOwnerProof"));
            Map<?, ?> identity = owner == null ? null : MemoryWritebackBackendInventory.map(owner.get("stableIdentity"));
            Long bytes = exactJsonLong(jsonPath(request, "target.writeback.backendCapacityBytes"));
            String resource = backend == null ? null : getString(backend, "resource_uuid");
            String fingerprint = backend == null ? null : getString(backend, "fingerprint");
            if (backendUuid == null || resource == null || !resource.equals(backendUuid.getAsString())
                    || bytes == null || !bytes.equals(exactJsonLong(backend.get("capacity_bytes")))
                    || fingerprint == null || !fingerprint.matches("[a-f0-9]{64}")
                    || owner == null || identity == null || !numberEquals(owner.get("schemaVersion"), 1)
                    || !Objects.equals(drain.getHostUuid(), owner.get("hostUuid"))
                    || !Objects.equals(drain.getUuid(), owner.get("controlOperationUuid"))
                    || !Objects.equals(boot, owner.get("bootId")) || !Objects.equals(pool, owner.get("poolGeneration"))
                    || !Objects.equals(device, owner.get("zramDevice"))
                    || !Objects.equals(resource, owner.get("resourceUuid"))
                    || !Objects.equals(resource, owner.get("ownershipGeneration"))
                    || !Objects.equals(fingerprint, owner.get("fingerprint"))
                    || !Objects.equals(fingerprint, owner.get("candidateIdentity"))
                    || !"ACTIVE".equals(owner.get("candidateState")) || !numberEquals(owner.get("capacityBytes"), bytes)
                    || !Boolean.TRUE.equals(inventory.getCanPrepare())) {
                throw new IllegalArgumentException("ORIGINAL_BACKEND_OWNER_NOT_PROVEN");
            }
            JsonElement originalIdentity = new JsonParser().parse(JSONObjectUtil.toJsonString(identity));
            String candidateId = null;
            Object candidates = observation.state.get("candidates");
            if (candidates instanceof List) {
                for (Object value : (List<?>) candidates) {
                    Map<?, ?> candidate = MemoryWritebackBackendInventory.map(value);
                    if (candidate == null || !"block".equals(candidate.get("type"))
                            || !Boolean.TRUE.equals(candidate.get("eligible"))
                            || !Boolean.FALSE.equals(candidate.get("writebackReady"))
                            || !"NEEDS_OFFLINE_PREPARATION".equals(candidate.get("state"))
                            || candidate.get("resourceUuid") != null
                            || !Objects.equals(boot, candidate.get("hostBootId"))
                            || !numberEquals(candidate.get("capacityBytes"), bytes)
                            || !Objects.equals(identity.get("device"), candidate.get("device"))
                            || !originalIdentity.equals(new JsonParser().parse(
                                    JSONObjectUtil.toJsonString(candidate.get("stableIdentity"))))) { continue; }
                    String id = MemoryWritebackBackendInventory.string(candidate.get("identity"));
                    if (id == null || !id.matches("candidate:[a-f0-9]{64}")
                            || !id.equals("candidate:" + candidate.get("fingerprint"))) { continue; }
                    if (candidateId != null) { throw new IllegalArgumentException("BACKEND_CANDIDATE_AMBIGUOUS"); }
                    candidateId = id;
                }
            }
            if (candidateId == null) { throw new IllegalArgumentException("ORIGINAL_BACKEND_CANDIDATE_NOT_PROVEN"); }
            MemoryBackendPreparation requestPreparation = new MemoryBackendPreparation();
            requestPreparation.setCandidateId(candidateId); requestPreparation.setExpectedHostBootId(boot);
            requestPreparation.setExpectedPoolGeneration(pool); requestPreparation.setBackendCapacityBytes(bytes);
            requestPreparation.setResetConfirmed(true);
            validation.setAction(MemoryBackendPreparationRules.ACTION);
            validation.setBackendPreparation(requestPreparation); MemoryBackendPreparationRules.validate(validation);
            preparation = JSONObjectUtil.toJsonString(requestPreparation);
        } else {
            Long poolObservedAt = inventory.getZramPoolPreparationObservedAt();
            if (poolObservedAt == null || poolObservedAt > now
                    || now - poolObservedAt >= MemoryOptimizationGlobalConfig.displayTtlMillis()
                    || !Boolean.TRUE.equals(inventory.getCanPrepareZramPool())
                    || !"Fresh".equals(inventory.getZramPoolPreparationQuality())) {
                throw new IllegalArgumentException("CURRENT_POOL_PREPARATION_NOT_PROVEN");
            }
            MemoryZramPoolPreparation requestPreparation = new MemoryZramPoolPreparation();
            requestPreparation.setExpectedHostBootId(boot); requestPreparation.setExpectedPoolGeneration(pool);
            requestPreparation.setResetConfirmed(true);
            validation.setAction(MemoryZramPoolPreparationRules.ACTION);
            MemoryZramPoolPreparationRules.validate(requestPreparation, drain.getUuid());
            preparation = JSONObjectUtil.toJsonString(requestPreparation);
        }
        MemoryTaskVO next = configurationTask("Host", drain.getHostUuid(), validation.getAction(),
                drain.getParentUuid(), drain.getHostUuid(), Math.addExact(drain.getDesiredRevision(), 1L),
                preparation, drain.getActorUuid());
        next.setExpectedControlOperationUuid(drain.getUuid()); next.setPolicyPlanHash(drain.getPolicyPlanHash());
        next.setStatus("Queued");
        return next;
    }

    /** Only the local pre-dispatch License branch may use this method.
     * No Agent call was made: fail the NEW request and restore the old slot,
     * without pretending the historical activation has a known outcome. */
    public void recoveryLicenseExpiredBeforeDispatch(String uuid) {
        transaction(em -> {
            lock(em);
            MemoryTaskVO task = em.find(MemoryTaskVO.class, uuid);
            if (task == null || !MemoryUncertainRecoveryRules.ACTION.equals(task.getAction())
                    || !"Applying".equals(task.getStatus())) { return null; }
            MemoryStateVO host = em.find(MemoryStateVO.class, task.getHostUuid());
            MemoryTaskVO original = em.find(MemoryTaskVO.class, task.getExpectedControlOperationUuid());
            if (host == null || original == null || !"Unknown".equals(original.getStatus())
                    || !Objects.equals(task.getUuid(), host.getActiveTaskUuid())
                    || !Objects.equals(original.getUuid(), host.getControlOperationUuid())
                    || !Objects.equals(task.getHostUuid(), original.getHostUuid())) { return null; }
            task.setStatus("Failed"); task.setReason("CLOUD_LICENSE_EXPIRED_BEFORE_RECOVERY_DISPATCH");
            bumpQueryVersion(em, "task");
            host.setActiveTaskUuid(original.getUuid()); host.setStatus("Unknown"); host.setReason(original.getReason());
            queueFailure(em, MemoryTaskFailureEvent.fromTask(task));
            em.flush(); aggregateParent(em, task.getParentUuid());
            return null;
        });
    }

    private void queueFailure(EntityManager em, MemoryTaskFailureEvent failureEvent) {
        if (failureEvent == null || em.find(MemoryTaskFailureOutboxVO.class, failureEvent.getTaskUuid()) != null) { return; }
        MemoryTaskFailureOutboxVO outbox = new MemoryTaskFailureOutboxVO();
        outbox.setTaskUuid(failureEvent.getTaskUuid()); outbox.setHostUuid(failureEvent.getHostUuid());
        outbox.setAction(failureEvent.getAction()); outbox.setReason(trim(failureEvent.getReason()));
        outbox.setCreateDate(new Timestamp(System.currentTimeMillis()));
        outbox.setAttempts(0); outbox.setDelivered(false); em.persist(outbox);
    }

    private static MemoryTaskVO uncertainRecoveryForResult(EntityManager em, MemoryTaskVO task) {
        if (task == null) { return null; }
        if (MemoryUncertainRecoveryRules.ACTION.equals(task.getAction())) { return task; }
        if (!"reconcile".equals(task.getAction()) || task.getReconcileOperationUuid() == null) { return null; }
        MemoryTaskVO original = em.find(MemoryTaskVO.class, task.getReconcileOperationUuid());
        return original != null && MemoryUncertainRecoveryRules.ACTION.equals(original.getAction()) ? original : null;
    }

    /**
     * An ordinary explicit Host apply has no policyPlanHash. If it exactly
     * applies the policy that was held for legacy review, it is sufficient
     * evidence to close that review. Do not infer success from unrelated,
     * stale, partial, or non-policy control operations.
     */
    private void resolveLegacyPolicyReviewAfterApply(EntityManager em, MemoryTaskVO task, MemoryStateVO state) {
        if (!"Host".equals(task.getScope()) || !Objects.equals(task.getResourceUuid(), task.getHostUuid())
                || task.getPolicyPlanHash() != null || task.getDesiredRevision() != state.getDesiredRevision()
                || !"NeedsReview".equals(state.getPolicyPlanStatus())
                || !hasLegacyPolicyReviewBlocker(state.getPolicyBlockedFields())) {
            return;
        }
        String appliedHash = digest(task.getPolicy());
        String currentEffective = effective(em, "Host", task.getHostUuid());
        if (!Objects.equals(state.getPolicyTargetHash(), appliedHash)
                || !Objects.equals(appliedHash, digest(currentEffective))
                || !Objects.equals(state.getAppliedPolicyHash(), appliedHash)) {
            return;
        }
        HostPolicyPlan plan = planForHost(em, task.getHostUuid(), currentEffective, state);
        if (!plan.hasPayload && !plan.hasBlockers) {
            recordPolicyPlan(state, plan, "Applied");
        }
    }

    private static boolean hasLegacyPolicyReviewBlocker(String blockedFields) {
        if (blockedFields == null) { return false; }
        try {
            JsonElement parsed = new JsonParser().parse(blockedFields);
            if (!parsed.isJsonObject()) { return false; }
            JsonElement policy = parsed.getAsJsonObject().get("policy");
            return policy != null && policy.isJsonObject()
                    && "LEGACY_APPLIED_POLICY_UNKNOWN".equals(
                            policy.getAsJsonObject().get("reason").getAsString());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static MemoryTaskVO rejectedResumeForReconcile(EntityManager em, MemoryTaskVO task) {
        if (task == null || !"reconcile".equals(task.getAction()) || task.getReconcileOperationUuid() == null) {
            return null;
        }
        MemoryTaskVO original = em.find(MemoryTaskVO.class, task.getReconcileOperationUuid());
        return original != null && "resume".equals(original.getAction())
                && "Host".equals(original.getScope()) && "Host".equals(task.getScope())
                && "Failed".equals(original.getStatus())
                && "CONTROL_OPERATION_FENCED".equals(original.getReason())
                && Objects.equals(original.getExpectedControlOperationUuid(), task.getExpectedControlOperationUuid())
                && Objects.equals(original.getHostUuid(), task.getHostUuid()) ? original : null;
    }

    private static boolean isRejectedResumeNotIssuedProof(EntityManager em, MemoryTaskVO reconcile,
                                                           MemoryTaskVO rejectedResume, MemoryTaskVO prior,
                                                           MemoryStateVO host, MemoryAgentResponse response) {
        if (host == null || response == null || !response.isSuccess()
                || !"Succeeded".equals(response.status)
                || !Objects.equals(rejectedResume.getUuid(), response.operationUuid)
                || !Objects.equals(reconcile.getUuid(), host.getActiveTaskUuid())
                || !Objects.equals(rejectedResume.getUuid(), host.getControlOperationUuid())
                || !Objects.equals(reconcile.getExpectedControlOperationUuid(), rejectedResume.getExpectedControlOperationUuid())
                || !isPauseOrDrain(prior, reconcile.getHostUuid())) { return false; }
        try {
            Map<?, ?> before = JSONObjectUtil.toObject(host.getState(), Map.class);
            Object bootValue = before.get("bootId");
            Object lastControl = before.get("lastConfirmedOperationUuid");
            String boot = bootValue instanceof String ? (String) bootValue : null;
            if (boot == null || boot.trim().isEmpty() || !Objects.equals(prior.getUuid(), lastControl)
                    || !Objects.equals(boot, response.bootId) || !(response.state instanceof Map)) { return false; }
            Map<?, ?> result = response.state;
            Object proofValue = result.get("controlFenceProof");
            if (!(proofValue instanceof Map) || !"CONTROL_NOT_ISSUED".equals(result.get("phase"))) { return false; }
            Map<?, ?> proof = (Map<?, ?>) proofValue;
            return numberEquals(proof.get("schemaVersion"), 1)
                    && Objects.equals(reconcile.getHostUuid(), proof.get("hostUuid"))
                    && Objects.equals(boot, proof.get("bootId"))
                    && Objects.equals(rejectedResume.getUuid(), proof.get("rejectedOperationUuid"))
                    && Objects.equals(prior.getUuid(), proof.get("controlOperationUuid"))
                    && Objects.equals(prior.getAction(), proof.get("controlAction"))
                    && "APPLIED".equals(proof.get("controlStage"))
                    && "REJECTED".equals(proof.get("rejectedStage"))
                    && "CONTROL_OPERATION_FENCED".equals(proof.get("reasonCode"));
        } catch (RuntimeException ignored) { return false; }
    }

    private static boolean numberEquals(Object value, long expected) {
        return value instanceof Number && ((Number) value).longValue() == expected
                && ((Number) value).doubleValue() == expected;
    }

    private void aggregateParent(EntityManager em, String parentUuid) {
        if (parentUuid == null) { return; }
        MemoryTaskVO parent = em.find(MemoryTaskVO.class, parentUuid);
        List<String> statuses = em.createQuery("select t.status from MemoryTaskVO t where t.parentUuid = :parent", String.class)
                .setParameter("parent", parentUuid).getResultList();
        parent.setStatus(MemoryTaskRules.aggregateParentStatus(statuses));
    }

    private static boolean isMonitoringSample(MemoryAgentResponse response) {
        if (response == null || response.sampleTime == null || response.state == null) { return false; }
        // Lifecycle/configuration ACKs commonly contain only phase or control
        // fields.  They may update the control state, but must not advance the
        // timestamp of the last complete monitoring sample.
        // Both envelopes are emitted by Agent's read-only collector.  A single
        // quality/error/inventory field is not sufficient: sparse error ACKs
        // may carry one of those fields without metrics.
        Object savingsValue = response.state.get("savings");
        Object actualValue = response.state.get("actual");
        if (!(savingsValue instanceof Map) || !(actualValue instanceof Map)) { return false; }
        Map<?, ?> savings = (Map<?, ?>) savingsValue;
        Map<?, ?> actual = (Map<?, ?>) actualValue;
        // A complete observation has the producer schema and all metric keys.
        // Partial/Missing are legitimate read envelopes (for example an old
        // kernel without zero-page counters); null metric values remain null.
        // Error/Unknown envelopes are sparse ACKs and must not refresh a task
        // result's previously valid monitoring timestamp.
        Object quality = savings.get("quality");
        if (!"mechanism-estimate-v1".equals(savings.get("formulaVersion"))
                || !("Fresh".equals(quality) || "Partial".equals(quality) || "Missing".equals(quality))) { return false; }
        if (!savings.containsKey("sampleTime") || !(savings.get("sampleTime") == null
                || savings.get("sampleTime") instanceof Number)) { return false; }
        for (String field : Arrays.asList("ksmOrdinaryBytes", "ksmZeroBytes",
                "ksmTotalBytes", "zramBytes", "totalSavedEstimateBytes")) {
            if (!savings.containsKey(field) || !(savings.get(field) == null
                    || savings.get(field) instanceof Number)) { return false; }
        }
        if (!(actual.get("ksm") instanceof Map) || !(actual.get("zram") instanceof Map)
                || !(actual.get("writeback") instanceof Map)) { return false; }
        if ("Fresh".equals(quality)) {
            for (String field : Arrays.asList("sampleTime", "ksmOrdinaryBytes", "ksmZeroBytes",
                    "ksmTotalBytes", "zramBytes", "totalSavedEstimateBytes")) {
                Object value = savings.get(field);
                if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue())) { return false; }
            }
        }
        return true;
    }

    private void updateState(MemoryStateVO host, MemoryAgentResponse response, boolean monitoringSample) {
        boolean olderControl = (response.appliedRevision != null && host.getAppliedRevision() != null
                && response.appliedRevision < host.getAppliedRevision())
                || (response.sampleTime != null && host.getLastSampleTime() != null
                && response.sampleTime < host.getLastSampleTime());
        // The control acknowledgement and the monitoring sample have different
        // ordering/validity rules.  A reconcile ACK without sampleTime must
        // still advance appliedRevision, while it must never refresh metrics.
        if (response.appliedRevision != null && (host.getAppliedRevision() == null
                || response.appliedRevision >= host.getAppliedRevision())) {
            host.setAppliedRevision(response.appliedRevision);
        }
        boolean freshSample = monitoringSample && response.sampleTime != null
                && (host.getLastSampleTime() == null || response.sampleTime >= host.getLastSampleTime());
        Map<String, Object> previousState = new LinkedHashMap<>();
        if (host.getState() != null) {
            try { previousState.putAll(JSONObjectUtil.toObject(host.getState(), Map.class)); }
            catch (RuntimeException ignored) { /* malformed history cannot establish control state */ }
        }
        if (!monitoringSample) {
            if (olderControl) { return; }
            // A sparse lifecycle ACK can update control state (for example
            // phase=PAUSED), but it is not evidence for metrics freshness.
            if (response.state != null) {
                Map<String, Object> nextState = new LinkedHashMap<>();
                nextState.putAll(previousState);
                for (String key : CONTROL_STATE_FIELDS) {
                    if (response.state.containsKey(key)) { nextState.put(key, response.state.get(key)); }
                }
                if (response.state.containsKey("managed")) {
                    nextState.put("managed", response.state.get("managed"));
                }
                host.setState(JSONObjectUtil.toJsonString(nextState));
            }
            return;
        }
        if (!freshSample) { return; }
        // Capabilities belong to the observation envelope. Do not leave an
        // older capability result attached to a newer sample that omitted it.
        host.setCapabilities(response.capabilities == null ? null
                : JSONObjectUtil.toJsonString(response.capabilities));
        // A complete sample replaces the observation envelope. Preserve only
        // the explicit ownership fact when an older Agent omitted it; never
        // carry old metrics/statistics into a newer sample.
        Map<String, Object> nextState = new LinkedHashMap<>();
        if (response.state != null) { nextState.putAll(response.state); }
        if (!nextState.containsKey("managed") && Boolean.TRUE.equals(previousState.get("managed"))) {
            nextState.put("managed", true);
        }
        host.setState(JSONObjectUtil.toJsonString(nextState));
        host.setLastSampleTime(response.sampleTime);
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
                // PortApiValidator checks existence at the public boundary. Recheck
                // here for KVM eligibility and a resource deleted since admission.
                // Do not silently turn an invalid selection into zero coverage.
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

    public void observe(String hostUuid, MemoryAgentResponse response) {
        transaction(em -> {
            lock(em);
            // A callback from an in-flight poll may arrive after resource deletion.
            // It must neither recreate a current row nor overwrite retained evidence.
            if (em.createQuery("select h.uuid from HostVO h where h.uuid = :uuid and h.hypervisorType = 'KVM'", String.class)
                    .setParameter("uuid", hostUuid).setMaxResults(1).getResultList().isEmpty()) { return null; }
            MemoryStateVO host = em.find(MemoryStateVO.class, hostUuid);
            // Hibernate validates non-null columns during persist, not only at
            // commit. The first read-only observation precedes any policy task.
            if (host == null) {
                host = new MemoryStateVO(); host.setHostUuid(hostUuid);
                host.setStatus("Unknown"); em.persist(host);
                bumpQueryVersion(em, "state");
            }
            updateState(host, response, true);
            // Before any control operation, the first read-only observation
            // establishes the initial display status. Once a control UUID
            // exists, later samples update only observation data; they are
            // not task receipts and must not replace its terminal outcome.
            if (host.getActiveTaskUuid() == null && host.getControlOperationUuid() == null) {
                host.setStatus(response.status == null ? "Unknown" : response.status);
            }
            return null;
        });
    }

    /**
     * Persist independently sampled native KSM facts without changing the
     * full monitoring timestamp or any control/task/ownership field.
     */
    public void observePartialKsm(String hostUuid, MemoryAgentResponse response) {
        if (!MemoryObservationRules.isKsmNativePartial(response, hostUuid, System.currentTimeMillis())) { return; }
        transaction(em -> {
            lock(em);
            if (em.createQuery("select h.uuid from HostVO h where h.uuid = :uuid and h.hypervisorType = 'KVM'", String.class)
                    .setParameter("uuid", hostUuid).setMaxResults(1).getResultList().isEmpty()) { return null; }
            MemoryStateVO host = em.find(MemoryStateVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
            if (host == null) {
                host = new MemoryStateVO(); host.setHostUuid(hostUuid); host.setStatus("Unknown");
                em.persist(host);
            }
            if (host.getLastSampleTime() != null && response.sampleTime <= host.getLastSampleTime()) { return null; }

            Map<String, Object> capabilities = new LinkedHashMap<>();
            if (host.getCapabilities() != null) {
                try {
                    Map<String, Object> previous = JSONObjectUtil.toObject(host.getCapabilities(), Map.class);
                    if (previous != null) { capabilities.putAll(previous); }
                } catch (RuntimeException malformed) { return null; }
            }
            Map<String, Object> oldPartial = objectMap(capabilities.get("nativeKsmPartial"));
            Long oldPartialTime = exactLongValue(oldPartial == null ? null : oldPartial.get("sampleTime"));
            if (oldPartialTime != null && response.sampleTime <= oldPartialTime) { return null; }

            Map<String, Object> actual = objectMap(response.state.get("actual"));
            Map<String, Object> ksm = objectMap(actual == null ? null : actual.get("ksm"));
            if (ksm == null) { return null; }
            Map<String, Object> partial = new LinkedHashMap<>();
            partial.put("observationKind", response.observationKind);
            partial.put("bootId", response.bootId);
            partial.put("sampleTime", response.sampleTime);
            partial.put("ksm", ksm);
            Map<String, Object> partialCapabilities = new LinkedHashMap<>();
            for (String key : Arrays.asList("supported", "ksm", "ksmZeroPages",
                    "ksmReasonCode", "ksmZeroPagesReasonCode")) {
                if (response.capabilities.containsKey(key)) {
                    partialCapabilities.put(key, response.capabilities.get(key));
                }
            }
            partial.put("capabilities", partialCapabilities);
            boolean localReady = Boolean.TRUE.equals(response.coldKsmAdmissionReady)
                    && "LOCAL_CONTROL_FENCE_CLEAR".equals(response.coldKsmAdmissionReason);
            partial.put("coldKsmAdmissionReady", localReady);
            partial.put("coldKsmAdmissionReason", localReady ? "LOCAL_CONTROL_FENCE_CLEAR"
                    : "LOCAL_CONTROL_FENCE_UNKNOWN");
            capabilities.put("nativeKsmPartial", partial);
            host.setCapabilities(JSONObjectUtil.toJsonString(capabilities));

            Map<String, Object> state = new LinkedHashMap<>();
            if (host.getState() != null) {
                try {
                    Map<String, Object> previous = JSONObjectUtil.toObject(host.getState(), Map.class);
                    if (previous != null) { state.putAll(previous); }
                } catch (RuntimeException malformed) { return null; }
            }
            state.put("nativeKsmObservation", partial);
            host.setState(JSONObjectUtil.toJsonString(state));
            bumpQueryVersion(em, "state");
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static Long exactLongValue(Object value) {
        if (!(value instanceof Number) || value instanceof Boolean) { return null; }
        try { return new java.math.BigDecimal(value.toString()).longValueExact(); }
        catch (RuntimeException invalid) { return null; }
    }

    public void authorizePermit(String hostUuid, boolean allowed, long deadline) {
        transaction(em -> {
            lock(em);
            MemoryStateVO host = em.find(MemoryStateVO.class, hostUuid);
            if (host != null) { host.setPermitAuthorized(allowed); host.setPermitDeadline(allowed ? deadline : null); }
            return null;
        });
    }

    /** Persist the first permit issued for a task; retries never replace it. */
    public boolean recordTaskPermit(String taskUuid, String operationUuid, long deadline) {
        return transaction(em -> {
            lock(em);
            MemoryTaskVO task = em.find(MemoryTaskVO.class, taskUuid, LockModeType.PESSIMISTIC_WRITE);
            if (task == null || operationUuid == null || deadline <= System.currentTimeMillis()) { return false; }
            if (task.getIssuedPermitDeadline() != null) {
                return operationUuid.equals(task.getIssuedPermitOperationUuid())
                        && task.getIssuedPermitDeadline() == deadline;
            }
            task.setIssuedPermitOperationUuid(operationUuid);
            task.setIssuedPermitDeadline(deadline);
            em.flush();
            return true;
        });
    }

    /** Authorize only the originally issued permit after a confirmed apply. */
    public void authorizeTaskPermit(String taskUuid) {
        transaction(em -> {
            lock(em);
            MemoryTaskVO task = em.find(MemoryTaskVO.class, taskUuid, LockModeType.PESSIMISTIC_WRITE);
            if (task == null || !"Succeeded".equals(task.getStatus())) { return null; }
            MemoryStateVO state = em.find(MemoryStateVO.class, task.getHostUuid());
            long deadline = task.getIssuedPermitDeadline() == null ? 0 : task.getIssuedPermitDeadline();
            if (state != null && taskUuid.equals(state.getControlOperationUuid())
                    && task.getIssuedPermitOperationUuid() != null
                    && task.getIssuedPermitOperationUuid().equals("managed-" + task.getHostUuid())
                    && state.getActiveTaskUuid() == null && deadline > System.currentTimeMillis()) {
                state.setPermitAuthorized(true); state.setPermitDeadline(deadline);
            }
            return null;
        });
    }

    public void renewPermit(String hostUuid, String operationUuid, long deadline) {
        transaction(em -> {
            lock(em);
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid);
            long now = System.currentTimeMillis();
            if (state != null && state.isPermitAuthorized() && operationUuid != null
                    && operationUuid.equals(state.getControlOperationUuid()) && state.getActiveTaskUuid() == null
                    && state.getPermitDeadline() != null && state.getPermitDeadline() > now && deadline > now) {
                state.setPermitDeadline(Math.max(state.getPermitDeadline(), deadline));
            }
            return null;
        });
    }

    public List<MemoryTaskVO> outstanding(String owner) {
        return transaction(em -> em.createQuery("from MemoryTaskVO t where t.ownerManagementNodeUuid = :owner " +
                "and t.status = 'Draining'", MemoryTaskVO.class).setParameter("owner", owner).getResultList());
    }

    MemoryTaskVO findTask(String uuid) {
        if (uuid == null) { return null; }
        return transaction(em -> em.find(MemoryTaskVO.class, uuid));
    }

    boolean canResumeControl(String hostUuid, String controlUuid) {
        if (hostUuid == null || controlUuid == null) { return false; }
        return transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid);
            MemoryTaskVO control = em.find(MemoryTaskVO.class, controlUuid);
            return state != null && state.getActiveTaskUuid() == null && "Succeeded".equals(state.getStatus())
                    && control != null && "pause".equals(control.getAction())
                    && "Succeeded".equals(control.getStatus())
                    && Objects.equals(hostUuid, control.getHostUuid())
                    && isFreshConfirmedControl(state, controlUuid, System.currentTimeMillis());
        });
    }

    boolean canReconcileRejectedResume(String hostUuid, String controlUuid) {
        if (hostUuid == null || controlUuid == null) { return false; }
        return transaction(em -> {
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid);
            if (state == null || !Objects.equals(controlUuid, state.getControlOperationUuid())
                    || state.getActiveTaskUuid() != null) { return false; }
            MemoryTaskVO failed = em.find(MemoryTaskVO.class, controlUuid);
            if (failed == null || !"Host".equals(failed.getScope())
                    || !"resume".equals(failed.getAction()) || !"Failed".equals(failed.getStatus())
                    || !"CONTROL_OPERATION_FENCED".equals(failed.getReason())
                    || !Objects.equals(hostUuid, failed.getHostUuid())) { return false; }
            MemoryTaskVO prior = failed.getExpectedControlOperationUuid() == null ? null
                    : em.find(MemoryTaskVO.class, failed.getExpectedControlOperationUuid());
            return isPauseOrDrain(prior, hostUuid)
                    && isFreshAgentControl(state, prior.getUuid(), System.currentTimeMillis());
        });
    }

    private static MemoryTaskVO rejectedResumeRecoverySource(EntityManager em, MemoryStateVO state,
                                                               APIUpdateMemoryPolicyMsg msg, String host,
                                                               long now) {
        if (state == null || state.getActiveTaskUuid() != null
                || msg.getExpectedControlOperationUuid() == null) {
            return null;
        }
        MemoryTaskVO failed = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
        if (failed == null || !"Host".equals(failed.getScope())
                || !"resume".equals(failed.getAction()) || !"Failed".equals(failed.getStatus())
                || !"CONTROL_OPERATION_FENCED".equals(failed.getReason())
                || !Objects.equals(failed.getExpectedControlOperationUuid(), msg.getExpectedControlOperationUuid())
                || !Objects.equals(host, failed.getHostUuid())) { return null; }
        MemoryTaskVO prior = failed.getExpectedControlOperationUuid() == null ? null
                : em.find(MemoryTaskVO.class, failed.getExpectedControlOperationUuid());
        if (!isPauseOrDrain(prior, host) || !isFreshAgentControl(state, prior.getUuid(), now)) { return null; }
        return failed;
    }

    private static boolean isPauseOrDrain(MemoryTaskVO task, String hostUuid) {
        return task != null && ("pause".equals(task.getAction()) || "drain".equals(task.getAction()))
                && "Host".equals(task.getScope()) && "Succeeded".equals(task.getStatus())
                && Objects.equals(hostUuid, task.getHostUuid());
    }

    private static boolean isFreshConfirmedControl(MemoryStateVO state, String controlUuid, long now) {
        return state != null && Objects.equals(controlUuid, state.getControlOperationUuid())
                && isFreshAgentControl(state, controlUuid, now);
    }

    private static boolean isFreshAgentControl(MemoryStateVO state, String controlUuid, long now) {
        return isFreshPausedSample(state, now) && Objects.equals(controlUuid,
                JSONObjectUtil.toObject(state.getState(), Map.class).get("lastConfirmedOperationUuid"));
    }

    private static boolean isFreshPausedSample(MemoryStateVO state, long now) {
        if (state == null || state.getLastSampleTime() == null || state.getLastSampleTime() > now + 5000
                || now - state.getLastSampleTime() >= MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            return false;
        }
        try {
            Map<?, ?> root = JSONObjectUtil.toObject(state.getState(), Map.class);
            Object bootId = root.get("bootId");
            Object lifecycleValue = root.get("lifecycle");
            if (!(bootId instanceof String)
                    || ((String) bootId).trim().isEmpty() || !(lifecycleValue instanceof Map)) { return false; }
            Map<?, ?> lifecycle = (Map<?, ?>) lifecycleValue;
            return Boolean.TRUE.equals(lifecycle.get("paused"))
                    && "paused".equalsIgnoreCase(String.valueOf(lifecycle.get("activeState")));
        } catch (RuntimeException ignored) { return false; }
    }

    /** Known service ownership is durable state, not a transient KSM enabled bit. */
    public boolean hasManagedOwnership(String hostUuid) {
        return transaction(em -> {
            // Use the same global policy fence as submit(). Legacy resource
            // config writes must not race first takeover.
            lock(em);
            MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
            if (state == null) { return false; }
            try {
                if (state.getState() != null) {
                    Map<?, ?> observed = JSONObjectUtil.toObject(state.getState(), Map.class);
                    if (Boolean.TRUE.equals(observed.get("managed"))) { return true; }
                }
            } catch (RuntimeException ignored) { /* inspect task fences below */ }
            if (state.getActiveTaskUuid() != null) { return true; }
            if (state.getControlOperationUuid() != null) {
                MemoryTaskVO task = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
                return task == null || !("Succeeded".equals(task.getStatus())
                        || "Failed".equals(task.getStatus()) || "Cancelled".equals(task.getStatus()));
            }
            return false;
        });
    }

    public List<MemoryStateVO> states(List<String> hosts, int start, int limit) {
        if (start < 0) { throw error("MEMORY_INVALID_PAGE", "Page start cannot be negative"); }
        int pageSize = MemoryOptimizationGlobalConfig.pageSize(limit);
        return transaction(em -> {
            String filter = currentStateFilter(hosts);
            TypedQuery<MemoryStateVO> query = em.createQuery("from MemoryStateVO s" + filter + " order by s.hostUuid", MemoryStateVO.class);
            if (hosts != null && !hosts.isEmpty()) { query.setParameter("hosts", hosts); }
            return query.setFirstResult(start).setMaxResults(pageSize).getResultList();
        });
    }

    MemoryQueryPage<MemoryStateVO> statePage(List<String> hosts, int start, int limit, String requestedSnapshot) {
        if (start < 0) { throw error("MEMORY_INVALID_PAGE", "Page start cannot be negative"); }
        int pageSize = MemoryOptimizationGlobalConfig.pageSize(limit);
        return this.<MemoryQueryPage<MemoryStateVO>>transaction(em -> {
            lock(em);
            if (hosts != null && !hosts.isEmpty()) {
                Set<String> requested = new HashSet<>(hosts);
                List<String> existing = em.createQuery("select h.uuid from HostVO h where h.uuid in :hosts"
                                + " and h.hypervisorType = 'KVM'", String.class)
                        .setParameter("hosts", requested).getResultList();
                if (existing.size() != requested.size()) {
                    throw error("MEMORY_INVALID_TARGETS", "Current memory states require existing KVM Host UUIDs");
                }
            }
            String filter = currentStateFilter(hosts);
            TypedQuery<Long> count = em.createQuery("select count(s) from MemoryStateVO s" + filter, Long.class);
            if (hosts != null && !hosts.isEmpty()) { count.setParameter("hosts", hosts); }
            long total = count.getSingleResult();
            String snapshot = digest("state-v2|" + queryVersion(em, "state") + "|" + total + "|"
                    + (hosts == null || hosts.isEmpty() ? "*" : new TreeSet<>(hosts)));
            if (requestedSnapshot != null && !requestedSnapshot.equals(snapshot)) {
                throw error("MEMORY_QUERY_SNAPSHOT_CHANGED", "The memory state result set changed; restart pagination");
            }
            TypedQuery<MemoryStateVO> page = em.createQuery("from MemoryStateVO s" + filter + " order by s.hostUuid", MemoryStateVO.class);
            if (hosts != null && !hosts.isEmpty()) { page.setParameter("hosts", hosts); }
            List<MemoryStateVO> rows = page.setFirstResult(start).setMaxResults(pageSize).getResultList();
            Set<String> activeIds = new HashSet<>();
            for (MemoryStateVO row : rows) {
                row.setCurrentStep(null);
                if (row.getActiveTaskUuid() != null) { activeIds.add(row.getActiveTaskUuid()); }
            }
            if (!activeIds.isEmpty()) {
                // Project only this page's bound operations in the same read
                // transaction. Do not scan task history or query per Host.
                Map<String, MemoryTaskVO> active = em.createQuery("from MemoryTaskVO t where t.uuid in :ids", MemoryTaskVO.class)
                        .setParameter("ids", activeIds).getResultList().stream()
                        .collect(Collectors.toMap(MemoryTaskVO::getUuid, task -> task));
                Set<String> roots = active.values().stream()
                        .filter(task -> Arrays.asList("apply", "clearOverride").contains(task.getAction()))
                        .map(MemoryTaskVO::getParentUuid).filter(Objects::nonNull).collect(Collectors.toSet());
                Map<String, MemoryTaskVO> drains = new HashMap<>();
                if (!roots.isEmpty()) {
                    List<Object[]> contexts = em.createQuery("select d, p from MemoryTaskVO d, MemoryTaskVO p "
                            + "where d.parentUuid = p.uuid and p.uuid in :roots and d.hostUuid in :hosts "
                            + "and d.action = 'drain' and d.status = 'Succeeded'", Object[].class)
                            .setParameter("roots", roots)
                            .setParameter("hosts", rows.stream().map(MemoryStateVO::getHostUuid).collect(Collectors.toList()))
                            .getResultList();
                    for (Object[] context : contexts) {
                        MemoryTaskVO drain = (MemoryTaskVO) context[0];
                        if (isAutomaticCapacityDrain(drain, (MemoryTaskVO) context[1])) {
                            drains.put(drain.getHostUuid() + ":" + drain.getParentUuid(), drain);
                        }
                    }
                }
                for (MemoryStateVO row : rows) {
                    MemoryTaskVO task = active.get(row.getActiveTaskUuid());
                    if (task == null || !Objects.equals(row.getHostUuid(), task.getHostUuid())
                            || row.getDesiredRevision() != task.getDesiredRevision()
                            || !MemoryTaskRules.blocksHost(task.getStatus())) { continue; }
                    String step = "clearOverride".equals(task.getAction()) ? "apply" : task.getAction();
                    if (!Arrays.asList("apply", "drain", "pause", "resume", "reconcile", "recoverUncertain",
                            "prepareZramPool", "prepareWritebackBackend").contains(step)) { continue; }
                    MemoryTaskVO drain = drains.get(row.getHostUuid() + ":" + task.getParentUuid());
                    if ("apply".equals(step) && drain != null) {
                        if (Objects.equals(drain.getUuid(), task.getExpectedControlOperationUuid())) {
                            step = "activateZramPool";
                        } else {
                            try {
                                if (Boolean.TRUE.equals(MemoryPolicyRules.decode(task.getPolicy()).writeback.enabled)) {
                                    step = "restoreWriteback";
                                }
                            } catch (RuntimeException invalid) { continue; }
                        }
                    }
                    row.setCurrentStep(step);
                }
            }
            return new MemoryQueryPage<>(rows, total, snapshot);
        });
    }

    public long stateCount(List<String> hosts) {
        return transaction(em -> {
            String filter = currentStateFilter(hosts);
            TypedQuery<Long> query = em.createQuery("select count(s) from MemoryStateVO s" + filter, Long.class);
            if (hosts != null && !hosts.isEmpty()) { query.setParameter("hosts", hosts); }
            return query.getSingleResult();
        });
    }

    /**
     * Explicitly remove one terminal root task and its Host children. The compact
     * receipt survives indefinitely so an old clientRequestUuid cannot become a
     * fresh operation after the visible task history is purged.
     */
    public MemoryTaskDeleteResult deleteTaskTree(String taskUuid) {
        if (taskUuid == null || !taskUuid.matches("[0-9a-fA-F]{32}")) {
            throw error("MEMORY_INVALID_REQUEST", "task UUID must be a 32-character UUID");
        }
        return transaction(em -> {
            lock(em);
            MemoryTaskVO root = em.find(MemoryTaskVO.class, taskUuid, LockModeType.PESSIMISTIC_WRITE);
            if (root == null) {
                // A receipt distinguishes an already purged UUID from a never-known UUID,
                // while both remain successful no-op DELETEs.
                em.find(MemoryTaskIdempotencyReceiptVO.class, taskUuid);
                MemoryTaskDeleteResult missing = new MemoryTaskDeleteResult();
                missing.setTaskUuid(taskUuid); missing.setDeleted(false);
                return missing;
            }
            if (root.getParentUuid() != null) {
                throw error("MEMORY_TASK_NOT_ROOT", "Task " + taskUuid + " is a child; delete its root task instead");
            }

            List<MemoryTaskVO> children = em.createQuery(
                            "from MemoryTaskVO t where t.parentUuid = :parent order by t.uuid", MemoryTaskVO.class)
                    .setParameter("parent", taskUuid).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            if (!children.isEmpty() && !MemoryTaskRules.canDeleteHistory(root.getStatus())
                    && !"Partial".equals(root.getStatus())) {
                throw error("MEMORY_TASK_REFERENCED", "Root task " + taskUuid + " is not terminal: " + root.getStatus());
            }
            if (children.isEmpty() && !MemoryTaskRules.canDeleteHistory(root.getStatus())) {
                throw error("MEMORY_TASK_REFERENCED", "Root task " + taskUuid + " is not terminal: " + root.getStatus());
            }
            for (MemoryTaskVO child : children) {
                if (!MemoryTaskRules.canDeleteHistory(child.getStatus())) {
                    throw error("MEMORY_TASK_REFERENCED", "Child task " + child.getUuid() + " is unresolved: " + child.getStatus());
                }
            }
            if (!children.isEmpty() && !Objects.equals(root.getStatus(), MemoryTaskRules.aggregateParentStatus(
                    children.stream().map(MemoryTaskVO::getStatus).collect(Collectors.toList())))) {
                throw error("MEMORY_TASK_REFERENCED", "Root task " + taskUuid + " aggregate status is inconsistent with its children");
            }
            if (em.find(MemoryTaskIdempotencyReceiptVO.class, taskUuid) != null) {
                throw error("MEMORY_TASK_REFERENCED", "A deletion receipt already exists for task " + taskUuid);
            }
            List<String> ids = new ArrayList<>();
            ids.add(taskUuid);
            List<String> childIds = children.stream().map(MemoryTaskVO::getUuid).collect(Collectors.toList());
            ids.addAll(childIds);
            Set<String> taskHosts = children.stream().map(MemoryTaskVO::getHostUuid).filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (root.getHostUuid() != null) { taskHosts.add(root.getHostUuid()); }

            // A child itself must not have descendants; this is a bounded one-level task tree.
            Long grandchildren = childIds.isEmpty() ? 0L : em.createQuery(
                    "select count(t) from MemoryTaskVO t where t.parentUuid in :parents", Long.class)
                    .setParameter("parents", childIds).getSingleResult();
            if (grandchildren != 0) {
                throw error("MEMORY_TASK_REFERENCED", "Task tree " + taskUuid + " is not a root with a closed child set");
            }

            Long stateReferences = em.createQuery("select count(s) from MemoryStateVO s where "
                            + "s.activeTaskUuid in :ids or s.controlOperationUuid in :ids", Long.class)
                    .setParameter("ids", ids).getSingleResult();
            boolean stateJsonReference = false;
            if (!taskHosts.isEmpty()) {
                List<MemoryStateVO> hostStates = em.createQuery("from MemoryStateVO s where s.hostUuid in :hosts", MemoryStateVO.class)
                        .setParameter("hosts", taskHosts).getResultList();
                for (MemoryStateVO state : hostStates) {
                    if (containsOperationReference(state.getState(), ids)) {
                        stateJsonReference = true; break;
                    }
                }
            }
            Long taskReferences = em.createQuery("select count(t) from MemoryTaskVO t where t.uuid not in :ids and "
                            + "(t.reconcileOperationUuid in :ids or t.expectedControlOperationUuid in :ids "
                            + "or t.issuedPermitOperationUuid in :ids)", Long.class)
                    .setParameter("ids", ids).getSingleResult();
            Long migrationReferences = em.createQuery("select count(m) from MemoryMigrationVO m where m.operationUuid in :ids", Long.class)
                    .setParameter("ids", ids).getSingleResult();
            if (stateReferences != 0 || stateJsonReference || taskReferences != 0
                    || migrationReferences != 0) {
                throw error("MEMORY_TASK_REFERENCED", "Task tree " + taskUuid
                        + " is still referenced by Host state, another task, or migration");
            }

            if (root.getRequestKey() != null) {
                List<MemoryTaskIdempotencyReceiptVO> duplicateReceipts = em.createQuery(
                                "from MemoryTaskIdempotencyReceiptVO r where r.requestKey = :key", MemoryTaskIdempotencyReceiptVO.class)
                        .setParameter("key", root.getRequestKey()).getResultList();
                if (!duplicateReceipts.isEmpty()) {
                    throw error("MEMORY_TASK_REFERENCED", "Idempotency receipt already exists for task tree " + taskUuid);
                }
            }

            Long pendingNotifications = em.createQuery("select count(o) from MemoryTaskFailureOutboxVO o "
                            + "where o.taskUuid in :ids and o.delivered = false", Long.class)
                    .setParameter("ids", ids).getSingleResult();
            if (pendingNotifications != 0) {
                throw error("MEMORY_TASK_REFERENCED", "Task tree " + taskUuid + " has an undelivered failure notification");
            }

            MemoryTaskIdempotencyReceiptVO receipt = new MemoryTaskIdempotencyReceiptVO();
            receipt.setTaskUuid(taskUuid);
            receipt.setRequestKey(root.getRequestKey());
            receipt.setRequestHash(root.getRequestHash());
            receipt.setOriginalStatus(root.getStatus());
            receipt.setDeletedDate(new Timestamp(System.currentTimeMillis()));
            em.persist(receipt);
            MemoryTaskDeleteResult deleted = new MemoryTaskDeleteResult();
            deleted.setTaskUuid(taskUuid); deleted.setDeleted(true);
            deleted.setScope(root.getScope()); deleted.setResourceUuid(root.getResourceUuid());
            List<String> hostUuids = new ArrayList<>();
            if (root.getHostUuid() != null) { hostUuids.add(root.getHostUuid()); }
            for (MemoryTaskVO child : children) {
                if (child.getHostUuid() != null && !hostUuids.contains(child.getHostUuid())) { hostUuids.add(child.getHostUuid()); }
            }
            deleted.setHostUuids(hostUuids);
            // Delivered outbox rows are queue bookkeeping, not the durable audit record;
            // the platform alarm/event remains the audit. Never remove undelivered rows.
            em.createQuery("delete from MemoryTaskFailureOutboxVO o where o.taskUuid in :ids and o.delivered = true")
                    .setParameter("ids", ids).executeUpdate();
            bumpQueryVersion(em, "task");
            for (MemoryTaskVO child : children) { em.remove(child); }
            em.remove(root);
            em.flush();
            return deleted;
        });
    }

    private static boolean containsOperationReference(String state, Collection<String> taskUuids) {
        if (state == null || state.trim().isEmpty()) { return false; }
        final JsonElement parsed;
        try { parsed = new JsonParser().parse(state); }
        catch (RuntimeException malformed) {
            // We cannot establish the absence of a control-operation reference in malformed state.
            return true;
        }
        return containsOperationReference(parsed, taskUuids);
    }

    private static boolean containsOperationReference(JsonElement value, Collection<String> taskUuids) {
        if (value == null || value.isJsonNull()) { return false; }
        if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                if (entry.getKey().toLowerCase(Locale.ROOT).contains("operationuuid")
                        && entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()
                        && taskUuids.contains(entry.getValue().getAsString())) { return true; }
                if (containsOperationReference(entry.getValue(), taskUuids)) { return true; }
            }
        } else if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) {
                if (containsOperationReference(child, taskUuids)) { return true; }
            }
        }
        return false;
    }

    private static String currentStateFilter(List<String> hosts) {
        return " where exists (select h.uuid from HostVO h where h.uuid = s.hostUuid and h.hypervisorType = 'KVM')"
                + (hosts == null || hosts.isEmpty() ? "" : " and s.hostUuid in :hosts");
    }

    /**
     * Called by the platform cascade CLEANUP phase, after ResourceVO deletion.
     * Only current data is cleaned. Task/outbox history and unresolved recovery
     * evidence are not history-retention candidates here. Destroyed but still
     * recoverable VMs retain policy until their actual database deletion.
     */
    public void cleanupDeletedResources() {
        transaction(em -> {
            lock(em);
            // A deleted Host with retained Unknown evidence leaves its state
            // row intact, but has still changed the current query membership.
            bumpQueryVersion(em, "state");
            String unfinished = "t.status not in ('Succeeded', 'Failed', 'Cancelled')";
            String orphanVm = "not exists (select v.uuid from VmInstanceVO v where v.uuid = e.vmUuid)"
                    + " and not exists (select t.uuid from MemoryTaskVO t where t.scope = 'VM' and t.resourceUuid = e.vmUuid and " + unfinished + ")"
                    + " and not exists (select m.vmUuid from MemoryMigrationVO m where m.vmUuid = e.vmUuid and m.status <> 'Released')";
            em.createQuery("delete from MemoryVmExclusionVO e where " + orphanVm).executeUpdate();
            em.createQuery("delete from MemoryMigrationVO m where m.status = 'Released'"
                    + " and not exists (select v.uuid from VmInstanceVO v where v.uuid = m.vmUuid)"
                    + " and not exists (select t.uuid from MemoryTaskVO t where t.scope = 'VM' and t.resourceUuid = m.vmUuid and " + unfinished + ")").executeUpdate();
            em.createQuery("delete from MemoryPolicyVO p where ((p.scope = 'Host' and not exists (select h.uuid from HostVO h where h.uuid = p.resourceUuid))"
                    + " or (p.scope = 'Cluster' and not exists (select c.uuid from ClusterVO c where c.uuid = p.resourceUuid))"
                    + " or (p.scope = 'VM' and not exists (select v.uuid from VmInstanceVO v where v.uuid = p.resourceUuid)))"
                    + " and not exists (select t.uuid from MemoryTaskVO t where ((t.scope = p.scope and t.resourceUuid = p.resourceUuid)"
                    + " or (p.scope = 'Host' and t.hostUuid = p.resourceUuid)) and " + unfinished + ")"
                    + " and not exists (select s.hostUuid from MemoryStateVO s where p.scope = 'Host' and s.hostUuid = p.resourceUuid"
                    + " and (s.activeTaskUuid is not null or s.status not in ('Succeeded', 'Failed', 'Cancelled')))"
                    + " and not exists (select m.vmUuid from MemoryMigrationVO m where m.status <> 'Released' and"
                    + " ((p.scope = 'VM' and m.vmUuid = p.resourceUuid) or (p.scope = 'Host' and (m.sourceHostUuid = p.resourceUuid or m.targetHostUuid = p.resourceUuid))))").executeUpdate();
            em.createQuery("delete from MemoryStateVO s where not exists (select h.uuid from HostVO h where h.uuid = s.hostUuid)"
                    + " and s.activeTaskUuid is null and s.status in ('Succeeded', 'Failed', 'Cancelled')"
                    + " and not exists (select t.uuid from MemoryTaskVO t where t.hostUuid = s.hostUuid and " + unfinished + ")"
                    + " and not exists (select m.vmUuid from MemoryMigrationVO m where m.status <> 'Released'"
                    + " and (m.sourceHostUuid = s.hostUuid or m.targetHostUuid = s.hostUuid))").executeUpdate();
            return null;
        });
    }

    public List<MemoryStateVO> allStates(List<String> hosts) {
        List<MemoryStateVO> result = new ArrayList<>();
        int size = MemoryOptimizationGlobalConfig.pageSize(null);
        for (int start = 0; ; start = Math.addExact(start, size)) {
            List<MemoryStateVO> page = states(hosts, start, size); result.addAll(page);
            if (page.size() < size) { return result; }
        }
    }

    public long taskCount(APIGetMemoryTasksMsg msg) {
        return transaction(em -> {
            String query = "select count(t) from MemoryTaskVO t where 1=1";
            if (msg.getUuid() != null) { query += " and (t.uuid = :uuid or t.parentUuid = :uuid)"; }
            if (msg.getHostUuid() != null) { query += " and t.hostUuid = :host"; }
            if (msg.getStatus() != null) { query += " and t.status = :status"; }
            TypedQuery<Long> q = em.createQuery(query, Long.class);
            if (msg.getUuid() != null) { q.setParameter("uuid", msg.getUuid()); }
            if (msg.getHostUuid() != null) { q.setParameter("host", msg.getHostUuid()); }
            if (msg.getStatus() != null) { q.setParameter("status", msg.getStatus()); }
            return q.getSingleResult();
        });
    }

    public List<MemoryTaskInventory> tasks(APIGetMemoryTasksMsg msg) {
        if (msg.getStart() < 0) { throw error("MEMORY_INVALID_PAGE", "Page start cannot be negative"); }
        return transaction(em -> {
            String query = "from MemoryTaskVO t where 1=1";
            if (msg.getUuid() != null) { query += " and (t.uuid = :uuid or t.parentUuid = :uuid)"; }
            if (msg.getHostUuid() != null) { query += " and t.hostUuid = :host"; }
            if (msg.getStatus() != null) { query += " and t.status = :status"; }
            TypedQuery<MemoryTaskVO> q = em.createQuery(query + " order by t.createDate desc, t.uuid", MemoryTaskVO.class);
            if (msg.getUuid() != null) { q.setParameter("uuid", msg.getUuid()); }
            if (msg.getHostUuid() != null) { q.setParameter("host", msg.getHostUuid()); }
            if (msg.getStatus() != null) { q.setParameter("status", msg.getStatus()); }
            return q.setFirstResult(msg.getStart()).setMaxResults(msg.getLimit())
                    .getResultList().stream().map(MemoryTaskVO::toInventory).collect(Collectors.toList());
        });
    }

    MemoryQueryPage<MemoryTaskInventory> taskPage(APIGetMemoryTasksMsg msg) {
        if (msg.getStart() < 0) { throw error("MEMORY_INVALID_PAGE", "Page start cannot be negative"); }
        return this.<MemoryQueryPage<MemoryTaskInventory>>transaction(em -> {
            lock(em);
            String query = "from MemoryTaskVO t where 1=1";
            if (msg.getUuid() != null) { query += " and (t.uuid = :uuid or t.parentUuid = :uuid)"; }
            if (msg.getHostUuid() != null) { query += " and t.hostUuid = :host"; }
            if (msg.getStatus() != null) { query += " and t.status = :status"; }
            TypedQuery<Long> count = em.createQuery("select count(t) " + query, Long.class);
            bindTaskFilter(count, msg);
            long total = count.getSingleResult();
            String snapshot = digest("task-v2|" + queryVersion(em, "task") + "|" + total + "|"
                    + msg.getUuid() + "|" + msg.getHostUuid() + "|" + msg.getStatus());
            if (msg.getSnapshotId() != null && !msg.getSnapshotId().equals(snapshot)) {
                throw error("MEMORY_QUERY_SNAPSHOT_CHANGED", "The memory task result set changed; restart pagination");
            }
            TypedQuery<MemoryTaskVO> page = em.createQuery(query + " order by t.createDate desc, t.uuid", MemoryTaskVO.class);
            bindTaskFilter(page, msg);
            List<MemoryTaskInventory> items = page.setFirstResult(msg.getStart()).setMaxResults(msg.getLimit())
                    .getResultList().stream().map(MemoryTaskVO::toInventory).collect(Collectors.toList());
            return new MemoryQueryPage<>(items, total, snapshot);
        });
    }

    private static void bindTaskFilter(TypedQuery<?> query, APIGetMemoryTasksMsg msg) {
        if (msg.getUuid() != null) { query.setParameter("uuid", msg.getUuid()); }
        if (msg.getHostUuid() != null) { query.setParameter("host", msg.getHostUuid()); }
        if (msg.getStatus() != null) { query.setParameter("status", msg.getStatus()); }
    }

    public MemoryTaskInventory cancel(String uuid) {
        return transaction(em -> {
            lock(em);
            MemoryTaskVO task = em.find(MemoryTaskVO.class, uuid);
            if (task == null || !MemoryTaskRules.canCancel(task.getStatus())) {
                throw error("MEMORY_TASK_NOT_CANCELLABLE", "Only Queued tasks can be cancelled");
            }
            bumpQueryVersion(em, "task");
            List<MemoryTaskVO> children = task.getHostUuid() == null
                    ? em.createQuery("from MemoryTaskVO t where t.parentUuid = :parent", MemoryTaskVO.class)
                    .setParameter("parent", uuid).getResultList() : Collections.singletonList(task);
            for (MemoryTaskVO child : children) {
                if (!MemoryTaskRules.canCancel(child.getStatus())) { throw error("MEMORY_TASK_NOT_CANCELLABLE", "A child already started"); }
                child.setStatus("Cancelled");
                MemoryStateVO state = em.find(MemoryStateVO.class, child.getHostUuid());
                if (state != null && child.getUuid().equals(state.getActiveTaskUuid())) {
                    MemoryTaskVO recovery = uncertainRecoveryForResult(em, child);
                    if (recovery != null) {
                        String restore = "reconcile".equals(child.getAction()) ? recovery.getUuid()
                                : recovery.getExpectedControlOperationUuid();
                        MemoryTaskVO previous = em.find(MemoryTaskVO.class, restore);
                        if (previous == null || !"Unknown".equals(previous.getStatus())
                                || !Objects.equals(previous.getHostUuid(), child.getHostUuid())
                                || !Objects.equals(recovery.getExpectedControlOperationUuid(), state.getControlOperationUuid())) {
                            throw error("MEMORY_CONTROL_OPERATION_FENCED", "Cannot verify the original unresolved recovery owner");
                        }
                        state.setActiveTaskUuid(previous.getUuid()); state.setStatus(previous.getStatus());
                        state.setReason(previous.getReason());
                    } else { state.setActiveTaskUuid(null); }
                }
            }
            task.setStatus("Cancelled");
            em.flush(); aggregateParent(em, task.getParentUuid());
            return task.toInventory();
        });
    }

    public void expireUnconfirmed(String owner) {
        transaction(em -> {
            lock(em);
            List<MemoryTaskVO> tasks = em.createQuery("from MemoryTaskVO t where t.ownerManagementNodeUuid = :owner " +
                    "and t.hostUuid is not null and t.status in ('Applying', 'Draining')", MemoryTaskVO.class)
                    .setParameter("owner", owner).getResultList();
            if (!tasks.isEmpty()) { bumpQueryVersion(em, "task"); }
            for (MemoryTaskVO task : tasks) {
                task.setStatus("Unknown"); task.setReason("Management node restarted; explicit reconcile required");
                MemoryStateVO state = em.find(MemoryStateVO.class, task.getHostUuid());
                if (state != null) { state.setStatus("Unknown"); }
                aggregateParent(em, task.getParentUuid());
            }
            return null;
        });
    }

    private static String digest(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) { result.append(String.format("%02x", value & 255)); }
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String trim(String value) { return value == null ? null : value.substring(0, Math.min(2048, value.length())); }
    private static MemoryOperationException error(String code, String detail) { return new MemoryOperationException(code, detail); }
}
