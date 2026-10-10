package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigCanonicalEvents;
import org.zstack.core.config.ConfigMutation;
import org.zstack.core.config.ConfigMutationContext;
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
    private static final long TARGET_SHARD_TTL_MS = 30 * 60 * 1000L;
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
        String blocked = transaction(em -> {
            em.createNativeQuery("INSERT IGNORE INTO MemoryPolicyVO " +
                    "(uuid, scope, resourceUuid, revision, policy, createDate, lastOpDate) " +
                    "VALUES ('Global:global', 'Global', 'global', 0, '{\"schemaVersion\":1}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .executeUpdate();
            lock(em);
            em.createNativeQuery("INSERT IGNORE INTO MemoryQueryVersionVO (name, revision) VALUES ('state', 0), ('task', 0)")
                    .executeUpdate();
            for (MemoryPolicyVO row : em.createQuery("from MemoryPolicyVO order by uuid", MemoryPolicyVO.class).getResultList()) {
                String migrated = MemoryPolicyRules.migrateLegacy(row.getPolicy(), row.getScope());
                if (!migrated.equals(row.getPolicy())) {
                    if (row.getLegacyPolicy() == null) { row.setLegacyPolicy(row.getPolicy()); }
                    row.setPolicy(migrated);
                }
            }
            return migrateOrdinaryFields(em);
        });
        if (blocked != null) {
            throw error("MEMORY_NOT_READY", "Standard memory policy migration is blocked: " + blocked);
        }
    }

    private static final class StandardMigrationValue {
        MemoryPolicyVO policyRow;
        MemoryStandardField field;
        Object value;
    }

    private String migrateOrdinaryFields(EntityManager em) {
        MemoryStandardConfigMigrationVO marker = em.find(MemoryStandardConfigMigrationVO.class,
                MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1, LockModeType.PESSIMISTIC_WRITE);
        if (marker != null && "DONE".equals(marker.getStatus())) { return null; }
        if (marker == null) {
            marker = new MemoryStandardConfigMigrationVO();
            marker.setUuid(MemoryStandardConfigMigrationVO.ORDINARY_FIELDS_V1);
            marker.setStatus("RUNNING"); em.persist(marker);
        } else {
            // BLOCKED is durable evidence, not a permanent latch. On every
            // startup retry the full preflight; remediation can then complete
            // the same migration without manual marker surgery.
            marker.setStatus("RUNNING"); marker.setReason(null);
        }

        List<StandardMigrationValue> planned = new ArrayList<>();
        Map<MemoryPolicyVO, JsonObject> strippedPolicy = new LinkedHashMap<>();
        String conflict = null;
        for (MemoryPolicyVO row : em.createQuery("from MemoryPolicyVO p where p.scope in ('Global','Cluster','Host') order by p.uuid", MemoryPolicyVO.class).getResultList()) {
            JsonObject original;
            try {
                JsonElement parsed = new JsonParser().parse(row.getPolicy());
                if (parsed == null || !parsed.isJsonObject()) { throw new IllegalArgumentException("not a JSON object"); }
                original = parsed.getAsJsonObject();
            } catch (RuntimeException malformed) {
                conflict = "policy row " + row.getUuid() + " is unreadable"; break;
            }
            JsonObject remaining = original.deepCopy();
            boolean hasStandard = false;
            for (MemoryStandardField field : MemoryStandardField.values()) {
                String[] path = field.path().split("\\.", -1);
                JsonElement section = original.get(path[0]);
                if (section == null || !section.isJsonObject() || !section.getAsJsonObject().has(path[1])) { continue; }
                MemoryStandardField.Scope sourceScope = scopeOf(row.getScope());
                if (sourceScope == null || !field.allows(sourceScope)) {
                    conflict = "legacy field " + field.path() + " is not supported at " + row.getScope() + " scope";
                    break;
                }
                JsonElement rawValue = section.getAsJsonObject().get(path[1]);
                if (rawValue == null || rawValue.isJsonNull()) {
                    conflict = "null legacy value " + field.path() + " at " + row.getUuid(); break;
                }
                Object value;
                try { value = MemoryStandardConfigCodec.get(original.toString(), field.path()); }
                catch (RuntimeException invalid) { conflict = "invalid legacy value " + field.path() + " at " + row.getUuid(); break; }
                if (value == null) { conflict = "unset legacy value " + field.path() + " at " + row.getUuid(); break; }
                if ("Global".equals(row.getScope())) {
                    String stored = standardConfigs.globalStoredValue(em, field.path());
                    if (stored == null) { conflict = "GlobalConfig row is missing for " + field.path(); break; }
                    if (!standardConfigs.globalIsSchemaDefault(em, field.path())) {
                        Object existing;
                        try { existing = standardConfigs.readGlobalOwn(em, field.path()); }
                        catch (RuntimeException invalid) { conflict = "invalid standard value for " + field.path() + " at " + row.getUuid(); break; }
                        if (!Objects.equals(existing, value)) { conflict = "conflicting standard and legacy values for " + field.path() + " at " + row.getUuid(); break; }
                    }
                } else {
                    if (standardConfigs.hasOverrideRow(em, field.path(), row.getResourceUuid())) {
                        Object existing;
                        try { existing = standardConfigs.readOverrideOwn(em, field.path(), row.getResourceUuid()); }
                        catch (RuntimeException malformed) { conflict = "invalid standard value for " + field.path() + " at " + row.getUuid(); break; }
                        if (!Objects.equals(existing, value)) {
                            conflict = "conflicting standard and legacy values for " + field.path() + " at " + row.getUuid(); break;
                        }
                    }
                }
                StandardMigrationValue item = new StandardMigrationValue();
                item.policyRow = row; item.field = field; item.value = value; planned.add(item); hasStandard = true;
                remaining.getAsJsonObject(path[0]).remove(path[1]);
            }
            if (conflict != null) { break; }
            if (hasStandard) {
                List<String> emptySections = new ArrayList<>();
                for (Map.Entry<String, JsonElement> section : remaining.entrySet()) {
                    if (section.getValue().isJsonObject() && section.getValue().getAsJsonObject().size() == 0) {
                        emptySections.add(section.getKey());
                    }
                }
                emptySections.forEach(remaining::remove);
                strippedPolicy.put(row, remaining);
            }
        }
        if (conflict != null) {
            marker.setStatus("BLOCKED"); marker.setReason(conflict.substring(0, Math.min(2048, conflict.length())));
            return marker.getReason();
        }

        List<MemoryStandardConfigAdapter.ResourceChange> resourceEvents = new ArrayList<>();
        LinkedHashSet<GlobalConfig> globalRefresh = new LinkedHashSet<>();
        for (StandardMigrationValue item : planned) {
            MemoryPolicyVO row = item.policyRow;
            MemoryStandardConfigAdapter.ChangeSet changes;
            if ("Global".equals(row.getScope())) {
                changes = standardConfigs.writeGlobal(em, item.field.path(), item.value);
            } else {
                String resourceType = "Host".equals(row.getScope()) ? HostVO.class.getSimpleName() : org.zstack.header.cluster.ClusterVO.class.getSimpleName();
                changes = standardConfigs.writeOverride(em, row.getResourceUuid(), resourceType, item.field.path(), item.value);
            }
            globalRefresh.addAll(changes.refreshAfterCommit()); resourceEvents.addAll(changes.resourceChanges());
        }
        for (Map.Entry<MemoryPolicyVO, JsonObject> entry : strippedPolicy.entrySet()) {
            MemoryPolicyVO row = entry.getKey();
            if (row.getLegacyPolicy() == null) { row.setLegacyPolicy(row.getPolicy()); }
            row.setPolicy(entry.getValue().toString());
        }
        marker.setStatus("DONE"); marker.setReason(null);
        registerStandardConfigRefresh(globalRefresh, resourceEvents);
        return null;
    }

    private static MemoryStandardField.Scope scopeOf(String scope) {
        if ("Global".equals(scope)) { return MemoryStandardField.Scope.GLOBAL; }
        if ("Cluster".equals(scope)) { return MemoryStandardField.Scope.CLUSTER; }
        if ("Host".equals(scope)) { return MemoryStandardField.Scope.HOST; }
        return null;
    }

    private void registerStandardConfigRefresh(Set<GlobalConfig> globals,
            List<MemoryStandardConfigAdapter.ResourceChange> resources) {
        if ((globals == null || globals.isEmpty()) && (resources == null || resources.isEmpty())) { return; }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        if (globals != null) {
                            for (GlobalConfig config : globals) {
                                try { org.zstack.core.db.AfterCommitTransactionExecutor.run(transactionManager,
                                        config::reloadAndPublishCanonicalUpdateAfterCommit); }
                                catch (Throwable failure) {
                                    logger.warn(String.format("committed memory policy update could not refresh global config %s.%s",
                                            config.getCategory(), config.getName()), failure);
                                }
                            }
                        }
                        if (resources != null) {
                            for (MemoryStandardConfigAdapter.ResourceChange change : resources) {
                                String category = change.config().getCategory();
                                String name = change.config().getName();
                                String path;
                                Object event;
                                if (change.deleted()) {
                                    path = s(ResourceConfigCanonicalEvents.DELETE_EVENT_PATH).formatByMap(map(
                                            e("nodeUuid", Platform.getManagementServerId()), e("category", category), e("name", name)));
                                    ResourceConfigCanonicalEvents.DeleteEvent deleted = new ResourceConfigCanonicalEvents.DeleteEvent();
                                    deleted.setOldValue(change.oldValue()); deleted.setResourceUuid(change.resourceUuid());
                                    deleted.setResourceType(change.resourceType()); event = deleted;
                                } else {
                                    path = s(ResourceConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                                            e("nodeUuid", Platform.getManagementServerId()), e("category", category), e("name", name)));
                                    ResourceConfigCanonicalEvents.UpdateEvent updated = new ResourceConfigCanonicalEvents.UpdateEvent();
                                    updated.setOldValue(change.oldValue()); updated.setResourceUuid(change.resourceUuid());
                                    updated.setResourceType(change.resourceType()); event = updated;
                                }
                                try { org.zstack.core.db.AfterCommitTransactionExecutor.run(transactionManager,
                                        () -> evtf.fire(path, event)); }
                                catch (Throwable failure) {
                                    logger.warn(String.format("committed memory policy update could not publish resource config event %s.%s for %s",
                                            category, name, change.resourceUuid()), failure);
                                }
                            }
                        }
                    }
                });
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

    /** Compatibility entry point for tests and non-ResourceConfig callers. */
    public void recordLegacyKsmResourceMutation(String resourceUuid) {
        transaction(em -> { recordLegacyKsmResourceMutation(em, resourceUuid); return null; });
    }

    /** Same-EntityManager ownership fence called from ResourceConfig's write path. */
    public void recordLegacyKsmResourceMutation(EntityManager em, String resourceUuid) {
        lock(em);
        String resourceType = em.createQuery("select r.resourceType from ResourceVO r where r.uuid = :uuid", String.class)
                .setParameter("uuid", resourceUuid).getResultList().stream().findFirst().orElse(null);
        if ("HostVO".equals(resourceType) || "Host".equals(resourceType)) {
            assertNoManagedOwnership(em, resourceUuid);
            bumpScopeRevision(em, "Host", resourceUuid);
        } else if ("ClusterVO".equals(resourceType) || "Cluster".equals(resourceType)) {
            List<String> hosts = em.createQuery("select h.uuid from HostVO h where h.clusterUuid = :cluster " +
                            "and h.hypervisorType = :type order by h.uuid", String.class)
                    .setParameter("cluster", resourceUuid).setParameter("type", "KVM").getResultList();
            for (String host : hosts) { assertNoManagedOwnership(em, host); }
            bumpScopeRevision(em, "Cluster", resourceUuid);
        } else {
            throw error("MEMORY_INVALID_SCOPE", "Legacy host.ksm supports only Host and Cluster resource configs");
        }
    }

    /**
     * Fence legacy GlobalConfig HOST_KSM updates before the GlobalConfig row is
     * changed. This is called only from its opted-in atomic local update hook.
     * A row already containing newValue is a remote canonical replay, not a
     * second local mutation.
     */
    public void recordLegacyKsmGlobalMutation(String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) { return; }
        transaction(em -> {
            List<String> persisted = em.createQuery("select g.value from GlobalConfigVO g where g.category = :category and g.name = :name", String.class)
                    .setParameter("category", org.zstack.kvm.KVMGlobalConfig.CATEGORY)
                    .setParameter("name", org.zstack.kvm.KVMGlobalConfig.HOST_KSM.getName()).getResultList();
            if (!persisted.isEmpty() && Objects.equals(persisted.get(0), newValue)) { return null; }
            lock(em);
            List<String> hosts = em.createQuery("select h.uuid from HostVO h where h.hypervisorType = :type order by h.uuid", String.class)
                    .setParameter("type", "KVM").getResultList();
            for (String host : hosts) { assertNoManagedOwnership(em, host); }
            bumpScopeRevision(em, "Global", "global");
            return null;
        });
    }

    private static final class StandardMutation {
        String scope;
        String resource;
        final Map<String, Object> values = new LinkedHashMap<>();
        final List<Object> identity = new ArrayList<>();
    }

    private StandardMutation standardMutation(EntityManager em, List<ConfigMutation> changes) {
        StandardMutation result = new StandardMutation();
        for (ConfigMutation change : changes) {
            MemoryStandardField field = MemoryStandardField.forConfig(change.getCategory(), change.getName());
            if (field == null) { continue; }
            String resource = change.getResourceUuid() == null ? "global" : change.getResourceUuid();
            String scope = "Global";
            if (change.getResourceUuid() != null) {
                ResourceVO row = em.find(ResourceVO.class, change.getResourceUuid());
                if (row == null || !Objects.equals(row.getResourceType(), change.getResourceType())) {
                    throw error("MEMORY_INVALID_SCOPE", "The configuration resource no longer exists or changed type");
                }
                if (HostVO.class.getSimpleName().equals(row.getResourceType())) { scope = "Host"; }
                else if (org.zstack.header.cluster.ClusterVO.class.getSimpleName().equals(row.getResourceType())) { scope = "Cluster"; }
                else { throw error("MEMORY_INVALID_SCOPE", "Memory configuration requires a KVM Host or Cluster"); }
            }
            if (!field.allows(scopeOf(scope))) {
                throw error("MEMORY_INVALID_SCOPE", field.path() + " does not support " + scope);
            }
            if (result.scope != null && (!result.scope.equals(scope) || !result.resource.equals(resource))) {
                throw error("MEMORY_INVALID_REQUEST", "A standard configuration transaction must address one resource");
            }
            if (result.values.containsKey(field.path())) {
                throw error("MEMORY_INVALID_REQUEST", "Duplicate configuration field " + field.path());
            }
            result.scope = scope; result.resource = resource;
            Object value = change.isDelete() ? null : storedMutationValue(field, change.getNewValue(), scope);
            if ("Global".equals(scope) && value == null) {
                value = storedMutationValue(field, field.config().getDefaultValue(), scope);
            }
            result.values.put(field.path(), value);
            result.identity.add(Arrays.asList(field.path(), change.isDelete(), change.getNewValue()));
        }
        return result;
    }

    private Object storedMutationValue(MemoryStandardField field, String value, String scope) {
        if (field == MemoryStandardField.KSM_ENABLED && value != null
                && "none".equalsIgnoreCase(value.trim())) {
            return MemoryStandardConfigAdapter.OverrideMode.UNMANAGED;
        }
        if (!"Global".equals(scope) && value != null && value.trim().isEmpty()) {
            throw error("MEMORY_INVALID_POLICY", "Stored memory configuration is empty; refusing to inherit");
        }
        return MemoryStandardConfigCodec.decode(field.path(), value);
    }

    /** A standard write/delete consumes the compatibility value so deletion cannot resurrect it. */
    private String scrubCompatibilityFields(EntityManager em, String scope, String resource,
            List<ConfigMutation> changes) {
        MemoryPolicyVO row = em.find(MemoryPolicyVO.class, scope + ":" + resource, LockModeType.PESSIMISTIC_WRITE);
        String compatibility = row == null ? policy(em, scope, resource).getPolicy() : row.getPolicy();
        for (ConfigMutation change : changes) {
            MemoryStandardField field = MemoryStandardField.forConfig(change.getCategory(), change.getName());
            if (field != null) { compatibility = MemoryStandardConfigCodec.put(compatibility, field.path(), null); }
        }
        return compatibility;
    }

    /** Actual-commit hook only: all scalar validators/preview remain free of DB writes. */
    public void prepareStandardMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
        StandardMutation mutation = standardMutation(em, changes);
        if (mutation.scope == null) { return; }
        lock(em);
        validateResource(em, mutation.scope, mutation.resource);
        if (context == null || context.getRequestId() == null || context.getActor() == null) {
            throw error("MEMORY_INVALID_REQUEST", "Configuration mutation identity is required");
        }
        String key = standardRequestKey(context);
        if (!em.createQuery("select t.uuid from MemoryTaskVO t where t.requestKey = :key", String.class)
                .setParameter("key", key).setMaxResults(1).getResultList().isEmpty()
                || !em.createQuery("select r.taskUuid from MemoryTaskIdempotencyReceiptVO r where r.requestKey = :key", String.class)
                .setParameter("key", key).setMaxResults(1).getResultList().isEmpty()) {
            // A replay must not overwrite a later configuration, even if the old
            // operation was already compacted to an idempotency receipt.
            throw error("MEMORY_IDEMPOTENCY_CONFLICT", "This configuration request was already accepted; query its task instead of replaying it");
        }
        try {
            Set<String> replacedFields = new HashSet<>(mutation.values.keySet());
            String candidate = ownPolicy(em, mutation.scope, mutation.resource, replacedFields);
            boolean explicitUnmanagedKsm = false;
            boolean changedKsm = false;
            for (Map.Entry<String, Object> value : mutation.values.entrySet()) {
                if ("ksm.enabled".equals(value.getKey())) {
                    changedKsm = true;
                    if (MemoryStandardConfigAdapter.isUnmanaged(value.getValue())) {
                        explicitUnmanagedKsm = true;
                        candidate = MemoryStandardConfigCodec.put(candidate, value.getKey(), null);
                    } else {
                        candidate = MemoryStandardPolicy.set(candidate, value.getKey(), value.getValue());
                    }
                } else {
                    candidate = MemoryStandardPolicy.set(candidate, value.getKey(), value.getValue());
                }
            }
            validateConfigurationCandidate(em, mutation.scope, mutation.resource, candidate,
                    changedKsm, explicitUnmanagedKsm);
        } catch (IllegalArgumentException invalid) { throw translatePolicyRuleFailure(invalid); }
    }

    /** Shared by standard writes and compatibility policy writes, before either persists values. */
    private void validateConfigurationCandidate(EntityManager em, String scope, String resource, String candidate) {
        validateConfigurationCandidate(em, scope, resource, candidate, false, false);
    }

    private void validateConfigurationCandidate(EntityManager em, String scope, String resource, String candidate,
            boolean ignoreStoredKsmOverride, boolean forceUnmanagedKsm) {
        String effectiveCandidate = mergeEffectiveLayer(em, parentEffective(em, scope, resource), scope, resource,
                candidate, ignoreStoredKsmOverride, forceUnmanagedKsm);
        MemoryPolicyConfig decoded = MemoryPolicyRules.decode(effectiveCandidate);
        if ("Global".equals(scope) && decoded.writeback != null && Boolean.TRUE.equals(decoded.writeback.enabled)) {
            throw error("MEMORY_INVALID_POLICY", "Writeback backends must be configured separately on each Host");
        }
        // A parent's final candidate must not invalidate surviving child overrides,
        // even when a compatibility client selected only a subset for immediate apply.
        for (String host : targets(em, scope, resource)) {
            effectiveHostWithCandidate(em, host, scope, resource, candidate,
                    ignoreStoredKsmOverride, forceUnmanagedKsm);
        }
    }

    private static String standardRequestKey(ConfigMutationContext context) {
        return context.getActor() + ":standard-config:" + context.getRequestId();
    }

    /** Same EntityManager/transaction as the standard rows; no network dispatch here. */
    public void completeStandardMutations(EntityManager em, List<ConfigMutation> changes, ConfigMutationContext context) {
        StandardMutation mutation = standardMutation(em, changes);
        if (mutation.scope == null) { return; }
        String compatibility = scrubCompatibilityFields(em, mutation.scope, mutation.resource, changes);
        commitConfiguration(em, mutation.scope, mutation.resource, compatibility,
                targets(em, mutation.scope, mutation.resource), context.getActor(),
                standardRequestKey(context), digest(MemoryPolicyRules.canonicalRequest(Arrays.asList(
                        mutation.scope, mutation.resource, mutation.identity))), null, "apply", false);
    }

    /**
     * The common configuration commit for both public entry families. The caller
     * owns the same transaction as the already-written standard rows. Ordinary
     * APIs persist desired intent for unavailable Hosts; the legacy CAS/preview
     * entry retains its stricter pre-admission and full-payload behavior.
     */
    private MemoryTaskInventory commitConfiguration(EntityManager em, String scope, String resource,
            String specializedPolicy, List<String> hosts, String actor, String requestKey,
            String requestHash, String targetSnapshotHash, String action, boolean compatibility) {
        lock(em);
        em.flush();
        bumpScopeRevision(em, scope, resource);
        MemoryPolicyVO desired = policy(em, scope, resource);
        if (specializedPolicy != null) { desired.setPolicy(specializedPolicy); }
        MemoryTaskVO parent = configurationTask(scope, resource, action, null, null,
                desired.getRevision(), "{}", actor);
        parent.setRequestKey(requestKey); parent.setRequestHash(requestHash);
        parent.setTargetSnapshotHash(targetSnapshotHash);
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
            if (!compatibility && plan.changedFields.isEmpty()) { continue; }
            String defer = compatibility ? null : standardMutationDeferReason(em, host, state);
            if (defer != null) { plan = deferredPlan(plan, defer); }
            // The standard batch is one candidate. Do not partially dispatch a
            // writeback/reclaim configuration when any field lacks admission.
            boolean canIssue = compatibility || (defer == null && plan.hasPayload && !plan.hasBlockers);
            String status = canIssue ? "Queued" : "Blocked";
            // An accepted request is not evidence that a legacy operation was
            // applied. Keep NeedsReview until the matching fresh Agent result.
            if (!compatibility && !"NeedsReview".equals(state.getPolicyPlanStatus())) {
                recordPolicyPlan(state, plan, status);
            }
            long revision = Math.addExact(state.getDesiredRevision(), 1L);
            MemoryTaskVO child = configurationTask(scope, resource, action, parent.getUuid(), host, revision,
                    compatibility ? effectivePolicy : canIssue ? plan.payload : "{}", actor);
            if (!compatibility) { child.setPolicyPlanHash(plan.planHash); }
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

    private String standardMutationDeferReason(EntityManager em, String host, MemoryStateVO state) {
        HostStatus status = em.createQuery("select h.status from HostVO h where h.uuid = :uuid", HostStatus.class)
                .setParameter("uuid", host).getResultList().stream().findFirst().orElse(null);
        if (status != HostStatus.Connected) { return "HOST_NOT_CONNECTED"; }
        if (state.getActiveTaskUuid() != null || hasUnresolvedControl(em, state)) { return "HOST_OPERATION_UNRESOLVED"; }
        if ("Unknown".equals(state.getStatus()) || state.getStatus() == null) { return "HOST_STATE_UNKNOWN"; }
        if (!hasFreshCapabilityObservation(state, System.currentTimeMillis())) { return "CAPABILITY_STALE"; }
        return configurationControlDeferReason(em, state);
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

    private MemoryTaskVO configurationTask(String scope, String resource, String action,
            String parent, String host, long revision, String payload, String actor) {
        MemoryTaskVO task = new MemoryTaskVO();
        task.setUuid(UUID.randomUUID().toString().replace("-", ""));
        task.setParentUuid(parent); task.setHostUuid(host); task.setScope(scope);
        task.setResourceUuid(resource); task.setAction(action);
        task.setDesiredRevision(revision); task.setPolicy(payload); task.setActorUuid(actor);
        task.setStatus("Queued"); return task;
    }

    public void rejectGenericStandardGlobalWrite(String category, String name, String newValue) {
        transaction(em -> {
            List<String> persisted = em.createQuery("select g.value from GlobalConfigVO g where g.category = :category and g.name = :name", String.class)
                    .setParameter("category", category).setParameter("name", name).getResultList();
            if (!persisted.isEmpty() && Objects.equals(persisted.get(0), newValue)) { return null; }
            throw error("MEMORY_INVALID_ACTION", "Memory scalar settings must be changed through the atomic memory policy API");
        });
    }

    public void rejectGenericStandardResourceWrite() {
        throw error("MEMORY_INVALID_ACTION", "Memory scalar settings must be changed through the atomic memory policy API");
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

    private void assertNoManagedOwnership(EntityManager em, String hostUuid) {
        MemoryStateVO state = em.find(MemoryStateVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
        if (state == null) { return; }
        if (state.getStatus() == null || "Unknown".equalsIgnoreCase(state.getStatus())) {
            throw error("MEMORY_CONTROLLER_CONFLICT", "host memory ownership state is unknown; refusing legacy host KSM update");
        }
        try {
            if (state.getState() != null) {
                Map<?, ?> observed = JSONObjectUtil.toObject(state.getState(), Map.class);
                if (Boolean.TRUE.equals(observed.get("managed"))) {
                    throw error("MEMORY_CONTROLLER_CONFLICT", "host KSM is owned by the memory controller; use the memory policy API");
                }
            }
        } catch (MemoryOperationException e) { throw e; }
        catch (RuntimeException malformed) {
            throw error("MEMORY_CONTROLLER_CONFLICT", "host memory ownership state is uncertain; refusing legacy host KSM update");
        }
        if (state.getActiveTaskUuid() != null) {
            throw error("MEMORY_CONTROLLER_CONFLICT", "host memory operation is active; refusing legacy host KSM update");
        }
        if (state.getControlOperationUuid() != null) {
            MemoryTaskVO task = em.find(MemoryTaskVO.class, state.getControlOperationUuid());
            if (task == null || !("Succeeded".equals(task.getStatus()) || "Failed".equals(task.getStatus())
                    || "Cancelled".equals(task.getStatus()))) {
                throw error("MEMORY_CONTROLLER_CONFLICT", "host memory control state is uncertain; refusing legacy host KSM update");
            }
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

    /** A stored legacy "none" masks only inherited ksm.enabled; it never means false. */
    private String mergeEffectiveLayer(EntityManager em, String inherited, String scope,
            String resource, String candidateOwn) {
        return mergeEffectiveLayer(em, inherited, scope, resource, candidateOwn, false, false);
    }

    private String mergeEffectiveLayer(EntityManager em, String inherited, String scope,
            String resource, String candidateOwn, boolean ignoreStoredKsmOverride, boolean forceUnmanagedKsm) {
        Object legacy = null;
        if (!ignoreStoredKsmOverride) {
            if ("Global".equals(scope)) { legacy = standardConfigs.readGlobalOwn(em, "ksm.enabled"); }
            else if ("Host".equals(scope) || "Cluster".equals(scope)) {
                legacy = standardConfigs.readOverrideOwn(em, "ksm.enabled", resource);
            }
        }
        if (forceUnmanagedKsm || MemoryStandardConfigAdapter.isUnmanaged(legacy)) {
            inherited = removePolicyPath(inherited, "ksm.enabled");
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
        if (level != null) {
            for (MemoryStandardField field : MemoryStandardField.values()) {
                if (field.allows(level) && (replacedFields.contains(field.path())
                        || standardFieldAuthoritative(em, scope, resource, field))) {
                    compatibility = MemoryStandardConfigCodec.put(compatibility, field.path(), null);
                }
            }
        }
        return MemoryStandardPolicy.combine(compatibility, standardOwnPolicy(em, scope, resource, replacedFields));
    }

    private boolean standardFieldAuthoritative(EntityManager em, String scope, String resource,
            MemoryStandardField field) {
        if ("Global".equals(scope)) {
            Object value = standardConfigs.readGlobalOwn(em, field.path());
            // The Global KSM default is the legacy "none" mode; it must mask
            // schema defaults and stale compatibility JSON even when not explicit.
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
        JsonObject result = new JsonObject(); result.addProperty("schemaVersion", 1);
        for (MemoryStandardField field : MemoryStandardField.values()) {
            if (!field.allows(standardScope) || skipFields.contains(field.path())) { continue; }
            Object value = "Global".equals(scope) ? standardConfigs.readGlobalOwn(em, field.path())
                    : standardConfigs.readOverrideOwn(em, field.path(), resource);
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
            lock(em); validateResource(em, "VM", vm);
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
            lock(em);
            MemoryVmExclusionVO exclusion = em.find(MemoryVmExclusionVO.class, vm);
            if (exclusion != null && !exclusion.retained) { em.remove(exclusion); }
            return null;
        });
    }

    public String vmParticipation(String vm) {
        return transaction(em -> {
            lock(em);
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
        }
        // Preview patches belong to the edited scope, including explicit false.
        // A GET's ownPolicy includes schema defaults; they are not explicit writes.
        if (previewPatch != null) { MemoryPolicyRules.addFieldSources(previewPatch, scope + ":" + resource, fields); }
        result.setSourceRevisions(revisions);
        result.setFieldSources(fields);
        result.setFieldModes(unmanagedFieldModes(em, scope, resource, previewClearedFields,
                previewPatch != null && MemoryStandardConfigCodec.get(previewPatch, "ksm.enabled") != null));
        if (MemoryStandardConfigCodec.get(result.getEffectivePolicy(), "ksm.enabled") == null
                && !result.getFieldModes().containsKey("ksm.enabled")) {
            fields.remove("ksm.enabled");
        }
        result.setSourceRevision(policy(em, "Global", "global").getRevision());
    }

    private Map<String, String> unmanagedFieldModes(EntityManager em, String scope, String resource,
            Set<String> previewClearedFields, boolean previewManagesKsm) {
        Map<String, String> modes = new LinkedHashMap<>();
        if ("VM".equals(scope)) { return modes; }
        for (MemoryPolicyVO source : sourceChain(em, scope, resource)) {
            if (source.getUuid().equals(scope + ":" + resource)
                    && previewClearedFields.contains("ksm.enabled")) { continue; }
            boolean authoritative = standardFieldAuthoritative(em, source.getScope(), source.getResourceUuid(),
                    MemoryStandardField.KSM_ENABLED);
            Object value = standardValue(em, source, MemoryStandardField.KSM_ENABLED);
            boolean schemaDefault = "Global".equals(source.getScope())
                    && standardConfigs.globalIsSchemaDefault(em, "ksm.enabled");
            if (authoritative && MemoryStandardConfigAdapter.isUnmanaged(value) && !schemaDefault) {
                modes.put("ksm.enabled", "Unmanaged");
            } else if (authoritative || MemoryStandardConfigCodec.get(source.getPolicy(), "ksm.enabled") != null) {
                modes.remove("ksm.enabled");
            }
        }
        if (previewManagesKsm) { modes.remove("ksm.enabled"); }
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
            if (schemaDefault) { continue; }
            if (MemoryStandardConfigAdapter.isUnmanaged(value)) {
                fields.remove(field.path());
                fields.put(field.path(), source.getUuid());
            } else if (value != null) {
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

    public MemoryPolicyInventory getPolicy(String requestedScope, String resource) {
        return transaction(em -> {
            String scope = resolvePolicyScope(em, requestedScope, resource);
            if ("VM".equals(scope)) { lock(em); }
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

    public MemoryPolicyInventory preview(String scope, String resource, String patch) {
        return preview(scope, resource, patch, "apply", null);
    }

    public MemoryPolicyInventory preview(String requestedScope, String resource, String patch, String action, List<String> clearOverrideFields) {
        return transaction(em -> {
            String scope = resolvePolicyScope(em, requestedScope, resource);
            String effectiveAction = action == null ? "apply" : action;
            if (!"apply".equals(effectiveAction) && !"clearOverride".equals(effectiveAction)) {
                throw new IllegalArgumentException("action must be apply or clearOverride");
            }
            if ("clearOverride".equals(effectiveAction) && "Global".equals(scope)) {
                throw new IllegalArgumentException("Global policy has no parent override to clear");
            }
            MemoryPolicyVO current = policy(em, scope, resource);
            MemoryPolicyInventory result = current.toInventory();
            String previewPolicy = candidateOwnPolicy(em, scope, resource, patch, effectiveAction, clearOverrideFields);
            result.setPolicy(previewPolicy);
            result.setEffectivePolicy("VM".equals(scope) ? result.getPolicy()
                    : mergeEffectiveLayer(em, parentEffective(em, scope, resource), scope, resource,
                        result.getPolicy(), clearsKsmEnabled(effectiveAction, clearOverrideFields), false));
            Set<String> previewClearedFields = "clearOverride".equals(effectiveAction)
                    && clearOverrideFields != null ? new HashSet<>(clearOverrideFields)
                    : Collections.<String>emptySet();
            describeSources(em, scope, resource, result, previewClearedFields,
                    "apply".equals(effectiveAction) ? patch : null);
            return result;
        });
    }

    /** Preview parent-scope changes after applying each Host's explicit overrides. */
    Map<String, MemoryHostPolicyPreviewSnapshot> previewHostPolicies(String scope, String resource,
            String patch, String action, List<String> clearOverrideFields, List<String> hosts) {
        return transaction(em -> {
            validateResource(em, scope, resource);
            String ownCandidate = candidateOwnPolicy(em, scope, resource, patch, action, clearOverrideFields);
            Map<String, MemoryHostPolicyPreviewSnapshot> result = new LinkedHashMap<>();
            for (String host : hosts) {
                result.put(host, new MemoryHostPolicyPreviewSnapshot(
                        effective(em, "Host", host), effectiveHostWithCandidate(em, host, scope, resource, ownCandidate,
                                clearsKsmEnabled(action, clearOverrideFields), false),
                        em.find(MemoryStateVO.class, host)));
            }
            return result;
        });
    }

    private String candidateOwnPolicy(EntityManager em, String scope, String resource,
            String patch, String action, List<String> clearOverrideFields) {
        String effectiveAction = action == null ? "apply" : action;
        MemoryPolicyVO current = policy(em, scope, resource);
        String specialized = current.getPolicy();
        if ("clearOverride".equals(effectiveAction)) {
            if ("Global".equals(scope)) { throw new IllegalArgumentException("Global policy has no parent override to clear"); }
            MemoryStandardPolicy.ClearParts clear = MemoryStandardPolicy.splitClearPaths(clearOverrideFields, scope);
            String standard = standardOwnPolicy(em, scope, resource, new HashSet<>(clear.standardPaths));
            if (!clear.specializedPaths.isEmpty()) {
                specialized = MemoryPolicyRules.clearFields(specialized, clear.specializedPaths, scope);
            }
            for (String path : clear.standardPaths) {
                standard = MemoryStandardPolicy.set(standard, path, null);
                specialized = MemoryStandardConfigCodec.put(specialized, path, null);
            }
            return MemoryStandardPolicy.combine(specialized, standard);
        }
        String standard = standardOwnPolicy(em, scope, resource);
        MemoryStandardPolicy.Parts parts = MemoryStandardPolicy.split(patch, scope);
        specialized = MemoryPolicyRules.mergeOverridesOnly(specialized, parts.specialized, scope);
        for (Map.Entry<String, Object> value : parts.standardValues.entrySet()) {
            standard = MemoryStandardPolicy.set(standard, value.getKey(), value.getValue());
            specialized = MemoryStandardConfigCodec.put(specialized, value.getKey(), null);
        }
        return MemoryStandardPolicy.combine(specialized, standard);
    }

    private boolean clearsKsmEnabled(String action, List<String> clearOverrideFields) {
        return "clearOverride".equals(action) && clearOverrideFields != null
                && clearOverrideFields.contains("ksm.enabled");
    }

    private String effectiveHostWithCandidate(EntityManager em, String host, String changedScope,
            String changedResource, String candidateOwn) {
        return effectiveHostWithCandidate(em, host, changedScope, changedResource, candidateOwn, false, false);
    }

    private String effectiveHostWithCandidate(EntityManager em, String host, String changedScope,
            String changedResource, String candidateOwn, boolean ignoreStoredKsmOverride, boolean forceUnmanagedKsm) {
        String global = "Global".equals(changedScope) && "global".equals(changedResource)
                ? candidateOwn : ownPolicy(em, "Global", "global");
        String result = mergeEffectiveLayer(em, MemoryPolicyRules.defaults(), "Global", "global", global,
                "Global".equals(changedScope) && "global".equals(changedResource) && ignoreStoredKsmOverride,
                "Global".equals(changedScope) && "global".equals(changedResource) && forceUnmanagedKsm);
        String cluster = em.createQuery("select h.clusterUuid from HostVO h where h.uuid = :uuid", String.class)
                .setParameter("uuid", host).getSingleResult();
        if (cluster != null) {
            String clusterPolicy = "Cluster".equals(changedScope) && cluster.equals(changedResource)
                    ? candidateOwn : ownPolicy(em, "Cluster", cluster);
            result = mergeEffectiveLayer(em, result, "Cluster", cluster, clusterPolicy,
                    "Cluster".equals(changedScope) && cluster.equals(changedResource) && ignoreStoredKsmOverride,
                    "Cluster".equals(changedScope) && cluster.equals(changedResource) && forceUnmanagedKsm);
        }
        String hostPolicy = "Host".equals(changedScope) && host.equals(changedResource)
                ? candidateOwn : ownPolicy(em, "Host", host);
        return mergeEffectiveLayer(em, result, "Host", host, hostPolicy,
                "Host".equals(changedScope) && host.equals(changedResource) && ignoreStoredKsmOverride,
                "Host".equals(changedScope) && host.equals(changedResource) && forceUnmanagedKsm);
    }

    private void checkCapabilityAdmission(EntityManager em, String scope, String resource,
            String candidateOwn, List<String> hosts, boolean ignoreStoredKsmOverride, boolean forceUnmanagedKsm) {
        for (String host : hosts) {
            String before = effective(em, "Host", host);
            String after = effectiveHostWithCandidate(em, host, scope, resource, candidateOwn,
                    ignoreStoredKsmOverride, forceUnmanagedKsm);
            Map<String, String> changed = MemoryPolicyAdmissionRules.changedFields(before, after);
            MemoryStateVO state = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
            Map<String, String> blocked = MemoryPolicyAdmissionRules.blockedFields(
                    changed, state, System.currentTimeMillis(), after);
            if (!blocked.isEmpty()) {
                Map.Entry<String, String> first = blocked.entrySet().iterator().next();
                throw error("MEMORY_CAPABILITY_PRECHECK",
                        "Host " + host + " cannot apply " + first.getKey() + ": " + first.getValue());
            }
        }
    }

    public MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg) {
        // Normalize before action validation and request hashing so omission and
        // the equivalent old explicit scope share one idempotency identity.
        msg.setScope(transaction(em -> resolvePolicyScope(em, msg.getScope(), msg.getResourceUuid())));
        return submit(msg, null, null, false);
    }

    private MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg, String targetSnapshotHash) {
        return submit(msg, targetSnapshotHash, null, false);
    }

    /** Internal first-install path; uses the same admission and task transaction as API updates. */
    public MemoryTaskInventory submitFreshCloudBootstrap(String requestUuid) {
        APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
        msg.setScope("Global"); msg.setResourceUuid("global"); msg.setAction("apply");
        msg.setExpectedRevision(0);
        msg.setTargetHostUuids(targets("Global", "global"));
        msg.setPolicy("{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true},"
                + "\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":false}}");
        msg.setClientRequestUuid(requestUuid);
        return submit(msg, null, "system:memory-fresh-cloud-bootstrap", true);
    }

    private MemoryTaskInventory submit(APIUpdateMemoryPolicyMsg msg, String targetSnapshotHash,
                                       String internalActor, boolean freshCloudBootstrap) {
        MemoryBackendPreparationRules.validate(msg);
        MemoryZramPoolPreparationRules.validate(msg);
        MemoryUncertainRecoveryRules.validate(msg);
        // The internal assembled-commit call has already validated the public
        // shard envelope and intentionally carries its metadata into apply.
        if (targetSnapshotHash == null) { MemoryPolicyActionParameters.validate(msg); }
        boolean recovery = MemoryUncertainRecoveryRules.ACTION.equals(msg.getAction());
        boolean preparation = MemoryBackendPreparationRules.ACTION.equals(msg.getAction()) ||
                MemoryZramPoolPreparationRules.ACTION.equals(msg.getAction());
        if (!MemoryPolicyRules.validUuid(msg.getClientRequestUuid())) {
            throw error("MEMORY_INVALID_REQUEST", "clientRequestUuid must be a UUID");
        }
        String actor = internalActor == null
                ? msg.getSession().getAccountUuid() + ":" + String.valueOf(msg.getSession().getUserUuid())
                : internalActor;
        String key = actor + ":" + msg.getClientRequestUuid();
        if ("stageTargetShard".equals(msg.getAction())) { return stageTargetShard(msg, actor); }
        if ("cancelTargetShards".equals(msg.getAction())) { return cancelTargetShards(msg, actor); }
        if ("commitTargetShards".equals(msg.getAction())) { return commitTargetShards(msg, actor); }
        List<Object> requestIdentity = new ArrayList<>(Arrays.asList(msg.getScope(), msg.getResourceUuid(),
                msg.getAction(), msg.getExpectedRevision(), msg.getExpectedGlobalRevision(), msg.getExpectedSourceRevisions(),
                msg.getClearOverrideFields(), msg.getExpectedInstanceGeneration(), msg.getTargetHostUuids(),
                JSONObjectUtil.toObject(msg.getPolicy(), Map.class)));
        if (preparation) {
            requestIdentity.add(msg.getBackendPreparation()); requestIdentity.add(msg.getPoolPreparation());
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
            MemoryCloudBootstrapVO bootstrap = null;
            if (freshCloudBootstrap) {
                bootstrap = em.find(MemoryCloudBootstrapVO.class, MemoryCloudBootstrapVO.GLOBAL,
                        LockModeType.PESSIMISTIC_WRITE);
                if (bootstrap == null || !MemoryCloudBootstrapVO.PENDING.equals(bootstrap.getStatus())) { return null; }
                MemoryPolicyVO globalPolicy = em.find(MemoryPolicyVO.class, GLOBAL, LockModeType.PESSIMISTIC_WRITE);
                if (globalPolicy == null || globalPolicy.getRevision() != 0 || hasExplicitManagedPolicy(em)) {
                    bootstrap.setStatus(MemoryCloudBootstrapVO.CANCELLED);
                    bootstrap.setReason("An explicit memory policy already exists; fresh defaults were not applied");
                    return null;
                }
            }
            List<String> availableTargets = targets(em, msg.getScope(), msg.getResourceUuid());
            // Fresh bootstrap persists one global target even when the Cloud has
            // no KVM Hosts yet; ordinary user requests still require explicit targets.
            List<String> hosts = freshCloudBootstrap && availableTargets.isEmpty()
                    ? Collections.emptyList()
                    : MemoryTargetRules.select(msg.getScope(), availableTargets, msg.getTargetHostUuids());
            if ("VM".equals(msg.getScope()) && !hosts.isEmpty() && msg.getExpectedInstanceGeneration() == null) {
                throw error("MEMORY_INSTANCE_REQUIRED", "Refresh the actual VM instance generation before changing participation");
            }
            if (!freshCloudBootstrap && !"VM".equals(msg.getScope()) && !"reconcile".equals(msg.getAction())) {
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
            if (!freshCloudBootstrap && !"VM".equals(msg.getScope())
                    && ("apply".equals(msg.getAction()) || "clearOverride".equals(msg.getAction()))) {
                for (String host : hosts) {
                    MemoryStateVO paused = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
                    if (paused == null) { continue; }
                    String defer = configurationControlDeferReason(em, paused);
                    if (defer != null) {
                        throw error("MEMORY_CONTROL_PAUSED_RESUME_REQUIRED",
                                ("HOST_DRAIN_MAINTENANCE_REQUIRED".equals(defer)
                                        ? "Complete explicit drained-pool maintenance before changing policy: "
                                        : "Resume the Host control operation before changing policy: ") + host);
                    }
                }
            }
            MemoryPolicyVO desired = policy(em, msg.getScope(), msg.getResourceUuid());
            if (desired.getRevision() != msg.getExpectedRevision()) {
                throw error("MEMORY_REVISION_CONFLICT", "Policy has changed; refresh before applying");
            }
            boolean update = "apply".equals(msg.getAction()) || "clearOverride".equals(msg.getAction());
            if (update || preparation || msg.getExpectedSourceRevisions() != null
                    || msg.getExpectedGlobalRevision() != null) { checkSources(em, msg); }
            if (Arrays.asList("apply", "clearOverride", "pause", "drain").contains(msg.getAction())
                    && msg.getExpectedControlOperationUuid() != null) {
                for (String host : hosts) {
                    MemoryStateVO control = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
                    if (control == null || !Objects.equals(control.getControlOperationUuid(), msg.getExpectedControlOperationUuid())) {
                        throw error("MEMORY_CONTROL_OPERATION_FENCED", "Refresh the Host control operation: " + host);
                    }
                }
            }
            if ("clearOverride".equals(msg.getAction()) && !MemoryTaskRules.isSafetyAction("pause", msg.getPolicy())) {
                throw error("MEMORY_INVALID_ACTION", "clearOverride cannot contain configuration");
            }
            if (!update && !preparation && !recovery && !MemoryTaskRules.isSafetyAction(msg.getAction(), msg.getPolicy())) {
                throw error("MEMORY_INVALID_ACTION", "Safety actions cannot contain configuration");
            }
            if (update) {
                String next = desired.getPolicy();
                Map<String, Object> standardChanges = new LinkedHashMap<>();
                if ("clearOverride".equals(msg.getAction())) {
                    MemoryStandardPolicy.ClearParts clear = MemoryStandardPolicy.splitClearPaths(msg.getClearOverrideFields(), msg.getScope());
                    if (!clear.specializedPaths.isEmpty()) {
                        next = MemoryPolicyRules.clearFields(next, clear.specializedPaths, msg.getScope());
                    }
                    for (String path : clear.standardPaths) {
                        standardChanges.put(path, null);
                        next = MemoryStandardConfigCodec.put(next, path, null);
                    }
                } else {
                    MemoryStandardPolicy.Parts parts = MemoryStandardPolicy.split(msg.getPolicy(), msg.getScope());
                    next = MemoryPolicyRules.mergeOverridesOnly(next, parts.specialized, msg.getScope());
                    standardChanges.putAll(parts.standardValues);
                    for (String path : parts.standardValues.keySet()) {
                        next = MemoryStandardConfigCodec.put(next, path, null);
                    }
                }
                String candidateOwn = candidateOwnPolicy(em, msg.getScope(), msg.getResourceUuid(),
                        msg.getPolicy(), msg.getAction(), msg.getClearOverrideFields());
                boolean replacingKsmOverride = standardChanges.containsKey("ksm.enabled")
                        || clearsKsmEnabled(msg.getAction(), msg.getClearOverrideFields());
                String candidateEffective = "VM".equals(msg.getScope()) ? candidateOwn
                        : "Global".equals(msg.getScope())
                            ? mergeEffectiveLayer(em, MemoryPolicyRules.defaults(), "Global", "global", candidateOwn,
                                    replacingKsmOverride, false)
                    : mergeEffectiveLayer(em, parentEffective(em, msg.getScope(), msg.getResourceUuid()),
                            msg.getScope(), msg.getResourceUuid(), candidateOwn, replacingKsmOverride, false);
                if (!freshCloudBootstrap && !"VM".equals(msg.getScope())) {
                    validateConfigurationCandidate(em, msg.getScope(), msg.getResourceUuid(), candidateOwn,
                            replacingKsmOverride, false);
                    checkCapabilityAdmission(em, msg.getScope(), msg.getResourceUuid(), candidateOwn, hosts,
                            replacingKsmOverride, false);
                }
                if ("Global".equals(msg.getScope()) && MemoryPolicyRules.decode(candidateEffective).writeback != null
                        && Boolean.TRUE.equals(MemoryPolicyRules.decode(candidateEffective).writeback.enabled)) {
                    throw error("MEMORY_INVALID_POLICY", "Writeback backends must be configured separately on each Host");
                }

                Set<GlobalConfig> refreshGlobal = new LinkedHashSet<>();
                List<MemoryStandardConfigAdapter.ResourceChange> refreshResources = new ArrayList<>();
                if (!standardChanges.isEmpty()) {
                    if ("Global".equals(msg.getScope())) {
                        for (Map.Entry<String, Object> change : standardChanges.entrySet()) {
                            MemoryStandardConfigAdapter.ChangeSet result = standardConfigs.writeGlobal(em, change.getKey(), change.getValue());
                            refreshGlobal.addAll(result.refreshAfterCommit());
                        }
                    } else {
                        String resourceType = "Host".equals(msg.getScope()) ? HostVO.class.getSimpleName()
                                : org.zstack.header.cluster.ClusterVO.class.getSimpleName();
                        MemoryStandardConfigAdapter.ChangeSet result = standardConfigs.writeOverrides(
                                em, msg.getResourceUuid(), resourceType, standardChanges);
                        refreshGlobal.addAll(result.refreshAfterCommit()); refreshResources.addAll(result.resourceChanges());
                    }
                }
                if (!freshCloudBootstrap && !"VM".equals(msg.getScope())) {
                    registerStandardConfigRefresh(refreshGlobal, refreshResources);
                    return commitConfiguration(em, msg.getScope(), msg.getResourceUuid(), next, hosts,
                            actor, key, hash, targetSnapshotHash, msg.getAction(), true);
                }
                desired.setPolicy(next);
                desired.setRevision(desired.getRevision() + 1);
                boolean explicitParticipation = "clearOverride".equals(msg.getAction())
                        ? msg.getClearOverrideFields() == null || msg.getClearOverrideFields().isEmpty()
                            || msg.getClearOverrideFields().contains("participation")
                        : MemoryPolicyRules.decode(msg.getPolicy()).participation != null;
                if ("VM".equals(msg.getScope()) && explicitParticipation) {
                    MemoryVmExclusionVO exclusion = em.find(MemoryVmExclusionVO.class, msg.getResourceUuid());
                    if (exclusion != null) { em.remove(exclusion); }
                } else if ("VM".equals(msg.getScope())) {
                    MemoryVmExclusionVO exclusion = em.find(MemoryVmExclusionVO.class, msg.getResourceUuid());
                    if (exclusion != null) { exclusion.vmPolicyRevision = desired.getRevision(); }
                }
                em.merge(desired);
                em.flush();
                registerStandardConfigRefresh(refreshGlobal, refreshResources);
            }
            MemoryTaskVO parent = task(msg, null, null, desired.getRevision(), "{}", actor);
            parent.setRequestKey(key);
            parent.setRequestHash(hash);
            parent.setTargetSnapshotHash(targetSnapshotHash);
            parent.setStatus(hosts.isEmpty() ? "Succeeded" : "Queued");
            parent.setReason(freshCloudBootstrap
                    ? "Fresh-install target persisted; inspect per-Host policy admission status" : null);
            bumpQueryVersion(em, "task");
            em.persist(parent);
            boolean bootstrapBlocked = false;
            boolean bootstrapQueued = false;
            for (String host : hosts) {
                if (freshCloudBootstrap) {
                    MemoryStateVO bootstrapState = em.find(MemoryStateVO.class, host, LockModeType.PESSIMISTIC_WRITE);
                    if (bootstrapState == null) {
                        bootstrapState = new MemoryStateVO(); bootstrapState.setHostUuid(host);
                        bootstrapState.setStatus("Unknown"); em.persist(bootstrapState);
                        bumpQueryVersion(em, "state");
                    }
                    HostPolicyPlan plan = planForHost(em, host, effective(em, "Host", host), bootstrapState);
                    HostStatus hostStatus = em.createQuery("select h.status from HostVO h where h.uuid = :uuid", HostStatus.class)
                            .setParameter("uuid", host).getResultList().stream().findFirst().orElse(null);
                    String deferReason = null;
                    if (hostStatus != HostStatus.Connected) { deferReason = "HOST_NOT_CONNECTED"; }
                    else if (bootstrapState.getActiveTaskUuid() != null) { deferReason = "HOST_OPERATION_UNRESOLVED"; }
                    else if ("Unknown".equals(bootstrapState.getStatus())) { deferReason = "HOST_STATE_UNKNOWN"; }
                    else {
                        try {
                            if (isConfirmedPausedState(bootstrapState.getState())) { deferReason = "HOST_POLICY_PAUSED"; }
                        } catch (RuntimeException e) { deferReason = "HOST_CONTROL_STATE_UNKNOWN"; }
                    }
                    if (deferReason != null) { plan = deferredPlan(plan, deferReason); }
                    if (!plan.hasPayload && !plan.hasBlockers) {
                        recordPolicyPlan(bootstrapState, plan, "Applied");
                        continue;
                    }
                    String planStatus = plan.hasPayload && deferReason == null ? "Queued" : "Blocked";
                    recordPolicyPlan(bootstrapState, plan, planStatus);
                    long revision = bootstrapState.getDesiredRevision() + 1;
                    MemoryTaskVO bootstrapChild = task(msg, parent.getUuid(), host, revision,
                            plan.hasPayload && deferReason == null ? plan.payload : "{}", actor);
                    bootstrapChild.setPolicyPlanHash(plan.planHash);
                    if ("Blocked".equals(planStatus)) {
                        bootstrapChild.setStatus("Blocked");
                        bootstrapChild.setReason("Policy not applied: " + plan.blockedFields);
                        em.persist(bootstrapChild);
                        bootstrapBlocked = true;
                        continue;
                    }
                    em.persist(bootstrapChild);
                    bootstrapState.setActiveTaskUuid(bootstrapChild.getUuid());
                    bootstrapState.setDesiredRevision(revision);
                    bootstrapState.setControlOperationUuid(bootstrapChild.getUuid());
                    bootstrapState.setPermitAuthorized(false);
                    bootstrapState.setStatus("Queued");
                    bootstrapQueued = true;
                    continue;
                }
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
                        : update ? state.getDesiredRevision() + 1 : state.getDesiredRevision();
                MemoryTaskVO child = task(msg, parent.getUuid(), host, revision,
                        recovery ? JSONObjectUtil.toJsonString(msg.getRecovery())
                                : preparation ? JSONObjectUtil.toJsonString(MemoryZramPoolPreparationRules.ACTION.equals(msg.getAction())
                                ? msg.getPoolPreparation() : msg.getBackendPreparation())
                                : "VM".equals(msg.getScope()) ? desired.getPolicy() : effective(em, "Host", host), actor);
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
                    }
                }
                em.persist(child);
                state.setActiveTaskUuid(child.getUuid());
                if (rejectedResume != null) { state.setStatus("Queued"); }
                if (!vmOnly) {
                    state.setDesiredRevision(revision);
                    // Maintenance must retain the original drain fence for native proof.
                    if (!preparation && !preserveMaintenanceControlFence) {
                        state.setControlOperationUuid(child.getUuid());
                    }
                    // A stale apply/lease callback must not restart a later pause or exit.
                    state.setPermitAuthorized(false);
                }
            }
            if (freshCloudBootstrap && bootstrap != null) {
                bootstrap.setStatus(MemoryCloudBootstrapVO.APPLIED);
                bootstrap.setTaskUuid(parent.getUuid());
                bootstrap.setTargetPolicyHash(digest(desired.getPolicy()));
                bootstrap.setReason("Fresh-install target persisted; per-Host results are tracked independently");
                if (!bootstrapQueued && bootstrapBlocked) { parent.setStatus("Blocked"); }
                else if (!bootstrapQueued) { parent.setStatus("Succeeded"); }
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
        String drainUuid = msg.getRecovery().getDrainControlOperationUuid();
        MemoryTaskVO drain = em.find(MemoryTaskVO.class, drainUuid);
        String boot = null;
        try { boot = (String) JSONObjectUtil.toObject(state.getState(), Map.class).get("bootId"); }
        catch (RuntimeException ignored) { }
        long now = System.currentTimeMillis();
        if (!Objects.equals(state.getActiveTaskUuid(), msg.getExpectedControlOperationUuid())
                || !Objects.equals(state.getControlOperationUuid(), msg.getExpectedControlOperationUuid())
                || original == null || !"Unknown".equals(original.getStatus())
                || !Arrays.asList("apply", "clearOverride").contains(original.getAction())
                || !"Host".equals(original.getScope()) || !Objects.equals(state.getHostUuid(), original.getHostUuid())
                || drain == null || !"drain".equals(drain.getAction()) || !"Succeeded".equals(drain.getStatus())
                || !Objects.equals(state.getHostUuid(), drain.getHostUuid())
                || !Objects.equals(boot, msg.getRecovery().getExpectedHostBootId())
                || state.getLastSampleTime() == null || state.getLastSampleTime() > now + 5000
                || now - state.getLastSampleTime() > MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            throw error("MEMORY_UNCERTAIN_RECOVERY_FENCED",
                    "Refresh the same-boot Host state and its exact unresolved apply/drain before recovery");
        }
    }

    private boolean hasExplicitManagedPolicy(EntityManager em) {
        for (MemoryPolicyVO row : em.createQuery("from MemoryPolicyVO p where p.scope in ('Global', 'Cluster', 'Host')",
                MemoryPolicyVO.class).getResultList()) {
            if ("Global".equals(row.getScope()) && "global".equals(row.getResourceUuid())
                    && row.getRevision() > 0) { return true; }
            try {
                Map<String, String> fields = new LinkedHashMap<>();
                MemoryPolicyRules.addFieldSources(row.getPolicy(), row.getScope(), fields);
                if (!fields.isEmpty()) { return true; }
            } catch (RuntimeException e) {
                return true; // An unreadable policy is not safe to replace.
            }
        }
        for (MemoryStandardField field : MemoryStandardField.values()) {
            if (standardConfigs.globalStoredValue(em, field.path()) != null
                    && !standardConfigs.globalIsSchemaDefault(em, field.path())
                    && standardConfigs.readGlobalOwn(em, field.path()) != null) { return true; }
            List<ResourceConfigVO> rows = em.createQuery("select r from ResourceConfigVO r where r.category = :category and r.name = :name", ResourceConfigVO.class)
                    .setParameter("category", field.category()).setParameter("name", field.configName()).getResultList();
            for (ResourceConfigVO row : rows) {
                if (standardConfigs.readOverrideOwn(em, field.path(), row.getResourceUuid()) != null) { return true; }
            }
        }
        return false;
    }

    public MemoryCloudBootstrapVO freshCloudBootstrap() {
        return transaction(em -> {
            MemoryCloudBootstrapVO row = em.find(MemoryCloudBootstrapVO.class, MemoryCloudBootstrapVO.GLOBAL);
            if (row == null) { return null; }
            MemoryCloudBootstrapVO copy = new MemoryCloudBootstrapVO();
            copy.setUuid(row.getUuid()); copy.setStatus(row.getStatus()); copy.setRequestUuid(row.getRequestUuid());
            copy.setTaskUuid(row.getTaskUuid()); copy.setTargetPolicyHash(row.getTargetPolicyHash());
            copy.setReason(row.getReason());
            copy.setCreateDate(row.getCreateDate()); copy.setLastOpDate(row.getLastOpDate());
            return copy;
        });
    }

    public String freshCloudBootstrapBlockReason() {
        return transaction(em -> {
            lock(em);
            MemoryCloudBootstrapVO marker = em.find(MemoryCloudBootstrapVO.class, MemoryCloudBootstrapVO.GLOBAL,
                    LockModeType.PESSIMISTIC_WRITE);
            if (marker == null || !MemoryCloudBootstrapVO.PENDING.equals(marker.getStatus())) { return null; }
            MemoryPolicyVO global = em.find(MemoryPolicyVO.class, GLOBAL, LockModeType.PESSIMISTIC_WRITE);
            if (global == null || global.getRevision() != 0 || hasExplicitManagedPolicy(em)) {
                marker.setStatus(MemoryCloudBootstrapVO.CANCELLED);
                marker.setReason("An explicit memory policy already exists; fresh defaults were not applied");
                return "EXPLICIT_MEMORY_POLICY_PRESENT";
            }
            return null;
        });
    }

    public void updateFreshCloudBootstrapReason(String reason) {
        transaction(em -> {
            MemoryCloudBootstrapVO row = em.find(MemoryCloudBootstrapVO.class, MemoryCloudBootstrapVO.GLOBAL,
                    LockModeType.PESSIMISTIC_WRITE);
            if (row != null && MemoryCloudBootstrapVO.PENDING.equals(row.getStatus())) {
                row.setReason(reason == null ? null : reason.substring(0, Math.min(2048, reason.length())));
            }
            return null;
        });
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
    }

    private HostPolicyPlan planForHost(EntityManager em, String hostUuid, String effectivePolicy,
            MemoryStateVO state) {
        HostPolicyPlan plan = new HostPolicyPlan();
        JsonObject target = explicitlyManagedPolicy(em, hostUuid, effectivePolicy);
        String targetText = target.toString();
        plan.targetHash = digest(targetText);
        String applied = state.getAppliedPolicy() == null ? "{}" : state.getAppliedPolicy();
        plan.changedFields = MemoryPolicyAdmissionRules.changedFields(applied, targetText);
        ignoreAbsentDisabledMechanismDefaults(applied, target, plan.changedFields);
        Map<String, String> blocked = MemoryPolicyAdmissionRules.blockedFields(
                plan.changedFields, state, System.currentTimeMillis());

        JsonObject payload = new JsonObject();
        payload.addProperty("schemaVersion", 1);
        JsonObject blockers = new JsonObject();
        for (Map.Entry<String, String> item : new TreeMap<>(plan.changedFields).entrySet()) {
            JsonElement value = jsonPath(target, item.getKey());
            String reason = blocked.get(item.getKey());
            if (reason != null) {
                JsonObject evidence = new JsonObject();
                evidence.add("desired", value == null ? JsonNull.INSTANCE : value.deepCopy());
                evidence.addProperty("reason", reason);
                blockers.add(item.getKey(), evidence);
            } else if (value != null) {
                setJsonPath(payload, item.getKey(), value.deepCopy());
            }
        }
        plan.hasPayload = payload.entrySet().size() > 1;
        plan.payload = plan.hasPayload ? payload.toString() : "{}";
        plan.payloadHash = plan.hasPayload ? digest(plan.payload) : null;
        plan.blockedFields = blockers.toString();
        plan.hasBlockers = blockers.size() > 0;
        plan.planHash = digest(plan.targetHash + "|" + String.valueOf(plan.payloadHash) + "|" + plan.blockedFields);
        return plan;
    }

    private static void ignoreAbsentDisabledMechanismDefaults(String appliedPolicy, JsonObject target,
            Map<String, String> changedFields) {
        JsonObject applied = new JsonParser().parse(appliedPolicy).getAsJsonObject();
        for (String field : Arrays.asList("zram.enabled", "writeback.enabled")) {
            if (!changedFields.containsKey(field) || jsonPath(target, field) != null) { continue; }
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
            if (state.getActiveTaskUuid() != null || isConfirmedPausedState(state.getState())) { return false; }
            if (!hasFreshCapabilityObservation(state, System.currentTimeMillis())) { return false; }
            if ("Unknown".equals(state.getStatus()) && hasUnresolvedControl(em, state)) { return false; }
            String effectivePolicy = effective(em, "Host", hostUuid);
            // A sparse/default policy has no authority over native legacy KSM.
            // Conversely an explicit false is intent too: the Agent must own
            // and enforce run=0 instead of silently preserving boot-enabled KSM.
            if (!hasExplicitMechanismPolicy(em, hostUuid)) { return false; }
            if (state.getAppliedPolicyHash() == null && hasHistoricalControl(state)) {
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
            if (plan.planHash.equals(state.getPolicyPlanHash())) { return false; }
            if (!plan.hasPayload || Objects.equals(plan.payloadHash, state.getAppliedPolicyHash())) {
                recordPolicyPlan(state, plan, plan.hasBlockers ? "Blocked" : "Applied");
                return false;
            }

            String actor = "system:memory-host-connect-policy";
            long revision = state.getDesiredRevision() + 1;
            APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
            msg.setScope("Host"); msg.setResourceUuid(hostUuid); msg.setAction("apply");
            MemoryTaskVO parent = task(msg, null, null, revision, "{}", actor);
            parent.setStatus("Queued"); em.persist(parent);
            bumpQueryVersion(em, "task");
            MemoryTaskVO child = task(msg, parent.getUuid(), hostUuid, revision, plan.payload, actor);
            child.setPolicyPlanHash(plan.planHash);
            em.persist(child);
            state.setDesiredRevision(revision); state.setActiveTaskUuid(child.getUuid());
            state.setControlOperationUuid(child.getUuid()); state.setPermitAuthorized(false);
            state.setStatus("Queued"); state.setReason(null);
            recordPolicyPlan(state, plan, "Queued");
            return true;
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

    private static boolean hasHistoricalControl(MemoryStateVO state) {
        return state.getDesiredRevision() > 0 || state.getAppliedRevision() != null
                || state.getControlOperationUuid() != null || state.getAppliedPolicy() != null;
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
                if (value == null) { continue; }
                if ("Global".equals(source.getScope()) && standardConfigs.globalIsSchemaDefault(em, field.path())) { continue; }
                return true;
            }
        }
        return false;
    }

    private static boolean isMaintenancePreparationAction(String action) {
        return "prepareWritebackBackend".equals(action) || "prepareZramPool".equals(action);
    }

    private List<String> shardTargets(APIUpdateMemoryPolicyMsg msg) {
        List<String> targets = msg.getTargetVmUuids() != null ? msg.getTargetVmUuids() : msg.getTargetHostUuids();
        if (targets == null) { throw error("MEMORY_TARGET_SNAPSHOT_INCOMPLETE", "Target shard is missing"); }
        return new ArrayList<>(targets);
    }

    private String shardKey(APIUpdateMemoryPolicyMsg msg, String actor) {
        return actor + ":" + msg.getClientRequestUuid() + ":" + msg.getScope() + ":" + msg.getResourceUuid();
    }

    private MemoryTaskInventory stagedInventory(APIUpdateMemoryPolicyMsg msg, String status, String reason) {
        MemoryTaskInventory result = new MemoryTaskInventory();
        result.setUuid(msg.getClientRequestUuid()); result.setScope(msg.getScope());
        result.setResourceUuid(msg.getResourceUuid()); result.setAction(msg.getAction());
        result.setStatus(status); result.setReason(reason); return result;
    }

    private MemoryTaskInventory stageTargetShard(APIUpdateMemoryPolicyMsg msg, String actor) {
        if (msg.getTargetShardIndex() == null || msg.getTargetShardCount() == null ||
                msg.getTargetTotalCount() == null || msg.getTargetDigest() == null ||
                msg.getTargetSnapshotGeneration() == null || msg.getExpectedRevision() < 0 ||
                msg.getTargetShardCount() <= 0 || msg.getTargetShardIndex() < 0 ||
                msg.getTargetShardIndex() >= msg.getTargetShardCount()) {
            throw error("MEMORY_TARGET_SNAPSHOT_INCOMPLETE", "Shard metadata is required");
        }
        List<String> targets = shardTargets(msg);
        if (targets.isEmpty()) {
            throw error("MEMORY_TARGET_SNAPSHOT_INCOMPLETE", "Target shard must not be empty");
        }
        if (new HashSet<>(targets).size() != targets.size()) {
            throw error("MEMORY_TARGET_SNAPSHOT_CONFLICT", "Target shard contains duplicates");
        }
        String key = shardKey(msg, actor);
        String hash = digest(MemoryPolicyRules.canonicalRequest(Arrays.asList(
                msg.getScope(), msg.getResourceUuid(), msg.getExpectedRevision(),
                msg.getTargetSnapshotGeneration(), msg.getTargetShardIndex(),
                msg.getTargetShardCount(), msg.getTargetTotalCount(), msg.getTargetDigest(), targets)));
        return transaction(em -> {
            lock(em); validateResource(em, msg.getScope(), msg.getResourceUuid());
            expireTargetShards(em, System.currentTimeMillis());
            List<MemoryTargetShardVO> same = em.createQuery("from MemoryTargetShardVO s where s.requestKey = :key and s.shardIndex = :idx", MemoryTargetShardVO.class)
                    .setParameter("key", key).setParameter("idx", msg.getTargetShardIndex()).getResultList();
            if (!same.isEmpty()) {
                if (!hash.equals(same.get(0).getRequestHash())) {
                    throw error("MEMORY_TARGET_SNAPSHOT_CONFLICT", "Shard replay differs from the staged request");
                }
                return stagedInventory(msg, "Staged", "idempotent shard replay");
            }
            MemoryTargetShardVO row = new MemoryTargetShardVO();
            row.setUuid(UUID.randomUUID().toString().replace("-", "")); row.setRequestKey(key);
            row.setScope(msg.getScope()); row.setResourceUuid(msg.getResourceUuid());
            row.setShardIndex(msg.getTargetShardIndex()); row.setShardCount(msg.getTargetShardCount());
            row.setTotalCount(msg.getTargetTotalCount()); row.setDigest(msg.getTargetDigest());
            row.setSnapshotGeneration(msg.getTargetSnapshotGeneration()); row.setExpectedRevision(msg.getExpectedRevision());
            row.setTargets(JSONObjectUtil.toJsonString(targets)); row.setRequestHash(hash);
            row.setExpiresAt(System.currentTimeMillis() + TARGET_SHARD_TTL_MS); em.persist(row); em.flush();
            return stagedInventory(msg, "Staged", "target shard accepted");
        });
    }

    private MemoryTaskInventory cancelTargetShards(APIUpdateMemoryPolicyMsg msg, String actor) {
        String key = shardKey(msg, actor);
        return transaction(em -> {
            lock(em); List<MemoryTargetShardVO> rows = em.createQuery("from MemoryTargetShardVO s where s.requestKey = :key", MemoryTargetShardVO.class)
                    .setParameter("key", key).getResultList();
            validateResource(em, msg.getScope(), msg.getResourceUuid());
            if (rows.stream().anyMatch(row -> row.getExpectedRevision() != msg.getExpectedRevision())) {
                throw error("MEMORY_TARGET_SNAPSHOT_CONFLICT", "Cancel differs from staged revision");
            }
            rows.forEach(em::remove); em.flush();
            return stagedInventory(msg, "Cancelled", "target staging cancelled");
        });
    }

    private MemoryTaskInventory commitTargetShards(APIUpdateMemoryPolicyMsg msg, String actor) {
        if (msg.getTargetSnapshotGeneration() == null || msg.getTargetDigest() == null
                || msg.getTargetTotalCount() == null || msg.getTargetShardCount() == null) {
            throw error("MEMORY_TARGET_SNAPSHOT_INCOMPLETE", "Complete snapshot metadata is required at commit");
        }
        String commitHash = digest(MemoryPolicyRules.canonicalRequest(Arrays.asList(
                msg.getScope(), msg.getResourceUuid(), msg.getExpectedRevision(), msg.getExpectedGlobalRevision(),
                msg.getExpectedSourceRevisions(), msg.getTargetSnapshotGeneration(), msg.getTargetDigest(),
                msg.getTargetTotalCount(), msg.getTargetShardCount(), msg.getTargetHostUuids(),
                JSONObjectUtil.toObject(msg.getPolicy(), Map.class))));
        MemoryTaskInventory previous = transaction(em -> {
            lock(em);
            validateResource(em, msg.getScope(), msg.getResourceUuid());
            List<MemoryTaskVO> rows = em.createQuery("from MemoryTaskVO t where t.requestKey = :key", MemoryTaskVO.class)
                    .setParameter("key", actor + ":" + msg.getClientRequestUuid()).getResultList();
            if (rows.isEmpty()) { return null; }
            if (!commitHash.equals(rows.get(0).getTargetSnapshotHash())) {
                throw error("MEMORY_IDEMPOTENCY_CONFLICT", "Target commit UUID was already used with different parameters");
            }
            return rows.get(0).toInventory();
        });
        if (previous != null) { return previous; }
        String key = shardKey(msg, actor);
        List<String> complete = transaction(em -> {
            lock(em); validateResource(em, msg.getScope(), msg.getResourceUuid());
            expireTargetShards(em, System.currentTimeMillis());
            List<MemoryTargetShardVO> rows = em.createQuery("from MemoryTargetShardVO s where s.requestKey = :key order by s.shardIndex", MemoryTargetShardVO.class)
                    .setParameter("key", key).getResultList();
            if (rows.isEmpty()) { throw error("MEMORY_TARGET_SNAPSHOT_INCOMPLETE", "No target shards staged"); }
            Map<Integer, List<String>> parts = new LinkedHashMap<>(); MemoryTargetShardVO first = rows.get(0);
            if (msg.getExpectedRevision() != first.getExpectedRevision()
                    || !msg.getTargetDigest().equals(first.getDigest())
                    || msg.getTargetTotalCount() != first.getTotalCount()
                    || msg.getTargetShardCount() != first.getShardCount()) {
                throw error("MEMORY_TARGET_SNAPSHOT_CONFLICT", "Commit differs from staged revision, count, or digest");
            }
            for (MemoryTargetShardVO row : rows) {
                if (row.getExpectedRevision() != first.getExpectedRevision() || row.getShardCount() != first.getShardCount()
                        || row.getTotalCount() != first.getTotalCount() || !row.getDigest().equals(first.getDigest())
                        || !row.getSnapshotGeneration().equals(first.getSnapshotGeneration())) {
                    throw error("MEMORY_TARGET_SNAPSHOT_CONFLICT", "Target shard metadata differs");
                }
                parts.put(row.getShardIndex(), JSONObjectUtil.toObject(row.getTargets(), List.class));
            }
            return MemoryTargetShardAssembler.assemble(first.getSnapshotGeneration(), msg.getTargetSnapshotGeneration(),
                    (int) first.getExpectedRevision(), first.getTotalCount(), first.getDigest(), parts, first.getShardCount());
        });
        String nextPolicy = policyWithSelectedVms(msg.getPolicy(), complete);
        String originalPolicy = msg.getPolicy(); List<String> originalTargets = msg.getTargetVmUuids();
        MemoryTaskInventory result;
        try {
            msg.setPolicy(nextPolicy); msg.setTargetVmUuids(null); msg.setAction("apply");
            result = submit(msg, commitHash);
        } finally {
            msg.setPolicy(originalPolicy); msg.setTargetVmUuids(originalTargets); msg.setAction("commitTargetShards");
        }
        transaction(em -> { em.createQuery("delete from MemoryTargetShardVO s where s.requestKey = :key").setParameter("key", key).executeUpdate(); return null; });
        return result;
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

    private static String policyWithSelectedVms(String policy, List<String> targets) {
        JsonObject root = new JsonParser().parse(policy == null ? "{}" : policy).getAsJsonObject();
        JsonObject zram = root.has("zram") && root.get("zram").isJsonObject() ? root.getAsJsonObject("zram") : new JsonObject();
        JsonArray selected = new JsonArray(); targets.forEach(selected::add); zram.add("selectedVmUuids", selected); zram.addProperty("selectionMode", "list"); root.add("zram", zram);
        return root.toString();
    }

    private void expireTargetShards(EntityManager em, long now) {
        em.createQuery("delete from MemoryTargetShardVO s where s.expiresAt < :now").setParameter("now", now).executeUpdate();
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
            if (!list.isEmpty()) { bumpQueryVersion(em, "task"); }
            for (MemoryTaskVO task : list) {
                task.setStatus("Applying"); task.setOwnerManagementNodeUuid(owner);
                MemoryTaskVO parent = em.find(MemoryTaskVO.class, task.getParentUuid());
                if (parent != null) { parent.setStatus("Applying"); }
            }
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
        transaction(em -> {
            lock(em);
            String resultStatus = status;
            String resultReason = reason;
            MemoryTaskVO task = em.find(MemoryTaskVO.class, uuid);
            MemoryStateVO host = task == null ? null : em.find(MemoryStateVO.class, task.getHostUuid());
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
            MemoryTaskVO recovery = uncertainRecoveryForResult(em, task);
            if (recovery != null) {
                Map<?, ?> request = JSONObjectUtil.toObject(recovery.getPolicy(), Map.class);
                MemoryTaskVO original = em.find(MemoryTaskVO.class, recovery.getExpectedControlOperationUuid());
                MemoryTaskVO drain = em.find(MemoryTaskVO.class, request.get("drainControlOperationUuid"));
                if (host != null && Objects.equals(uuid, host.getActiveTaskUuid())
                        && MemoryUncertainRecoveryRules.confirms(recovery, original, drain, host, response)) {
                    resultStatus = "Succeeded";
                    resultReason = "Uncertain activation retired; original outcome remains Unknown";
                    restoredControlFence = drain.getUuid();
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
            if (task == null || !MemoryTaskRules.canTransition(task.getStatus(), resultStatus)) { return null; }
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
                    } else { updateState(host, response, isMonitoringSample(response)); }
                }
                if ("Succeeded".equals(resultStatus) && !"VM".equals(task.getScope())
                        && Arrays.asList("apply", "clearOverride").contains(task.getAction())) {
                    host.setAppliedPolicyHash(digest(task.getPolicy()));
                    String previousApplied = host.getAppliedPolicy() == null ? "{}" : host.getAppliedPolicy();
                    host.setAppliedPolicy(MemoryPolicyRules.merge(previousApplied, task.getPolicy(), "Host"));
                    resolveLegacyPolicyReviewAfterApply(em, task, host);
                }
                if (task.getPolicyPlanHash() != null
                        && task.getPolicyPlanHash().equals(host.getPolicyPlanHash())) {
                    String planStatus = "Succeeded".equals(resultStatus)
                            ? (host.getPolicyBlockedFields() != null
                                && !"{}".equals(host.getPolicyBlockedFields()) ? "Blocked" : "Applied")
                            : status;
                    host.setPolicyPlanStatus(planStatus);
                }
                host.setStatus(resultStatus); host.setReason(trim(resultReason));
                if (!MemoryTaskRules.blocksHost(resultStatus)) { host.setActiveTaskUuid(null); }
                if (restoredUncertainOwner != null) {
                    // A known rejection of the NEW recovery does not release
                    // the OLD unresolved execution or refresh its native facts.
                    host.setActiveTaskUuid(restoredUncertainOwner.getUuid());
                    host.setStatus("Unknown"); host.setReason(restoredUncertainOwner.getReason());
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
            if (host.getActiveTaskUuid() == null) { host.setStatus(response.status == null ? "Unknown" : response.status); }
            return null;
        });
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
        if (state == null || state.getLastSampleTime() == null || state.getLastSampleTime() > now + 5000
                || now - state.getLastSampleTime() >= MemoryOptimizationGlobalConfig.displayTtlMillis()) {
            return false;
        }
        try {
            Map<?, ?> root = JSONObjectUtil.toObject(state.getState(), Map.class);
            Object lastConfirmed = root.get("lastConfirmedOperationUuid");
            Object bootId = root.get("bootId");
            Object lifecycleValue = root.get("lifecycle");
            if (!Objects.equals(controlUuid, lastConfirmed) || !(bootId instanceof String)
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
            return new MemoryQueryPage<>(page.setFirstResult(start).setMaxResults(pageSize).getResultList(), total, snapshot);
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

            // Snapshot commits have a separate replay contract keyed by targetSnapshotHash.
            // Until that receipt is modeled in the tombstone, retain these roots.
            if (root.getTargetSnapshotHash() != null) {
                throw error("MEMORY_TASK_REFERENCED", "Task tree " + taskUuid
                        + " retains target snapshot commit replay evidence");
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
            Long bootstrapReferences = em.createQuery("select count(b) from MemoryCloudBootstrapVO b where b.taskUuid in :ids", Long.class)
                    .setParameter("ids", ids).getSingleResult();
            if (stateReferences != 0 || stateJsonReference || taskReferences != 0
                    || migrationReferences != 0 || bootstrapReferences != 0) {
                throw error("MEMORY_TASK_REFERENCED", "Task tree " + taskUuid
                        + " is still referenced by Host state, another task, migration, or bootstrap state");
            }

            // A staging row with the same request key is still part of an in-flight target commit.
            if (root.getRequestKey() != null) {
                Long shardReferences = em.createQuery("select count(s) from MemoryTargetShardVO s where s.requestKey = :key", Long.class)
                        .setParameter("key", root.getRequestKey()).getSingleResult();
                if (shardReferences != 0) {
                    throw error("MEMORY_TASK_REFERENCED", "Task tree " + taskUuid + " still has target snapshot staging rows");
                }
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

    public long taskCount(APIQueryMemoryTaskMsg msg) {
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

    public List<MemoryTaskInventory> tasks(APIQueryMemoryTaskMsg msg) {
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

    MemoryQueryPage<MemoryTaskInventory> taskPage(APIQueryMemoryTaskMsg msg) {
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

    private static void bindTaskFilter(TypedQuery<?> query, APIQueryMemoryTaskMsg msg) {
        if (msg.getUuid() != null) { query.setParameter("uuid", msg.getUuid()); }
        if (msg.getHostUuid() != null) { query.setParameter("host", msg.getHostUuid()); }
        if (msg.getStatus() != null) { query.setParameter("status", msg.getStatus()); }
    }

    public MemoryTaskInventory cancel(String uuid) {
        return transaction(em -> {
            lock(em);
            MemoryTaskVO task = em.find(MemoryTaskVO.class, uuid);
            if (task == null || !MemoryTaskRules.canCancel(task.getStatus())) {
                throw error("MEMORY_NOT_CANCELLABLE", "Only Queued tasks can be cancelled");
            }
            bumpQueryVersion(em, "task");
            List<MemoryTaskVO> children = task.getHostUuid() == null
                    ? em.createQuery("from MemoryTaskVO t where t.parentUuid = :parent", MemoryTaskVO.class)
                    .setParameter("parent", uuid).getResultList() : Collections.singletonList(task);
            for (MemoryTaskVO child : children) {
                if (!MemoryTaskRules.canCancel(child.getStatus())) { throw error("MEMORY_NOT_CANCELLABLE", "A child already started"); }
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
