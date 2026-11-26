package org.zstack.kvm.memory;

import java.util.*;

import static org.zstack.kvm.memory.MemoryWritebackBackendInventory.*;

/** A disk of the Host as a swap disk candidate, or the Host's swap disk itself. */
public class MemorySwapDiskDeviceInventory {
    private static final Map<String, String> STATES = new HashMap<>();
    static {
        STATES.put("AVAILABLE", "Available");
        // Data on the disk: a preparation must confirm it is overwritten.
        STATES.put("NEEDS_OVERWRITE_CONFIRMATION", "NeedsOverwriteConfirmation");
        STATES.put("INELIGIBLE", "Ineligible");
        STATES.put("ACTIVE", "Active");
        STATES.put("FAULT", "Fault");
    }

    private String id;
    private String device;
    private String byIdPath;
    private String serial;
    private String wwn;
    private Long size;
    private String state;
    private List<String> reasons;
    private String swapUuid;
    private Long usedSize;

    static MemorySwapDiskDeviceInventory candidate(Map<?, ?> source) {
        MemorySwapDiskDeviceInventory result = common(source);
        result.id = string(source.get("id"));
        result.state = named(STATES, source.get("state"), "SWAP_DISK_CANDIDATE_STATE_UNKNOWN");
        return result;
    }

    /** The Host's swap disk: active while the Agent finds nothing wrong with it. */
    static MemorySwapDiskDeviceInventory swapDisk(Map<?, ?> source) {
        MemorySwapDiskDeviceInventory result = common(source);
        result.state = result.reasons.isEmpty() ? "Active" : "Fault";
        result.swapUuid = optional(source.get("uuid"));
        result.usedSize = exactLong(source.get("usedBytes"));
        return result;
    }

    private static MemorySwapDiskDeviceInventory common(Map<?, ?> source) {
        MemorySwapDiskDeviceInventory result = new MemorySwapDiskDeviceInventory();
        result.device = string(source.get("device"));
        result.byIdPath = optional(source.get("byIdPath"));
        result.serial = optional(source.get("serial"));
        result.wwn = optional(source.get("wwn"));
        result.size = exactLong(source.get("capacityBytes"));
        List<String> reasons = strings(source.get("reasons"));
        result.reasons = reasons == null ? Collections.emptyList() : reasons;
        return result;
    }

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getDevice() { return device; }
    public void setDevice(String value) { device = value; }
    public String getByIdPath() { return byIdPath; }
    public void setByIdPath(String value) { byIdPath = value; }
    public String getSerial() { return serial; }
    public void setSerial(String value) { serial = value; }
    public String getWwn() { return wwn; }
    public void setWwn(String value) { wwn = value; }
    public Long getSize() { return size; }
    public void setSize(Long value) { size = value; }
    public String getState() { return state; }
    public void setState(String value) { state = value; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> value) { reasons = value; }
    public String getSwapUuid() { return swapUuid; }
    public void setSwapUuid(String value) { swapUuid = value; }
    public Long getUsedSize() { return usedSize; }
    public void setUsedSize(Long value) { usedSize = value; }
}
