package org.zstack.kvm.memory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Fixed public projection of the read-only Agent writeback-backend inventory. */
public class MemoryWritebackBackendInventory {
    private String hostUuid;
    private String bootId;
    private String observedAt;
    private String status;
    private List<String> reasons;
    private Boolean candidatesFieldPresent;
    private List<MemoryWritebackBackendCandidateInventory> candidates;
    private String controlOperationUuid;
    private MemoryWritebackMaintenanceInventory maintenance;
    private String poolGeneration;
    private Boolean canPrepare;
    private List<String> preparationReasons;
    private Boolean canPrepareZramPool;
    private List<String> zramPoolPreparationReasons;
    private String zramPoolPreparationQuality;
    private Long zramPoolPreparationObservedAt;

    public static MemoryWritebackBackendInventory __example__() {
        String hostUuid = "093d46206e694835b4a218449eb1bc7c";
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", hostUuid);
        state.put("bootId", "8c6ce488-e264-4c1d-bcd9-763196d10a3e");
        state.put("observedAt", "2026-10-08T14:04:20.720910Z");
        state.put("status", "AVAILABLE");
        state.put("reasons", java.util.Collections.emptyList());
        state.put("controlOperationUuid", "a257efe35e334ee89e600d4abbeba371");
        state.put("poolGeneration", null);
        state.put("canPrepare", false);
        state.put("preparationReasons", java.util.Arrays.asList("MEMORY_SERVICE_NOT_STOPPED",
                "NO_ELIGIBLE_WRITEBACK_CANDIDATE", "POOL_GENERATION_UNKNOWN"));
        state.put("canPrepareZramPool", false);
        state.put("zramPoolPreparationReasons", java.util.Arrays.asList("DRAIN_PROOF_UNAVAILABLE",
                "MEMORY_SERVICE_NOT_STOPPED", "WRITEBACK_BACKEND_CONFIGURED"));
        state.put("zramPoolPreparationQuality", "Ineligible");
        state.put("zramPoolPreparationObservedAt", 1791468260938L);

        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("identity", "e9a8d7cb8b6dc595a1c589841e83eb2d10760690a1c9dad3e2c4e970695f253b");
        candidate.put("fingerprint", "e9a8d7cb8b6dc595a1c589841e83eb2d10760690a1c9dad3e2c4e970695f253b");
        candidate.put("resourceUuid", "6266ba305b5ed487b5f22cefee13f466");
        candidate.put("type", "block");
        candidate.put("path", "/dev/disk/by-id/virtio-e4e9ca6be5425a879187");
        candidate.put("device", "/dev/vdb");
        candidate.put("byIdPath", "/dev/disk/by-id/virtio-e4e9ca6be5425a879187");
        candidate.put("capacityBytes", 21474836480L);
        candidate.put("hostBootId", state.get("bootId"));
        candidate.put("eligible", true);
        candidate.put("writebackReady", true);
        candidate.put("state", "PREPARED");
        candidate.put("reasons", java.util.Collections.emptyList());
        candidate.put("transport", "");
        candidate.put("active", false);
        Map<String, Object> stableIdentity = new LinkedHashMap<>();
        stableIdentity.put("bootId", state.get("bootId"));
        stableIdentity.put("device", "/dev/vdb");
        stableIdentity.put("byIdPath", candidate.get("byIdPath"));
        stableIdentity.put("majorMinor", "252:16");
        stableIdentity.put("serial", "e4e9ca6be5425a879187");
        stableIdentity.put("wwn", "");
        stableIdentity.put("capacityBytes", 21474836480L);
        stableIdentity.put("transport", "");
        stableIdentity.put("sysfsPath", "/sys/devices/pci0000:00/0000:00:0c.0/virtio6/block/vdb");
        stableIdentity.put("sysfsInode", "79610");
        stableIdentity.put("rdev", 64528L);
        stableIdentity.put("nodeInode", 1043854454L);
        candidate.put("stableIdentity", stableIdentity);
        state.put("candidates", java.util.Collections.singletonList(candidate));

        Map<String, Object> maintenance = new LinkedHashMap<>();
        maintenance.put("schema", 1L);
        maintenance.put("request_id", "288097e635094a56afe759757cc5397f");
        maintenance.put("candidate_id", "candidate:724d4582adec92a9bd70b2da9d9c2965fbc1506e71987b7201e2d782d12de065");
        maintenance.put("request_fingerprint", "13114029099baf472d0f3942440f800967e7a904896695de16d38196d5b80af8");
        maintenance.put("stage", "APPLIED");
        maintenance.put("started_at", "2026-10-08T09:22:55.375876961Z");
        maintenance.put("updated_at", "2026-10-08T11:51:44.459781937Z");
        maintenance.put("host_boot_id", state.get("bootId"));
        maintenance.put("pool_generation", "af013f2fc83c7845def03466c8612396eb8604a1ddccd6ff4283f0e0317f2333");
        Map<String, Object> targetIdentity = new LinkedHashMap<>();
        targetIdentity.put("boot_id", state.get("bootId"));
        targetIdentity.put("canonical_device", "/dev/vdb");
        targetIdentity.put("rdev", 64528L);
        targetIdentity.put("node_inode", 1043854454L);
        targetIdentity.put("sysfs_inode", 79610L);
        targetIdentity.put("capacity_bytes", 21474836480L);
        targetIdentity.put("direct_io", false);
        targetIdentity.put("stack_sha256", "866d2053446ba0b001fdf9b46dbe373f418c09be30fcd465c5a006884600f75c");
        maintenance.put("target_identity", targetIdentity);
        maintenance.put("reset_attempted", true);
        maintenance.put("archive", "/var/lib/zram-efficiency-writeback-20261007/backend-maintenance/288097e635094a56afe759757cc5397f/pre-reset-archive");
        maintenance.put("backend_resource_uuid", "6266ba305b5ed487b5f22cefee13f466");
        state.put("maintenance", maintenance);
        return fromAgentState(hostUuid, hostUuid, state);
    }

