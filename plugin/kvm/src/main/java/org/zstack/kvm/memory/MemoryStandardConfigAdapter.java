package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.*;

/**
 * Same-transaction persistence adapter for standard memory config values.
 * Transaction ownership belongs to the caller. No cache refresh, event, or
 * external action is performed here; callers refresh returned configs after commit.
 */
public final class MemoryStandardConfigAdapter {
    /** Stored legacy literal meaning this scope leaves native KSM unmanaged. */
    public enum OverrideMode { UNMANAGED }

    public static boolean isUnmanaged(Object value) { return value == OverrideMode.UNMANAGED; }
    public static final class ChangeSet {
        private final Set<GlobalConfig> refreshAfterCommit;
        private final List<ResourceChange> resourceChanges;
        private ChangeSet(Set<GlobalConfig> configs, List<ResourceChange> resourceChanges) {
            this.refreshAfterCommit = Collections.unmodifiableSet(new LinkedHashSet<>(configs));
            this.resourceChanges = Collections.unmodifiableList(new ArrayList<>(resourceChanges));
        }
        public Set<GlobalConfig> refreshAfterCommit() { return refreshAfterCommit; }
        public List<ResourceChange> resourceChanges() { return resourceChanges; }
    }

    public static final class ResourceChange {
        private final GlobalConfig config;
        private final String resourceUuid, resourceType, oldValue, newValue;
        private final boolean deleted;
        private ResourceChange(GlobalConfig config, String resourceUuid, String resourceType,
                               String oldValue, String newValue, boolean deleted) {
            this.config = config; this.resourceUuid = resourceUuid; this.resourceType = resourceType;
            this.oldValue = oldValue; this.newValue = newValue; this.deleted = deleted;
        }
        public GlobalConfig config() { return config; }
        public String resourceUuid() { return resourceUuid; }
        public String resourceType() { return resourceType; }
        public String oldValue() { return oldValue; }
        public String newValue() { return newValue; }
        public boolean deleted() { return deleted; }
    }

    public Object readEffective(EntityManager em, String path, List<String> resourceUuidsMostSpecificFirst) {
        Objects.requireNonNull(em, "EntityManager");
        Objects.requireNonNull(resourceUuidsMostSpecificFirst, "resourceUuidsMostSpecificFirst");
        MemoryStandardField field = MemoryStandardField.forPath(path);
        for (String uuid : resourceUuidsMostSpecificFirst) {
            if (uuid == null || uuid.trim().isEmpty()) { throw new IllegalArgumentException("resource UUID is required"); }
            Object override = readOverrideOwn(em, field.path(), uuid);
            if (isUnmanaged(override)) { return override; }
            if (override != null) { return override; }
        }
        GlobalConfigVO global = globalRow(em, field);
        String value = global == null ? null : global.getValue();
        if (value == null || value.trim().isEmpty()) { value = global == null ? field.config().getDefaultValue() : global.getDefaultValue(); }
        return readStored(field, value);
    }

