package org.zstack.header.storage.primary;

import java.util.List;

/**
 */
public class PrimaryStorageAllocationSpec {
    private long size;
    private String requiredZoneUuid;
    private List<String> requiredClusterUuids;
    private String requiredHostUuid;
    private String requiredPrimaryStorageUuid;
    private List<String> tags;
    private AllocatePrimaryStorageMsg allocationMessage;
    private String vmInstanceUuid;
    private String diskOfferingUuid;
    private List<String> avoidPrimaryStorageUuids;
    private String imageUuid;
    private boolean noOverProvisioning;
    private String purpose;
    private List<String> requiredPrimaryStorageTypes;

    public List<String> getRequiredPrimaryStorageTypes() {
        return requiredPrimaryStorageTypes;
    }

    public void setRequiredPrimaryStorageTypes(List<String> requiredPrimaryStorageTypes) {
        this.requiredPrimaryStorageTypes = requiredPrimaryStorageTypes;
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

    public List<String> getAvoidPrimaryStorageUuids() {
        return avoidPrimaryStorageUuids;
    }

    public void setAvoidPrimaryStorageUuids(List<String> avoidPrimaryStorageUuids) {
        this.avoidPrimaryStorageUuids = avoidPrimaryStorageUuids;
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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

    public AllocatePrimaryStorageMsg getAllocationMessage() {
        return allocationMessage;
    }

    public void setAllocationMessage(AllocatePrimaryStorageMsg allocationMessage) {
        this.allocationMessage = allocationMessage;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
