package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.CollectionDSL;

import java.util.ArrayList;
import java.util.List;

public class AllocatePrimaryStorageMsg extends NeedReplyMessage {
    private String requiredZoneUuid;
    private List<String> requiredClusterUuids;
    private String requiredHostUuid;
    private String requiredPrimaryStorageUuid;
    private String backupStorageUuid;
    private List<String> possiblePrimaryStorageTypes;
    private List<String> excludePrimaryStorageTypes;

    private Long totalSize = null;
    private long size;
    private boolean dryRun;
    private List<String> tags;
    private String allocationStrategy;
    private String vmInstanceUuid;
    private String diskOfferingUuid;
    private List<String> excludePrimaryStorageUuids;
    private List<String> excludeAllocatorStrategies;
    private String imageUuid;
    private String volumeUuid;
    private boolean noOverProvisioning;
    private String purpose;

    public List<String> getExcludePrimaryStorageTypes() {
        return excludePrimaryStorageTypes;
    }

    public void addExcludePrimaryStorageTypes(List<String> excludePrimaryStorageTypes) {
        if (this.excludePrimaryStorageTypes == null) {
            this.excludePrimaryStorageTypes = CollectionDSL.list();
        }
        this.excludePrimaryStorageTypes.addAll(excludePrimaryStorageTypes);
    }

    public void setExcludePrimaryStorageTypes(List<String> excludePrimaryStorageTypes) {
        this.excludePrimaryStorageTypes = excludePrimaryStorageTypes;
    }

    public List<String> getPossiblePrimaryStorageTypes() {
        return possiblePrimaryStorageTypes;
    }

    public void setPossiblePrimaryStorageTypes(List<String> possiblePrimaryStorageTypes) {
        this.possiblePrimaryStorageTypes = possiblePrimaryStorageTypes;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public boolean isNoOverProvisioning() {
        return noOverProvisioning;
    }

    public void setNoOverProvisioning(boolean noOverProvisioning) {
        this.noOverProvisioning = noOverProvisioning;
    }

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
            excludeAllocatorStrategies = new ArrayList<>();
        }
        excludeAllocatorStrategies.add(allocationStrategy);
    }

    public List<String> getExcludePrimaryStorageUuids() {
        return excludePrimaryStorageUuids;
    }

    public void setExcludePrimaryStorageUuids(List<String> excludePrimaryStorageUuids) {
        this.excludePrimaryStorageUuids = excludePrimaryStorageUuids;
    }

    public void addExcludePrimaryStorageUuid(String priUuid) {
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

    public String getRequiredPrimaryStorageUuid() {
        return requiredPrimaryStorageUuid;
    }

    public void setRequiredPrimaryStorageUuid(String requiredPrimaryStorageUuid) {
        this.requiredPrimaryStorageUuid = requiredPrimaryStorageUuid;
    }

    public String getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(String allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public String getRequiredZoneUuid() {
        return requiredZoneUuid;
    }

    public void setRequiredZoneUuid(String requiredZoneUuid) {
        this.requiredZoneUuid = requiredZoneUuid;
    }

    public List<String> getRequiredClusterUuids() {
        return requiredClusterUuids;
    }

    public void setRequiredClusterUuids(List<String> requiredClusterUuids) {
        this.requiredClusterUuids = requiredClusterUuids;
    }

    public String getRequiredHostUuid() {
        return requiredHostUuid;
    }

    public void setRequiredHostUuid(String requiredHostUuid) {
        this.requiredHostUuid = requiredHostUuid;
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

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
