package org.zstack.header.allocator;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class HostAllocatorSpec {
    private List<String> avoidHostUuids;
    private long cpuCapacity;
    private long memoryCapacity;
    private List<String> l3NetworkUuids;
    private long diskSize;
    private String hypervisorType;
    private String allocatorStrategy;
    private VmInstanceInventory vmInstance;
    private ImageInventory image;
    private String vmOperation;
    private List<DiskOfferingInventory> diskOfferings = new ArrayList<>();
    private Map<Object, Object> extraData = new HashMap<>();
    private boolean allowNoL3Networks;
    private boolean listAllHosts;
    private String requiredBackupStorageUuid;
    private String requiredPrimaryStorageUuid;
    private Map<String, List<String>> backupStoragePrimaryStorageMetrics;
    private boolean dryRun;

    public String getRequiredPrimaryStorageUuid() {
        return requiredPrimaryStorageUuid;
    }

    public void setRequiredPrimaryStorageUuid(String requiredPrimaryStorageUuid) {
        this.requiredPrimaryStorageUuid = requiredPrimaryStorageUuid;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getRequiredBackupStorageUuid() {
        return requiredBackupStorageUuid;
    }

    public void setRequiredBackupStorageUuid(String requiredBackupStorageUuid) {
        this.requiredBackupStorageUuid = requiredBackupStorageUuid;
    }

    public boolean isListAllHosts() {
        return listAllHosts;
    }

    public void setListAllHosts(boolean listAllHosts) {
        this.listAllHosts = listAllHosts;
    }

    public boolean isAllowNoL3Networks() {
        return allowNoL3Networks;
    }

    public void setAllowNoL3Networks(boolean allowNoL3Networks) {
        this.allowNoL3Networks = allowNoL3Networks;
    }

    public List<DiskOfferingInventory> getDiskOfferings() {
        return diskOfferings;
    }

    public void setDiskOfferings(List<DiskOfferingInventory> diskOfferings) {
        this.diskOfferings = diskOfferings;
    }

    public String getVmOperation() {
        return vmOperation;
    }

    public void setVmOperation(String vmOperation) {
        this.vmOperation = vmOperation;
    }

    public List<String> getAvoidHostUuids() {
        if (avoidHostUuids == null) {
            avoidHostUuids = new ArrayList<>();
        }
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public long getCpuCapacity() {
        return cpuCapacity;
    }

    public void setCpuCapacity(long cpuCapacity) {
        this.cpuCapacity = cpuCapacity;
    }

    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    public void setMemoryCapacity(long memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    public List<String> getL3NetworkUuids() {
        if (l3NetworkUuids == null) {
            l3NetworkUuids = new ArrayList<>();
        }
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public Map<Object, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<Object, Object> extraData) {
        this.extraData = extraData;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public static HostAllocatorSpec fromAllocationMsg(AllocateHostMsg msg) {
        HostAllocatorSpec spec = new HostAllocatorSpec();
        spec.setAllocatorStrategy(msg.getAllocatorStrategy());
        spec.setAvoidHostUuids(msg.getAvoidHostUuids());
        spec.setCpuCapacity(msg.getCpuCapacity());
        spec.setDiskSize(msg.getDiskSize());
        spec.setListAllHosts(msg.isListAllHosts());
        spec.setDryRun(msg.isDryRun());
        String hvType = null;
        if (msg.getVmInstance() != null && msg.getVmInstance().getHypervisorType() != null) {
            hvType = msg.getVmInstance().getHypervisorType();
        }
        if (hvType == null && msg.getImage() != null) {
            HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getImage().getFormat());
            if (type != null) {
                hvType = type.toString();
            }
        }
        spec.setHypervisorType(hvType);
        spec.setMemoryCapacity(msg.getMemoryCapacity());
        spec.setVmInstance(msg.getVmInstance());
        spec.setL3NetworkUuids(msg.getL3NetworkUuids());
        spec.setImage(msg.getImage());
        spec.setVmOperation(msg.getVmOperation());
        spec.setDiskOfferings(msg.getDiskOfferings());
        spec.setAllowNoL3Networks(msg.isAllowNoL3Networks());
        spec.setRequiredBackupStorageUuid(msg.getRequiredBackupStorageUuid());
        spec.setRequiredPrimaryStorageUuid(msg.getRequiredPrimaryStorageUuid());
        return spec;
    }

    public Map<String, List<String>> getBackupStoragePrimaryStorageMetrics() {
        return backupStoragePrimaryStorageMetrics;
    }

    public void setBackupStoragePrimaryStorageMetrics(Map<String, List<String>> backupStoragePrimaryStorageMetrics) {
        this.backupStoragePrimaryStorageMetrics = backupStoragePrimaryStorageMetrics;
    }
}
