package org.zstack.kvm.memory;

/** Typed persisted policy. Null internally means no override, never an API clear operation. */
public class MemoryPolicyConfig {
    public int schemaVersion = 1;
    public Ksm ksm;
    public Zram zram;
    public Writeback writeback;
    public String participation;

    public static class Ksm {
        public Boolean enabled;
        public Boolean zeroPagesEnabled;
        public Long pagesToScan;
        public Long sleepMillis;
    }

    public static class Zram {
        public Boolean enabled;
        public String selectionMode;
        public java.util.List<String> selectedVmUuids;
        public Long discoveryIntervalSeconds, discoveryTimeoutSeconds, discoveryTtlSeconds;
        public Long hostSampleIntervalSeconds, hostSampleTtlSeconds, vmSampleIntervalSeconds, vmSampleTtlSeconds;
        public Boolean hostCpuGuardEnabled, hostMemoryPsiGuardEnabled, hostIoPsiGuardEnabled;
        public Double hostCpuThresholdPercent, hostMemoryPsiThresholdPercent, hostIoPsiThresholdPercent;
        public Long reclaimBatchBytes, concurrency, timeoutIsolationSlots, reclaimSlowOperationSeconds;
        public Long operationRecordBudgetBytes, startupObservationSeconds;
        public String algorithm;
        public Long logicalCapacityBytes;
        public Long ramLimitBytes;
        public Long requiredAvailableBytes;
        public Long hostFloorBytes;
        public Long metadataReserveBytes;
        public Long transientReserveBytes;
        public Long cpuThresholdPercent;
        public Long cpuHoldSeconds;
        public Long operationIntervalSeconds;
    }

    public static class Writeback {
        public Boolean enabled;
        public String backendResourceUuid;
        public Long backendCapacityBytes;
        public Long batchBytes;
        public Long idleObservationSeconds;
        public Long operationIntervalSeconds;
        public Long slowOperationSeconds, noProgressLimit, backoffSeconds, ioErrorBackoffSeconds;
        public Long metadataBudgetBytes, filesystemReserveBytes;
    }
}
