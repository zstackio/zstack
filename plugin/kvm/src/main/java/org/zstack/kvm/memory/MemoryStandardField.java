package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.kvm.KVMGlobalConfig;

import java.math.BigDecimal;
import java.util.*;

/**
 * Explicit ownership map between ordinary scalar policy fields and standard config identities.
 * Selection mode plus selectedVmUuids remain the dedicated, atomic list policy;
 * VM participation and migration/restore lifecycle state are also specialized.
 */
public enum MemoryStandardField {
    KSM_ENABLED("ksm.enabled", KVMGlobalConfig.HOST_KSM, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    KSM_ZERO_PAGES("ksm.zeroPagesEnabled", MemoryStandardGlobalConfig.KSM_ZERO_PAGES, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_ENABLED("zram.enabled", MemoryStandardGlobalConfig.ZRAM_ENABLED, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_LOGICAL_CAPACITY("zram.logicalCapacityBytes", MemoryStandardGlobalConfig.ZRAM_LOGICAL_CAPACITY, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_RAM_LIMIT("zram.ramLimitBytes", MemoryStandardGlobalConfig.ZRAM_RAM_LIMIT, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_ENABLED("writeback.enabled", MemoryStandardGlobalConfig.WRITEBACK_ENABLED, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_BACKEND_RESOURCE("writeback.backendResourceUuid", MemoryStandardGlobalConfig.WRITEBACK_BACKEND_RESOURCE, Kind.STRING, Scope.HOST),
    // Backend identity and its capacity are Host-owned.  Global/Cluster may
    // express only the independent writeback intent.
    WRITEBACK_BACKEND_CAPACITY("writeback.backendCapacityBytes", MemoryStandardGlobalConfig.WRITEBACK_BACKEND_CAPACITY, Kind.LONG, Scope.HOST);

    public enum Kind { BOOLEAN, LONG, STRING }
    public enum Scope { GLOBAL, CLUSTER, HOST }
    private final String path;
    private final GlobalConfig config;
    private final Kind kind;
    private final EnumSet<Scope> scopes;
    private static final Map<String, MemoryStandardField> BY_PATH;

    static {
        Map<String, MemoryStandardField> fields = new HashMap<>();
        for (MemoryStandardField field : values()) {
            if (fields.put(field.path, field) != null) { throw new IllegalStateException("duplicate memory field " + field.path); }
        }
        BY_PATH = Collections.unmodifiableMap(fields);
    }

    MemoryStandardField(String path, GlobalConfig config, Kind kind, Scope... scopes) {
        this.path = path; this.config = config; this.kind = kind;
        this.scopes = scopes.length == 0 ? EnumSet.noneOf(Scope.class) : EnumSet.copyOf(Arrays.asList(scopes));
    }

    public String path() { return path; }
    public GlobalConfig config() { return config; }
    public String category() { return config.getCategory(); }
    public String configName() { return config.getName(); }
    public Kind kind() { return kind; }
    boolean isFeatureSwitch() {
        return this == KSM_ENABLED || this == KSM_ZERO_PAGES
                || this == ZRAM_ENABLED || this == WRITEBACK_ENABLED;
    }
    public boolean allows(Scope scope) { return scopes.contains(scope); }
    public Set<Scope> scopes() { return Collections.unmodifiableSet(scopes); }
    public static MemoryStandardField forPath(String path) {
        MemoryStandardField field = BY_PATH.get(path);
        if (field == null) { throw new IllegalArgumentException("field is not standard-backed: " + path); }
        return field;
    }

    /** Null denotes an unrelated standard configuration, not an invalid memory field. */
    public static MemoryStandardField forConfig(String category, String name) {
        for (MemoryStandardField field : values()) {
            if (field.category().equals(category) && field.configName().equals(name)) { return field; }
        }
        return null;
    }

    public static Scope scope(String resourceType) {
        if (HostVO.class.getSimpleName().equals(resourceType)) { return Scope.HOST; }
        if (ClusterVO.class.getSimpleName().equals(resourceType)) { return Scope.CLUSTER; }
        throw new IllegalArgumentException("unsupported memory config resource type: " + resourceType);
    }

    Object normalize(Object value) {
        if (value == null) { return null; }
        switch (kind) {
            case BOOLEAN:
                if (!(value instanceof Boolean)) { throw incompatible(value); }
                return value;
            case LONG:
                if (!(value instanceof Number)) { throw incompatible(value); }
                try { return new BigDecimal(value.toString()).longValueExact(); }
                catch (ArithmeticException | NumberFormatException e) { throw incompatible(value); }
            case STRING:
                if (!(value instanceof String)) { throw incompatible(value); }
                String text = (String) value;
                if ("writeback.backendResourceUuid".equals(path)
                        && !text.matches("(?i)[0-9a-f]{32}|[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
                    throw new IllegalArgumentException("writeback.backendResourceUuid must be a UUID");
                }
                if (text.length() > 128) { throw new IllegalArgumentException("memory config string exceeds 128 characters"); }
                return value;
            default: throw new IllegalStateException("unknown type " + kind);
        }
    }

    private IllegalArgumentException incompatible(Object value) {
        return new IllegalArgumentException("field " + path + " requires " + kind + ", got " + value.getClass().getSimpleName());
    }
}
