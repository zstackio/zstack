package org.zstack.kvm.memory;

import java.util.Map;

/** Stable local device identity used by the backend candidate contract. */
public class MemoryWritebackStableIdentityInventory {
    private String bootId;
    private String device;
    private String byIdPath;
    private String majorMinor;
    private String serial;
    private String wwn;
    private Long capacityBytes;
    private String transport;
    private String sysfsPath;
    private String sysfsInode;
    private Long rdev;
    private Long nodeInode;

    static MemoryWritebackStableIdentityInventory fromMap(Map<?, ?> source) {
        if (source == null) {
            return null;
        }
        MemoryWritebackStableIdentityInventory result = new MemoryWritebackStableIdentityInventory();
        result.bootId = MemoryWritebackBackendInventory.string(source.get("bootId"));
        result.device = MemoryWritebackBackendInventory.string(source.get("device"));
        result.byIdPath = MemoryWritebackBackendInventory.string(source.get("byIdPath"));
        result.majorMinor = MemoryWritebackBackendInventory.string(source.get("majorMinor"));
        result.serial = MemoryWritebackBackendInventory.string(source.get("serial"));
        result.wwn = MemoryWritebackBackendInventory.string(source.get("wwn"));
        result.capacityBytes = MemoryWritebackBackendInventory.exactLong(source.get("capacityBytes"));
        result.transport = MemoryWritebackBackendInventory.string(source.get("transport"));
        result.sysfsPath = MemoryWritebackBackendInventory.string(source.get("sysfsPath"));
        result.sysfsInode = MemoryWritebackBackendInventory.string(source.get("sysfsInode"));
        result.rdev = MemoryWritebackBackendInventory.exactLong(source.get("rdev"));
        result.nodeInode = MemoryWritebackBackendInventory.exactLong(source.get("nodeInode"));
        return result;
    }

    public String getBootId() { return bootId; }
    public String getDevice() { return device; }
    public String getByIdPath() { return byIdPath; }
    public String getMajorMinor() { return majorMinor; }
    public String getSerial() { return serial; }
    public String getWwn() { return wwn; }
    public Long getCapacityBytes() { return capacityBytes; }
    public String getTransport() { return transport; }
    public String getSysfsPath() { return sysfsPath; }
    public String getSysfsInode() { return sysfsInode; }
    public Long getRdev() { return rdev; }
    public Long getNodeInode() { return nodeInode; }
}