    public static MemoryWritebackBackendInventory fromAgentState(String expectedHostUuid,
                                                                  String responseHostUuid,
                                                                  Map<String, Object> state) {
        if (expectedHostUuid == null || !expectedHostUuid.equals(responseHostUuid)
                || state == null || !expectedHostUuid.equals(state.get("hostUuid"))) {
            throw new IllegalArgumentException("WRITEBACK_INVENTORY_HOST_IDENTITY_MISMATCH");
        }
        MemoryWritebackBackendInventory result = new MemoryWritebackBackendInventory();
        result.hostUuid = expectedHostUuid;
        result.bootId = string(state.get("bootId"));
        result.observedAt = string(state.get("observedAt"));
        result.status = string(state.get("status"));
        result.reasons = strings(state.get("reasons"));
        result.candidatesFieldPresent = state.containsKey("candidates");
        result.candidates = candidates(state.get("candidates"));
        result.controlOperationUuid = string(state.get("controlOperationUuid"));
        result.maintenance = MemoryWritebackMaintenanceInventory.fromMap(map(state.get("maintenance")));
        result.poolGeneration = string(state.get("poolGeneration"));
        result.canPrepare = bool(state.get("canPrepare"));
        result.preparationReasons = strings(state.get("preparationReasons"));
        result.canPrepareZramPool = bool(state.get("canPrepareZramPool"));
        result.zramPoolPreparationReasons = strings(state.get("zramPoolPreparationReasons"));
        result.zramPoolPreparationQuality = string(state.get("zramPoolPreparationQuality"));
        result.zramPoolPreparationObservedAt = exactLong(state.get("zramPoolPreparationObservedAt"));
        return result;
    }

    private static List<MemoryWritebackBackendCandidateInventory> candidates(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("WRITEBACK_CANDIDATES_MALFORMED");
        }
        List<MemoryWritebackBackendCandidateInventory> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            Map<?, ?> candidate = map(item);
            if (candidate == null) {
                throw new IllegalArgumentException("WRITEBACK_CANDIDATE_MALFORMED");
            }
            result.add(MemoryWritebackBackendCandidateInventory.fromMap(candidate));
        }
        return result;
    }

    static Map<?, ?> map(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("WRITEBACK_OBJECT_MALFORMED");
        }
        return (Map<?, ?>) value;
    }

    static String string(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("WRITEBACK_STRING_FIELD_MALFORMED");
        }
        return (String) value;
    }

    static Boolean bool(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("WRITEBACK_BOOLEAN_FIELD_MALFORMED");
        }
        return (Boolean) value;
    }

    static Long exactLong(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("WRITEBACK_INTEGER_FIELD_MALFORMED");
        }
        if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            double maxExactlyRepresentable = value instanceof Float ? 16777215d : 9007199254740991d;
            if (!Double.isFinite(number) || number != Math.rint(number)
                    || number > maxExactlyRepresentable) {
                throw new IllegalArgumentException("WRITEBACK_INTEGER_PRECISION_UNSAFE");
            }
        }
        try {
            long exact = new BigDecimal(value.toString()).longValueExact();
            if (exact < 0) {
                throw new IllegalArgumentException("WRITEBACK_INTEGER_FIELD_MALFORMED");
            }
            return exact;
        } catch (ArithmeticException | NumberFormatException invalid) {
            throw new IllegalArgumentException("WRITEBACK_INTEGER_FIELD_MALFORMED", invalid);
        }
    }

    static List<String> strings(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("WRITEBACK_STRING_LIST_MALFORMED");
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            String string = string(item);
            if (string == null) {
                throw new IllegalArgumentException("WRITEBACK_STRING_LIST_MALFORMED");
            }
            result.add(string);
        }
        return result;
    }

    public String getHostUuid() { return hostUuid; }
    public String getBootId() { return bootId; }
    public String getObservedAt() { return observedAt; }
    public String getStatus() { return status; }
    public List<String> getReasons() { return reasons; }
    public Boolean getCandidatesFieldPresent() { return candidatesFieldPresent; }
    public List<MemoryWritebackBackendCandidateInventory> getCandidates() { return candidates; }
    public String getControlOperationUuid() { return controlOperationUuid; }
    public MemoryWritebackMaintenanceInventory getMaintenance() { return maintenance; }
    public String getPoolGeneration() { return poolGeneration; }
    public Boolean getCanPrepare() { return canPrepare; }
    public List<String> getPreparationReasons() { return preparationReasons; }
    public Boolean getCanPrepareZramPool() { return canPrepareZramPool; }
    public List<String> getZramPoolPreparationReasons() { return zramPoolPreparationReasons; }
    public String getZramPoolPreparationQuality() { return zramPoolPreparationQuality; }
    public Long getZramPoolPreparationObservedAt() { return zramPoolPreparationObservedAt; }
}
