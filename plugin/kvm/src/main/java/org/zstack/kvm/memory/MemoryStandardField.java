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
    KSM_PAGES_TO_SCAN("ksm.pagesToScan", MemoryStandardGlobalConfig.KSM_PAGES_TO_SCAN, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    KSM_SLEEP_MILLIS("ksm.sleepMillis", MemoryStandardGlobalConfig.KSM_SLEEP_MILLIS, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_ENABLED("zram.enabled", MemoryStandardGlobalConfig.ZRAM_ENABLED, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_LOGICAL_CAPACITY("zram.logicalCapacityBytes", MemoryStandardGlobalConfig.ZRAM_LOGICAL_CAPACITY, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_RAM_LIMIT("zram.ramLimitBytes", MemoryStandardGlobalConfig.ZRAM_RAM_LIMIT, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_REQUIRED_AVAILABLE("zram.requiredAvailableBytes", MemoryStandardGlobalConfig.ZRAM_REQUIRED_AVAILABLE, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_HOST_FLOOR("zram.hostFloorBytes", MemoryStandardGlobalConfig.ZRAM_HOST_FLOOR, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_METADATA_RESERVE("zram.metadataReserveBytes", MemoryStandardGlobalConfig.ZRAM_METADATA_RESERVE, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_TRANSIENT_RESERVE("zram.transientReserveBytes", MemoryStandardGlobalConfig.ZRAM_TRANSIENT_RESERVE, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_CPU_THRESHOLD("zram.cpuThresholdPercent", MemoryStandardGlobalConfig.ZRAM_CPU_THRESHOLD, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_CPU_HOLD("zram.cpuHoldSeconds", MemoryStandardGlobalConfig.ZRAM_CPU_HOLD, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_OPERATION_INTERVAL("zram.operationIntervalSeconds", MemoryStandardGlobalConfig.ZRAM_OPERATION_INTERVAL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_DISCOVERY_INTERVAL("zram.discoveryIntervalSeconds", MemoryStandardGlobalConfig.ZRAM_DISCOVERY_INTERVAL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_DISCOVERY_TIMEOUT("zram.discoveryTimeoutSeconds", MemoryStandardGlobalConfig.ZRAM_DISCOVERY_TIMEOUT, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_DISCOVERY_TTL("zram.discoveryTtlSeconds", MemoryStandardGlobalConfig.ZRAM_DISCOVERY_TTL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_HOST_SAMPLE_INTERVAL("zram.hostSampleIntervalSeconds", MemoryStandardGlobalConfig.ZRAM_HOST_SAMPLE_INTERVAL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_HOST_SAMPLE_TTL("zram.hostSampleTtlSeconds", MemoryStandardGlobalConfig.ZRAM_HOST_SAMPLE_TTL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_VM_SAMPLE_INTERVAL("zram.vmSampleIntervalSeconds", MemoryStandardGlobalConfig.ZRAM_VM_SAMPLE_INTERVAL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_VM_SAMPLE_TTL("zram.vmSampleTtlSeconds", MemoryStandardGlobalConfig.ZRAM_VM_SAMPLE_TTL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_CPU_GUARD("zram.hostCpuGuardEnabled", MemoryStandardGlobalConfig.ZRAM_CPU_GUARD, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_MEMORY_PSI_GUARD("zram.hostMemoryPsiGuardEnabled", MemoryStandardGlobalConfig.ZRAM_MEMORY_PSI_GUARD, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_IO_PSI_GUARD("zram.hostIoPsiGuardEnabled", MemoryStandardGlobalConfig.ZRAM_IO_PSI_GUARD, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_HOST_CPU_THRESHOLD("zram.hostCpuThresholdPercent", MemoryStandardGlobalConfig.ZRAM_HOST_CPU_THRESHOLD, Kind.DECIMAL, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_HOST_MEMORY_PSI_THRESHOLD("zram.hostMemoryPsiThresholdPercent", MemoryStandardGlobalConfig.ZRAM_HOST_MEMORY_PSI_THRESHOLD, Kind.DECIMAL, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_HOST_IO_PSI_THRESHOLD("zram.hostIoPsiThresholdPercent", MemoryStandardGlobalConfig.ZRAM_HOST_IO_PSI_THRESHOLD, Kind.DECIMAL, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_RECLAIM_BATCH("zram.reclaimBatchBytes", MemoryStandardGlobalConfig.ZRAM_RECLAIM_BATCH, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_CONCURRENCY("zram.concurrency", MemoryStandardGlobalConfig.ZRAM_CONCURRENCY, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_TIMEOUT_ISOLATION_SLOTS("zram.timeoutIsolationSlots", MemoryStandardGlobalConfig.ZRAM_TIMEOUT_ISOLATION_SLOTS, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_RECLAIM_SLOW_SECONDS("zram.reclaimSlowOperationSeconds", MemoryStandardGlobalConfig.ZRAM_RECLAIM_SLOW_SECONDS, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_OPERATION_RECORD_BUDGET("zram.operationRecordBudgetBytes", MemoryStandardGlobalConfig.ZRAM_OPERATION_RECORD_BUDGET, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_STARTUP_OBSERVATION("zram.startupObservationSeconds", MemoryStandardGlobalConfig.ZRAM_STARTUP_OBSERVATION, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    ZRAM_ALGORITHM("zram.algorithm", MemoryStandardGlobalConfig.ZRAM_ALGORITHM, Kind.STRING, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_ENABLED("writeback.enabled", MemoryStandardGlobalConfig.WRITEBACK_ENABLED, Kind.BOOLEAN, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_BACKEND_RESOURCE("writeback.backendResourceUuid", MemoryStandardGlobalConfig.WRITEBACK_BACKEND_RESOURCE, Kind.STRING, Scope.HOST),
    WRITEBACK_BACKEND_CAPACITY("writeback.backendCapacityBytes", MemoryStandardGlobalConfig.WRITEBACK_BACKEND_CAPACITY, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_BATCH("writeback.batchBytes", MemoryStandardGlobalConfig.WRITEBACK_BATCH, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_IDLE_OBSERVATION("writeback.idleObservationSeconds", MemoryStandardGlobalConfig.WRITEBACK_IDLE_OBSERVATION, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_OPERATION_INTERVAL("writeback.operationIntervalSeconds", MemoryStandardGlobalConfig.WRITEBACK_OPERATION_INTERVAL, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_SLOW_SECONDS("writeback.slowOperationSeconds", MemoryStandardGlobalConfig.WRITEBACK_SLOW_SECONDS, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_NO_PROGRESS_LIMIT("writeback.noProgressLimit", MemoryStandardGlobalConfig.WRITEBACK_NO_PROGRESS_LIMIT, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_BACKOFF("writeback.backoffSeconds", MemoryStandardGlobalConfig.WRITEBACK_BACKOFF, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_IO_BACKOFF("writeback.ioErrorBackoffSeconds", MemoryStandardGlobalConfig.WRITEBACK_IO_BACKOFF, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_METADATA_BUDGET("writeback.metadataBudgetBytes", MemoryStandardGlobalConfig.WRITEBACK_METADATA_BUDGET, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL),
    WRITEBACK_FILESYSTEM_RESERVE("writeback.filesystemReserveBytes", MemoryStandardGlobalConfig.WRITEBACK_FILESYSTEM_RESERVE, Kind.LONG, Scope.HOST, Scope.CLUSTER, Scope.GLOBAL);

    public enum Kind { BOOLEAN, LONG, DECIMAL, STRING }
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
            case DECIMAL:
                if (!(value instanceof Number)) { throw incompatible(value); }
                double number = ((Number) value).doubleValue();
                if (Double.isInfinite(number) || Double.isNaN(number)) { throw incompatible(value); }
                return number;
            case STRING:
                if (!(value instanceof String)) { throw incompatible(value); }
                String text = (String) value;
                if ("zram.algorithm".equals(path) && !text.matches("[a-zA-Z0-9_-]{1,64}")) {
                    throw new IllegalArgumentException("zram.algorithm must be a kernel algorithm identifier");
                }
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
