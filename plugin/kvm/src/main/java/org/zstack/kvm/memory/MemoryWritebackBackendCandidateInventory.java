package org.zstack.kvm.memory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A disk that can back ZRAM writeback, or one prepared before. */
public class MemoryWritebackBackendCandidateInventory {
    /** The Agent's candidate states as the API names them. */
    private static final Map<String, String> STATES = new HashMap<>();
    static {
        STATES.put("NEEDS_OFFLINE_PREPARATION", "Available");
        STATES.put("PREPARED", "Prepared");
        STATES.put("ACTIVE", "Active");
        STATES.put("INELIGIBLE", "Ineligible");
        STATES.put("DISCONNECTED", "Disconnected");
        STATES.put("IDENTITY_MISMATCH", "IdentityMismatch");
    }

    private String id;
    private String resourceUuid;
    private String type;
    private String path;
    private String device;
    private Long size;
    private String serial;
    private String wwn;
    private String transport;
    private String state;
    private List<String> reasons;

    static MemoryWritebackBackendCandidateInventory fromMap(Map<?, ?> source) {
        MemoryWritebackBackendCandidateInventory result = new MemoryWritebackBackendCandidateInventory();
        result.id = MemoryWritebackBackendInventory.string(source.get("identity"));
        result.resourceUuid = MemoryWritebackBackendInventory.string(source.get("resourceUuid"));
        result.type = MemoryWritebackBackendInventory.string(source.get("type"));
        result.path = MemoryWritebackBackendInventory.string(source.get("path"));
        result.device = MemoryWritebackBackendInventory.string(source.get("device"));
        result.size = MemoryWritebackBackendInventory.exactLong(source.get("capacityBytes"));
        Map<?, ?> stable = MemoryWritebackBackendInventory.map(source.get("stableIdentity"));
        result.serial = stable == null ? null : MemoryWritebackBackendInventory.optional(stable.get("serial"));
        result.wwn = stable == null ? null : MemoryWritebackBackendInventory.optional(stable.get("wwn"));
        result.transport = MemoryWritebackBackendInventory.optional(source.get("transport"));
        result.state = MemoryWritebackBackendInventory.named(STATES, source.get("state"), "WRITEBACK_CANDIDATE_STATE_UNKNOWN");
        result.reasons = MemoryWritebackBackendInventory.strings(source.get("reasons"));
        return result;
    }

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getResourceUuid() { return resourceUuid; }
    public void setResourceUuid(String value) { resourceUuid = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getPath() { return path; }
    public void setPath(String value) { path = value; }
    public String getDevice() { return device; }
    public void setDevice(String value) { device = value; }
    public Long getSize() { return size; }
    public void setSize(Long value) { size = value; }
    public String getSerial() { return serial; }
    public void setSerial(String value) { serial = value; }
    public String getWwn() { return wwn; }
    public void setWwn(String value) { wwn = value; }
    public String getTransport() { return transport; }
    public void setTransport(String value) { transport = value; }
    public String getState() { return state; }
    public void setState(String value) { state = value; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> value) { reasons = value; }
}
