package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.*;

/**
 * Same-transaction reader/writer for standard memory config values.
 * Transaction ownership belongs to the caller.
 */
public final class MemoryStandardConfigAdapter {
    /** Stored literal meaning this scope contributes no feature-switch override. */
    public enum OverrideMode { UNMANAGED }

    public static boolean isUnmanaged(Object value) { return value == OverrideMode.UNMANAGED; }
    /** Value owned at exactly this resource level; null means inherit. */
    public Object readOverrideOwn(EntityManager em, String path, String resourceUuid) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        ResourceConfigVO row = resourceRow(em, field, resourceUuid);
        return readResourceStored(field, row, resourceUuid);
    }

    private static Object readResourceStored(MemoryStandardField field, ResourceConfigVO row, String resourceUuid) {
        // An empty legacy row contributes no override; it must not block policy evaluation.
        if (row == null || row.getValue() == null || row.getValue().trim().isEmpty()) { return null; }
        return readStored(field, row.getValue());
    }

    /** Value owned by the GlobalConfig row, including its schema default. */
    public Object readGlobalOwn(EntityManager em, String path) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        GlobalConfigVO row = globalRow(em, field);
        return readGlobalStored(field, row);
    }

    private static Object readGlobalStored(MemoryStandardField field, GlobalConfigVO row) {
        String value = row == null ? field.config().getDefaultValue() : row.getValue();
        if (value == null || value.trim().isEmpty()) {
            value = row == null ? field.config().getDefaultValue() : row.getDefaultValue();
        }
        return readStored(field, value);
    }

    /** One scoped read in the caller's transaction; no process-wide config cache. */
    public Map<MemoryStandardField, Object> readOwn(EntityManager em, MemoryStandardField.Scope scope,
            String resourceUuid, Set<String> skippedPaths) {
        Set<MemoryStandardField> selected = EnumSet.noneOf(MemoryStandardField.class);
        Set<String> categories = new HashSet<>(), names = new HashSet<>();
        for (MemoryStandardField field : MemoryStandardField.values()) {
            if (field.allows(scope) && !skippedPaths.contains(field.path())) {
                selected.add(field); categories.add(field.category()); names.add(field.configName());
            }
        }
        Map<MemoryStandardField, Object> values = new EnumMap<>(MemoryStandardField.class);
        if (selected.isEmpty()) { return values; }
        if (scope == MemoryStandardField.Scope.GLOBAL) {
            List<GlobalConfigVO> rows = em.createQuery(
                    "select g from GlobalConfigVO g where g.category in :categories and g.name in :names", GlobalConfigVO.class)
                    .setParameter("categories", categories).setParameter("names", names).getResultList();
            for (GlobalConfigVO row : rows) {
                MemoryStandardField field = MemoryStandardField.forConfig(row.getCategory(), row.getName());
                if (!selected.contains(field)) { continue; }
                if (values.containsKey(field)) { throw new javax.persistence.NonUniqueResultException(field.path()); }
                values.put(field, readGlobalStored(field, row));
            }
            for (MemoryStandardField field : selected) {
                if (!values.containsKey(field)) { values.put(field, readGlobalStored(field, null)); }
            }
        } else {
            List<ResourceConfigVO> rows = em.createQuery(
                    "select r from ResourceConfigVO r where r.category in :categories and r.name in :names "
                            + "and r.resourceUuid = :resourceUuid", ResourceConfigVO.class)
                    .setParameter("categories", categories).setParameter("names", names)
                    .setParameter("resourceUuid", resourceUuid).getResultList();
            for (ResourceConfigVO row : rows) {
                MemoryStandardField field = MemoryStandardField.forConfig(row.getCategory(), row.getName());
                if (!selected.contains(field)) { continue; }
                if (values.containsKey(field)) { throw new javax.persistence.NonUniqueResultException(field.path()); }
                values.put(field, readResourceStored(field, row, resourceUuid));
            }
        }
        return values;
    }

    private static Object readStored(MemoryStandardField field, String value) {
        if (field.isFeatureSwitch() && value != null
                && "none".equalsIgnoreCase(value.trim())) {
            return OverrideMode.UNMANAGED;
        }
        // GlobalConfig schema defaults may be empty for optional fields. Resource
        // overrides are checked for blank storage by readOverrideOwn above.
        return MemoryStandardConfigCodec.decode(field.path(), value);
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
        // none over a different default, it remains an explicit stored none rather than a reset to the default.
        return Objects.equals(storedValue, defaultValue);
    }

    /** Set/delete same-scope rows; caller owns cross-field validation and the enclosing transaction. */
    public void writeOverrides(EntityManager em, String resourceUuid, String resourceType, Map<String, ?> values) {
        Objects.requireNonNull(em, "EntityManager");
        if (resourceUuid == null || resourceUuid.trim().isEmpty()) { throw new IllegalArgumentException("resource UUID is required"); }
        MemoryStandardField.Scope scope = MemoryStandardField.scope(resourceType);
        if (scope == MemoryStandardField.Scope.GLOBAL) { throw new IllegalArgumentException("resource override requires Host or Cluster"); }
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            MemoryStandardField field = MemoryStandardField.forPath(entry.getKey());
            requireScope(field, scope);
            String encoded = MemoryStandardConfigCodec.encode(field.path(), entry.getValue());
            ResourceConfigVO row = resourceRow(em, field, resourceUuid);
            if (encoded == null) {
                if (row != null) { em.remove(row); }
            } else if (row == null) {
                row = new ResourceConfigVO();
                row.setUuid(UUID.randomUUID().toString().replace("-", ""));
                row.setCategory(field.category()); row.setName(field.configName()); row.setValue(encoded);
                row.setDescription(field.config().getDescription());
                row.setResourceUuid(resourceUuid); row.setResourceType(resourceType);
                em.persist(row);
            } else {
                row.setValue(encoded);
                row.setResourceType(resourceType);
                em.merge(row);
            }
        }
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
