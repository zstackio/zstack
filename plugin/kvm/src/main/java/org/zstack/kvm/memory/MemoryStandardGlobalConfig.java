package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.host.HostVO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.kvm.KVMGlobalConfig;

/** Standard ResourceConfig identities for scalar memory settings. */
@GlobalConfigDefinition
public class MemoryStandardGlobalConfig {
    private static final String CATEGORY = KVMGlobalConfig.CATEGORY;
    private static final String EMPTY = "";

    @GlobalConfigDef(defaultValue = EMPTY, description = "KSM zero-page scanning")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig KSM_ZERO_PAGES = new GlobalConfig(CATEGORY, "memory.ksm.zeroPagesEnabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "KSM pages scanned per batch")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig KSM_PAGES_TO_SCAN = new GlobalConfig(CATEGORY, "memory.ksm.pagesToScan");
    @GlobalConfigDef(defaultValue = EMPTY, description = "KSM sleep interval in milliseconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig KSM_SLEEP_MILLIS = new GlobalConfig(CATEGORY, "memory.ksm.sleepMillis");

    @GlobalConfigDef(defaultValue = "false", description = "Enable ZRAM")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_ENABLED = new GlobalConfig(CATEGORY, "memory.zram.enabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM logical capacity in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_LOGICAL_CAPACITY = new GlobalConfig(CATEGORY, "memory.zram.logicalCapacityBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM physical RAM limit in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_RAM_LIMIT = new GlobalConfig(CATEGORY, "memory.zram.ramLimitBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Minimum available host memory in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_REQUIRED_AVAILABLE = new GlobalConfig(CATEGORY, "memory.zram.requiredAvailableBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Host memory floor in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_HOST_FLOOR = new GlobalConfig(CATEGORY, "memory.zram.hostFloorBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM metadata reserve in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_METADATA_RESERVE = new GlobalConfig(CATEGORY, "memory.zram.metadataReserveBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM transient reserve in bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_TRANSIENT_RESERVE = new GlobalConfig(CATEGORY, "memory.zram.transientReserveBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM CPU threshold percent")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_CPU_THRESHOLD = new GlobalConfig(CATEGORY, "memory.zram.cpuThresholdPercent");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM CPU threshold hold seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_CPU_HOLD = new GlobalConfig(CATEGORY, "memory.zram.cpuHoldSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM operation interval seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_OPERATION_INTERVAL = new GlobalConfig(CATEGORY, "memory.zram.operationIntervalSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM discovery interval seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_DISCOVERY_INTERVAL = new GlobalConfig(CATEGORY, "memory.zram.discoveryIntervalSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM discovery timeout seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_DISCOVERY_TIMEOUT = new GlobalConfig(CATEGORY, "memory.zram.discoveryTimeoutSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM discovery TTL seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_DISCOVERY_TTL = new GlobalConfig(CATEGORY, "memory.zram.discoveryTtlSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Host sample interval seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_HOST_SAMPLE_INTERVAL = new GlobalConfig(CATEGORY, "memory.zram.hostSampleIntervalSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Host sample TTL seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_HOST_SAMPLE_TTL = new GlobalConfig(CATEGORY, "memory.zram.hostSampleTtlSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "VM sample interval seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_VM_SAMPLE_INTERVAL = new GlobalConfig(CATEGORY, "memory.zram.vmSampleIntervalSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "VM sample TTL seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_VM_SAMPLE_TTL = new GlobalConfig(CATEGORY, "memory.zram.vmSampleTtlSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Enable host CPU guard")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_CPU_GUARD = new GlobalConfig(CATEGORY, "memory.zram.hostCpuGuardEnabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Enable host memory PSI guard")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_MEMORY_PSI_GUARD = new GlobalConfig(CATEGORY, "memory.zram.hostMemoryPsiGuardEnabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Enable host IO PSI guard")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_IO_PSI_GUARD = new GlobalConfig(CATEGORY, "memory.zram.hostIoPsiGuardEnabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Host CPU guard threshold percent")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_HOST_CPU_THRESHOLD = new GlobalConfig(CATEGORY, "memory.zram.hostCpuThresholdPercent");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Host memory PSI threshold percent")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_HOST_MEMORY_PSI_THRESHOLD = new GlobalConfig(CATEGORY, "memory.zram.hostMemoryPsiThresholdPercent");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Host IO PSI threshold percent")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_HOST_IO_PSI_THRESHOLD = new GlobalConfig(CATEGORY, "memory.zram.hostIoPsiThresholdPercent");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM reclaim batch bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_RECLAIM_BATCH = new GlobalConfig(CATEGORY, "memory.zram.reclaimBatchBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM reclaim concurrency")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_CONCURRENCY = new GlobalConfig(CATEGORY, "memory.zram.concurrency");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM timeout isolation slots")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_TIMEOUT_ISOLATION_SLOTS = new GlobalConfig(CATEGORY, "memory.zram.timeoutIsolationSlots");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM slow reclaim operation seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_RECLAIM_SLOW_SECONDS = new GlobalConfig(CATEGORY, "memory.zram.reclaimSlowOperationSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM operation record budget bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_OPERATION_RECORD_BUDGET = new GlobalConfig(CATEGORY, "memory.zram.operationRecordBudgetBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "ZRAM startup observation seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_STARTUP_OBSERVATION = new GlobalConfig(CATEGORY, "memory.zram.startupObservationSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Kernel-advertised ZRAM algorithm identifier")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig ZRAM_ALGORITHM = new GlobalConfig(CATEGORY, "memory.zram.algorithm");

    @GlobalConfigDef(defaultValue = "false", description = "Enable writeback")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_ENABLED = new GlobalConfig(CATEGORY, "memory.writeback.enabled");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback backend capacity bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_BACKEND_CAPACITY = new GlobalConfig(CATEGORY, "memory.writeback.backendCapacityBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback backend resource UUID; host scope only")
    @BindResourceConfig({HostVO.class})
    public static GlobalConfig WRITEBACK_BACKEND_RESOURCE = new GlobalConfig(CATEGORY, "memory.writeback.backendResourceUuid");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback batch bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_BATCH = new GlobalConfig(CATEGORY, "memory.writeback.batchBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback idle observation seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_IDLE_OBSERVATION = new GlobalConfig(CATEGORY, "memory.writeback.idleObservationSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback operation interval seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_OPERATION_INTERVAL = new GlobalConfig(CATEGORY, "memory.writeback.operationIntervalSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback slow operation seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_SLOW_SECONDS = new GlobalConfig(CATEGORY, "memory.writeback.slowOperationSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback no-progress limit")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_NO_PROGRESS_LIMIT = new GlobalConfig(CATEGORY, "memory.writeback.noProgressLimit");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback backoff seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_BACKOFF = new GlobalConfig(CATEGORY, "memory.writeback.backoffSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback IO error backoff seconds")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_IO_BACKOFF = new GlobalConfig(CATEGORY, "memory.writeback.ioErrorBackoffSeconds");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback metadata budget bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_METADATA_BUDGET = new GlobalConfig(CATEGORY, "memory.writeback.metadataBudgetBytes");
    @GlobalConfigDef(defaultValue = EMPTY, description = "Writeback filesystem reserve bytes")
    @BindResourceConfig({HostVO.class, ClusterVO.class})
    public static GlobalConfig WRITEBACK_FILESYSTEM_RESERVE = new GlobalConfig(CATEGORY, "memory.writeback.filesystemReserveBytes");
}
