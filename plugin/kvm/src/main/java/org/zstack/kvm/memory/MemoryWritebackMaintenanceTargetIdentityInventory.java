package org.zstack.kvm.memory;

import java.util.Map;

/** Readback identity fields pinned by backend-maintenance receipts. */
public class MemoryWritebackMaintenanceTargetIdentityInventory {
    private String bootId;
    private String canonicalDevice;
    private Long rdev;
    private Long nodeInode;
    private Long sysfsInode;
    private Long capacityBytes;
    private String filePath;
    private Long fileInode;
    private Long fileDevice;
    private Long fileSize;
    private Boolean directIo;
    private String stackSha256;
    private String fileStackSha256;

    static MemoryWritebackMaintenanceTargetIdentityInventory fromMap(Map<?, ?> source) {
        if (source == null) {
            return null;
        }
        MemoryWritebackMaintenanceTargetIdentityInventory result = new MemoryWritebackMaintenanceTargetIdentityInventory();
        result.bootId = MemoryWritebackBackendInventory.string(source.get("boot_id"));
        result.canonicalDevice = MemoryWritebackBackendInventory.string(source.get("canonical_device"));
        result.rdev = MemoryWritebackBackendInventory.exactLong(source.get("rdev"));
        result.nodeInode = MemoryWritebackBackendInventory.exactLong(source.get("node_inode"));
        result.sysfsInode = MemoryWritebackBackendInventory.exactLong(source.get("sysfs_inode"));
        result.capacityBytes = MemoryWritebackBackendInventory.exactLong(source.get("capacity_bytes"));
        result.filePath = MemoryWritebackBackendInventory.string(source.get("file_path"));
        result.fileInode = MemoryWritebackBackendInventory.exactLong(source.get("file_inode"));
        result.fileDevice = MemoryWritebackBackendInventory.exactLong(source.get("file_device"));
        result.fileSize = MemoryWritebackBackendInventory.exactLong(source.get("file_size"));
        result.directIo = MemoryWritebackBackendInventory.bool(source.get("direct_io"));
        result.stackSha256 = MemoryWritebackBackendInventory.string(source.get("stack_sha256"));
        result.fileStackSha256 = MemoryWritebackBackendInventory.string(source.get("file_stack_sha256"));
        return result;
    }

    public String getBootId() { return bootId; }
    public String getCanonicalDevice() { return canonicalDevice; }
    public Long getRdev() { return rdev; }
    public Long getNodeInode() { return nodeInode; }
    public Long getSysfsInode() { return sysfsInode; }
    public Long getCapacityBytes() { return capacityBytes; }
    public String getFilePath() { return filePath; }
    public Long getFileInode() { return fileInode; }
    public Long getFileDevice() { return fileDevice; }
    public Long getFileSize() { return fileSize; }
    public Boolean getDirectIo() { return directIo; }
    public String getStackSha256() { return stackSha256; }
    public String getFileStackSha256() { return fileStackSha256; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getBoot_id() { return bootId; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getCanonical_device() { return canonicalDevice; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getNode_inode() { return nodeInode; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getSysfs_inode() { return sysfsInode; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getCapacity_bytes() { return capacityBytes; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getFile_path() { return filePath; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getFile_inode() { return fileInode; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getFile_device() { return fileDevice; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Long getFile_size() { return fileSize; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public Boolean getDirect_io() { return directIo; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getStack_sha256() { return stackSha256; }
    /** @deprecated internal Java source compatibility only. */
    @Deprecated public String getFile_stack_sha256() { return fileStackSha256; }
}
