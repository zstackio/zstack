package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.List;

public class AllocatePrimaryStorageMsg extends NeedReplyMessage {
    private String zoneUuid;
    private List<String> clusterUuids;
    private String hostUuid;
    private long size;
    private boolean dryRun;
    private List<String> tags;
    private String allocationStrategy;
    private String primaryStorageUuid;
    private String vmInstanceUuid;
    private String diskOfferingUuid;
    private List<String> excludePrimaryStorageUuids;
    private List<String> excludeAllocatorStrategies;
    private String imageUuid;

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public List<String> getExcludeAllocatorStrategies() {
        return excludeAllocatorStrategies;
    }

    public void setExcludeAllocatorStrategies(List<String> excludeAllocatorStrategies) {
        this.excludeAllocatorStrategies = excludeAllocatorStrategies;
    }

    public void addExcludeAllocatorStrategy(String allocationStrategy) {
        if (excludeAllocatorStrategies == null) {
            excludeAllocatorStrategies = new ArrayList<String>();
        }
        excludeAllocatorStrategies.add(allocationStrategy);
    }

    public List<String> getExcludePrimaryStorageUuids() {
        return excludePrimaryStorageUuids;
    }

    public void setExcludePrimaryStorageUuids(List<String> excludePrimaryStorageUuids) {
        this.excludePrimaryStorageUuids = excludePrimaryStorageUuids;
    }

    public void addExcludePrimaryStoratgeUuid(String priUuid) {
        if (excludePrimaryStorageUuids == null) {
            excludePrimaryStorageUuids = new ArrayList<String>();
        }

        excludePrimaryStorageUuids.add(priUuid);
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getDiskOfferingUuid() {
        return diskOfferingUuid;
    }

    public void setDiskOfferingUuid(String diskOfferingUuid) {
        this.diskOfferingUuid = diskOfferingUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(String allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
