package org.zstack.kvm.memory;

import java.util.*;

import static org.zstack.kvm.memory.MemoryWritebackBackendInventory.*;

/** The Host's swap disk for the disk-swap reclaim backend, its candidates and whether one can be prepared now. */
public class MemorySwapDiskInventory {
    private static final Map<String, String> STATUSES = new HashMap<>();
    static {
        STATUSES.put("NOT_CONFIGURED", "NotConfigured");
        STATUSES.put("ACTIVE", "Active");
        // Missing, replaced, not enabled, or left by an interrupted preparation; never formatted again by itself.
        STATUSES.put("FAULT", "Fault");
    }

    private String hostUuid;
    private String status;
    private MemorySwapDiskDeviceInventory swapDisk;
    private List<String> otherSwaps;
    private List<String> preparationReasons;
    private List<MemorySwapDiskDeviceInventory> candidates;

    public static MemorySwapDiskInventory __example__() {
        String hostUuid = "1106a40096e946839d9aaed1b97d4672";
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("id", "candidate:48e5a935e1c0e0eb31aea15a379969ffe824d0a6dd0d453eb77ad81d913a5fec");
        candidate.put("device", "/dev/nvme0n1");
        candidate.put("byIdPath", "/dev/disk/by-id/nvme-SAMSUNG_MZVL2256HCHQ_S674NX0T133700");
        candidate.put("serial", "S674NX0T133700");
        candidate.put("wwn", "eui.002538b131a0b1c2");
        candidate.put("capacityBytes", 256060514304L);
        candidate.put("state", "AVAILABLE");
        candidate.put("reasons", Collections.emptyList());
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", hostUuid);
        state.put("status", "NOT_CONFIGURED");
        state.put("swapDisk", null);
        state.put("otherSwaps", Collections.emptyList());
        state.put("preparationReasons", Collections.emptyList());
        state.put("candidates", Collections.singletonList(candidate));
        return fromAgentState(hostUuid, hostUuid, state);
    }

    public static MemorySwapDiskInventory fromAgentState(String expectedHostUuid, String responseHostUuid,
                                                         Map<String, Object> state) {
        if (expectedHostUuid == null || !expectedHostUuid.equals(responseHostUuid)
                || state == null || !expectedHostUuid.equals(state.get("hostUuid"))) {
            throw new IllegalArgumentException("SWAP_DISK_INVENTORY_HOST_IDENTITY_MISMATCH");
        }
        MemorySwapDiskInventory result = new MemorySwapDiskInventory();
        result.hostUuid = expectedHostUuid;
        result.status = named(STATUSES, state.get("status"), "SWAP_DISK_STATUS_UNKNOWN");
        Map<?, ?> disk = map(state.get("swapDisk"));
        result.swapDisk = disk == null ? null : MemorySwapDiskDeviceInventory.swapDisk(disk);
        List<String> others = strings(state.get("otherSwaps"));
        result.otherSwaps = others == null ? Collections.emptyList() : others;
        // An Agent that did not decide whether a preparation can start must not read as "it can".
        List<String> preparation = strings(state.get("preparationReasons"));
        result.preparationReasons = preparation != null ? preparation
                : Collections.singletonList("PREPARATION_STATE_UNKNOWN");
        if (!state.containsKey("candidates") || state.get("candidates") == null) {
            throw new IllegalArgumentException("SWAP_DISK_CANDIDATES_UNKNOWN");
        }
        result.candidates = new ArrayList<>();
        Object candidates = state.get("candidates");
        if (!(candidates instanceof List)) {
            throw new IllegalArgumentException("SWAP_DISK_CANDIDATES_MALFORMED");
        }
        for (Object item : (List<?>) candidates) {
            Map<?, ?> candidate = map(item);
            if (candidate == null) {
                throw new IllegalArgumentException("SWAP_DISK_CANDIDATE_MALFORMED");
            }
            result.candidates.add(MemorySwapDiskDeviceInventory.candidate(candidate));
        }
        return result;
    }

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public MemorySwapDiskDeviceInventory getSwapDisk() { return swapDisk; }
    public void setSwapDisk(MemorySwapDiskDeviceInventory value) { swapDisk = value; }
    public List<String> getOtherSwaps() { return otherSwaps; }
    public void setOtherSwaps(List<String> value) { otherSwaps = value; }
    public List<String> getPreparationReasons() { return preparationReasons; }
    public void setPreparationReasons(List<String> value) { preparationReasons = value; }
    public List<MemorySwapDiskDeviceInventory> getCandidates() { return candidates; }
    public void setCandidates(List<MemorySwapDiskDeviceInventory> value) { candidates = value; }
}
