package org.zstack.kvm.memory;

import java.util.List;
import java.util.Map;

/** Candidate as returned by the Agent's read-only device probe. */
public class MemoryWritebackBackendCandidateInventory {
    private String identity;
    private String fingerprint;
    private String resourceUuid;
    private String type;
    private String path;
    private String device;
    private String byIdPath;
    private Long capacityBytes;
    private String hostBootId;
    private Boolean eligible;
    private Boolean writebackReady;
    private String state;
    private List<String> reasons;
    private String transport;
    private MemoryWritebackStableIdentityInventory stableIdentity;
    private Boolean active;

    static MemoryWritebackBackendCandidateInventory fromMap(Map<?, ?> source) {
        MemoryWritebackBackendCandidateInventory result = new MemoryWritebackBackendCandidateInventory();
        result.identity = MemoryWritebackBackendInventory.string(source.get("identity"));
        result.fingerprint = MemoryWritebackBackendInventory.string(source.get("fingerprint"));
        result.resourceUuid = MemoryWritebackBackendInventory.string(source.get("resourceUuid"));
        result.type = MemoryWritebackBackendInventory.string(source.get("type"));
        result.path = MemoryWritebackBackendInventory.string(source.get("path"));
        result.device = MemoryWritebackBackendInventory.string(source.get("device"));
        result.byIdPath = MemoryWritebackBackendInventory.string(source.get("byIdPath"));
        result.capacityBytes = MemoryWritebackBackendInventory.exactLong(source.get("capacityBytes"));
        result.hostBootId = MemoryWritebackBackendInventory.string(source.get("hostBootId"));
        result.eligible = MemoryWritebackBackendInventory.bool(source.get("eligible"));
        result.writebackReady = MemoryWritebackBackendInventory.bool(source.get("writebackReady"));
        result.state = MemoryWritebackBackendInventory.string(source.get("state"));
        result.reasons = MemoryWritebackBackendInventory.strings(source.get("reasons"));
        result.transport = MemoryWritebackBackendInventory.string(source.get("transport"));
        result.stableIdentity = MemoryWritebackStableIdentityInventory.fromMap(
                MemoryWritebackBackendInventory.map(source.get("stableIdentity")));
        result.active = MemoryWritebackBackendInventory.bool(source.get("active"));
        return result;
    }

    public String getIdentity() { return identity; }
    public String getFingerprint() { return fingerprint; }
    public String getResourceUuid() { return resourceUuid; }
    public String getType() { return type; }
    public String getPath() { return path; }
    public String getDevice() { return device; }
    public String getByIdPath() { return byIdPath; }
    public Long getCapacityBytes() { return capacityBytes; }
    public String getHostBootId() { return hostBootId; }
    public Boolean getEligible() { return eligible; }
    public Boolean getWritebackReady() { return writebackReady; }
    public String getState() { return state; }
    public List<String> getReasons() { return reasons; }
    public String getTransport() { return transport; }
    public MemoryWritebackStableIdentityInventory getStableIdentity() { return stableIdentity; }
    public Boolean getActive() { return active; }
}
