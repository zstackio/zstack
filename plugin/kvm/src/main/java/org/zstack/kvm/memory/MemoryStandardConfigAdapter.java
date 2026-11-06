package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.resourceconfig.ResourceConfigVO;

import org.zstack.core.db.Q;
import org.zstack.resourceconfig.ResourceConfigVO_;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

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

    /** The values each resource owns for every field, in one query; a missing field means inherit. */
    public Map<String, Map<MemoryStandardField, Object>> readOverridesOwn(EntityManager em, Collection<String> resourceUuids) {
        Map<String, MemoryStandardField> byName = new HashMap<>();
        for (MemoryStandardField field : MemoryStandardField.values()) { byName.put(field.category() + "/" + field.configName(), field); }
        Set<String> names = Arrays.stream(MemoryStandardField.values()).map(MemoryStandardField::configName).collect(Collectors.toSet());
        Map<String, Map<MemoryStandardField, Object>> result = new HashMap<>();
        List<ResourceConfigVO> rows = Q.New(ResourceConfigVO.class).in(ResourceConfigVO_.resourceUuid, resourceUuids)
                .in(ResourceConfigVO_.name, names).list();
        for (ResourceConfigVO row : rows) {
            MemoryStandardField field = byName.get(row.getCategory() + "/" + row.getName());
            Object value = field == null ? null : readResourceStored(field, row, row.getResourceUuid());
            if (value != null) { result.computeIfAbsent(row.getResourceUuid(), k -> new EnumMap<>(MemoryStandardField.class)).put(field, value); }
        }
        return result;
    }

    /** Value of the GlobalConfig as the management node holds it, including its schema default. */
    public Object readGlobalOwn(GlobalConfigFacade gcf, MemoryStandardField field) {
        GlobalConfig live = gcf.getAllConfig().get(GlobalConfig.produceIdentity(field.category(), field.configName()));
        String value = live == null ? null : live.value();
        if (value == null || value.trim().isEmpty()) {
            value = live == null ? field.config().getDefaultValue() : live.getDefaultValue();
        }
        return readStored(field, value);
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

    /** Set/delete same-scope rows; caller owns cross-field validation and the enclosing transaction. */
    public void writeOverrides(EntityManager em, String resourceUuid, String resourceType, Map<String, ?> values) {
        Objects.requireNonNull(em, "EntityManager");
        if (resourceUuid == null || resourceUuid.trim().isEmpty()) { throw new IllegalArgumentException("resource UUID is required"); }
        MemoryStandardField.Scope scope = MemoryStandardField.scope(resourceType);
        if (scope == MemoryStandardField.Scope.GLOBAL) { throw new IllegalArgumentException("resource override requires Host or Cluster"); }
        // The resource's rows of these categories are read once, not per field.
        Map<String, ResourceConfigVO> rows = new HashMap<>();
        Set<String> categories = values.keySet().stream().map(path -> MemoryStandardField.forPath(path).category())
                .collect(Collectors.toSet());
        if (!categories.isEmpty()) {
            for (ResourceConfigVO row : Q.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                    .in(ResourceConfigVO_.category, categories).<ResourceConfigVO>list()) {
                rows.put(row.getCategory() + "/" + row.getName(), row);
            }
        }
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            MemoryStandardField field = MemoryStandardField.forPath(entry.getKey());
            requireScope(field, scope);
            String encoded = MemoryStandardConfigCodec.encode(field.path(), entry.getValue());
            ResourceConfigVO row = rows.get(field.category() + "/" + field.configName());
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
        return Q.New(ResourceConfigVO.class).eq(ResourceConfigVO_.category, field.category())
                .eq(ResourceConfigVO_.name, field.configName()).eq(ResourceConfigVO_.resourceUuid, resourceUuid).find();
    }

}