    /** Value owned at exactly this resource level; null means inherit. */
    public Object readOverrideOwn(EntityManager em, String path, String resourceUuid) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        ResourceConfigVO row = resourceRow(em, field, resourceUuid);
        if (row == null) { return null; }
        String value = row.getValue();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("stored resource memory config is empty for " + field.path()
                    + " at " + resourceUuid + "; refusing to inherit until the invalid row is repaired or deleted");
        }
        return readStored(field, value);
    }

    /** Value owned by the GlobalConfig row, including its schema default. */
    public Object readGlobalOwn(EntityManager em, String path) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        GlobalConfigVO row = globalRow(em, field);
        String value = row == null ? field.config().getDefaultValue() : row.getValue();
        if (value == null || value.trim().isEmpty()) {
            value = row == null ? field.config().getDefaultValue() : row.getDefaultValue();
        }
        return readStored(field, value);
    }

    private static Object readStored(MemoryStandardField field, String value) {
        if (field == MemoryStandardField.KSM_ENABLED && value != null
                && "none".equalsIgnoreCase(value.trim())) {
            return OverrideMode.UNMANAGED;
        }
        // GlobalConfig schema defaults may be empty for optional fields. Resource
        // overrides are checked for blank storage by readOverrideOwn above.
        return MemoryStandardConfigCodec.decode(field.path(), value);
    }

    public String globalStoredValue(EntityManager em, String path) {
        GlobalConfigVO row = globalRow(em, MemoryStandardField.forPath(path));
        return row == null ? null : row.getValue();
    }

    public String overrideStoredValue(EntityManager em, String path, String resourceUuid) {
        ResourceConfigVO row = resourceRow(em, MemoryStandardField.forPath(path), resourceUuid);
        return row == null ? null : row.getValue();
    }

    public boolean hasOverrideRow(EntityManager em, String path, String resourceUuid) {
        return resourceRow(em, MemoryStandardField.forPath(path), resourceUuid) != null;
    }

    public boolean globalIsSchemaDefault(EntityManager em, String path) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        GlobalConfigVO row = globalRow(em, field);
        if (row == null) { return true; }
        String defaultValue = row.getDefaultValue();
        String storedValue = row.getValue();
        // A literal `none` is only the schema default when it actually equals
        // this row's declared default. If an administrator explicitly stored
        // none over a different default, it is an unmanaged override and must
        // be visible as such in fieldModes.
        return Objects.equals(storedValue, defaultValue);
    }

    /** Set/delete one Host or Cluster override. Null removes the row to reveal the parent. */
    public ChangeSet writeOverride(EntityManager em, String resourceUuid, String resourceType, String path, Object value) {
        Map<String, Object> one = new LinkedHashMap<>(); one.put(path, value);
        return writeOverrides(em, resourceUuid, resourceType, one);
    }

    /** Set/delete same-scope rows; caller owns cross-field validation and the enclosing transaction. */
    public ChangeSet writeOverrides(EntityManager em, String resourceUuid, String resourceType, Map<String, ?> values) {
        Objects.requireNonNull(em, "EntityManager");
        if (resourceUuid == null || resourceUuid.trim().isEmpty()) { throw new IllegalArgumentException("resource UUID is required"); }
        MemoryStandardField.Scope scope = MemoryStandardField.scope(resourceType);
        if (scope == MemoryStandardField.Scope.GLOBAL) { throw new IllegalArgumentException("resource override requires Host or Cluster"); }
        LinkedHashSet<GlobalConfig> refresh = new LinkedHashSet<>();
        List<ResourceChange> resourceChanges = new ArrayList<>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            MemoryStandardField field = MemoryStandardField.forPath(entry.getKey());
            requireScope(field, scope);
            String encoded = MemoryStandardConfigCodec.encode(field.path(), entry.getValue());
            ResourceConfigVO row = resourceRow(em, field, resourceUuid);
            Object previous;
            try { previous = readEffective(em, field.path(), Collections.singletonList(resourceUuid)); }
            catch (IllegalArgumentException invalidStoredValue) {
                if (encoded != null) { throw invalidStoredValue; }
                previous = null; // Explicit delete is the supported repair path for an invalid row.
            }
            String oldValue = previous == null ? field.config().getDefaultValue() : String.valueOf(previous);
            if (encoded == null) {
                if (row != null) {
                    em.remove(row);
                    resourceChanges.add(new ResourceChange(field.config(), resourceUuid, resourceType, oldValue,
                            field.config().value(), true));
                }
            } else if (row == null) {
                row = new ResourceConfigVO();
                row.setUuid(UUID.randomUUID().toString().replace("-", ""));
                row.setCategory(field.category()); row.setName(field.configName()); row.setValue(encoded);
                row.setDescription(field.config().getDescription());
                row.setResourceUuid(resourceUuid); row.setResourceType(resourceType);
                em.persist(row);
                resourceChanges.add(new ResourceChange(field.config(), resourceUuid, resourceType, oldValue, encoded, false));
            } else {
                String original = row.getValue();
                row.setValue(encoded);
                row.setResourceType(resourceType);
                em.merge(row);
                if (!Objects.equals(original, encoded)) {
                    resourceChanges.add(new ResourceChange(field.config(), resourceUuid, resourceType, oldValue, encoded, false));
                }
            }
        }
        return new ChangeSet(refresh, resourceChanges);
    }

    /** Global-level changes require the ordinary GlobalConfig row to have been initialized. */
    public ChangeSet writeGlobal(EntityManager em, String path, Object value) {
        Objects.requireNonNull(em, "EntityManager");
        MemoryStandardField field = MemoryStandardField.forPath(path);
        requireScope(field, MemoryStandardField.Scope.GLOBAL);
        GlobalConfigVO row = globalRow(em, field);
        if (row == null) { throw new IllegalStateException("standard GlobalConfig row is not initialized for " + path); }
        String encoded = MemoryStandardConfigCodec.encode(path, value);
        row.setValue(encoded == null ? row.getDefaultValue() : encoded);
        em.merge(row);
        return new ChangeSet(Collections.singleton(field.config()), Collections.<ResourceChange>emptyList());
    }

    private static void requireScope(MemoryStandardField field, MemoryStandardField.Scope scope) {
        if (!field.allows(scope)) { throw new IllegalArgumentException("field " + field.path() + " does not allow " + scope + " scope"); }
    }

    private static ResourceConfigVO resourceRow(EntityManager em, MemoryStandardField field, String resourceUuid) {
        try {
            return em.createQuery("select r from ResourceConfigVO r where r.category = :category and r.name = :name and r.resourceUuid = :resourceUuid", ResourceConfigVO.class)
                    .setParameter("category", field.category()).setParameter("name", field.configName())
                    .setParameter("resourceUuid", resourceUuid).getSingleResult();
        } catch (NoResultException e) { return null; }
    }

    private static GlobalConfigVO globalRow(EntityManager em, MemoryStandardField field) {
        try {
            return em.createQuery("select g from GlobalConfigVO g where g.category = :category and g.name = :name", GlobalConfigVO.class)
                    .setParameter("category", field.category()).setParameter("name", field.configName()).getSingleResult();
        } catch (NoResultException e) { return null; }
    }
}
