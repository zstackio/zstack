package org.zstack.header.allocator;

import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.*;

public class AllocateHostMsg extends NeedReplyMessage {
    private long cpuCapacity;
    private long memoryCapacity;
    private long diskSize;
    private String allocatorStrategy;
    private List<String> avoidHostUuids;
    private List<String> softAvoidHostUuids;
    private List<String> l3NetworkUuids;
    private VmInstanceInventory vmInstance;
    private ImageInventory image;
    private String vmOperation;
    private boolean isDryRun;
    private List<DiskOfferingInventory> diskOfferings;
    private boolean allowNoL3Networks;
    private boolean listAllHosts;
    private String requiredBackupStorageUuid;
    // each primary storage in this set is required
    private Set<String> requiredPrimaryStorageUuids = new HashSet<>();
    // for each set in the list, the primary storage inside is optional
    private final List<Set<String>> optionalPrimaryStorageUuids = new ArrayList<>();
    private boolean fullAllocate = true;
    private long oldMemoryCapacity = 0;
    private AllocationScene allocationScene;
    private String architecture;

    public List<Set<String>> getOptionalPrimaryStorageUuids() {
        return optionalPrimaryStorageUuids;
    }

    public void addOptionalPrimaryStorageUuids(Set<String> optionalPrimaryStorageUuids) {
        if (optionalPrimaryStorageUuids != null) {
            this.optionalPrimaryStorageUuids.add(optionalPrimaryStorageUuids);
        }
    }

    public AllocationScene getAllocationScene() {
        return allocationScene;
    }

    public void setAllocationScene(AllocationScene allocationScene) {
        this.allocationScene = allocationScene;
    }

    public Set<String> getRequiredPrimaryStorageUuids() {
        return requiredPrimaryStorageUuids;
    }

    public void setRequiredPrimaryStorageUuids(Set<String> requiredPrimaryStorageUuids) {
        this.requiredPrimaryStorageUuids = requiredPrimaryStorageUuids;
    }

    public void addRequiredPrimaryStorageUuid(String requiredPrimaryStorageUuid) {
        this.requiredPrimaryStorageUuids.add(requiredPrimaryStorageUuid);
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

    public boolean isDryRun() {
        return isDryRun;
    }

    public void setDryRun(boolean isDryRun) {
        this.isDryRun = isDryRun;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public String getVmOperation() {
        return vmOperation;
    }

    public void setVmOperation(String vmOperation) {
        this.vmOperation = vmOperation;
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

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public List<String> getAvoidHostUuids() {
        if (avoidHostUuids == null) {
            avoidHostUuids = new ArrayList<String>(0);
        }
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public List<String> getSoftAvoidHostUuids() {
        return softAvoidHostUuids;
    }

    public void setSoftAvoidHostUuids(List<String> softAvoidHostUuids) {
        this.softAvoidHostUuids = softAvoidHostUuids;
    }

    public List<String> getL3NetworkUuids() {
        if (l3NetworkUuids == null) {
            l3NetworkUuids = new ArrayList<String>(0);
        }
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public boolean isFullAllocate() {
        return fullAllocate;
    }

    public void setFullAllocate(boolean fullAllocate) {
        this.fullAllocate = fullAllocate;
    }

    public long getOldMemoryCapacity() {
        return oldMemoryCapacity;
    }

    public void setOldMemoryCapacity(long oldMemoryCapacity) {
        this.oldMemoryCapacity = oldMemoryCapacity;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }
}
