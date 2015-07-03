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
    private List<String> avoidPrimaryStorageUuids;

    public List<String> getAvoidPrimaryStorageUuids() {
        return avoidPrimaryStorageUuids;
    }

    public void setAvoidPrimaryStorageUuids(List<String> avoidPrimaryStorageUuids) {
        this.avoidPrimaryStorageUuids = avoidPrimaryStorageUuids;
    }

    public void addVoidPrimaryStoratgeUuid(String priUuid) {
        if (avoidPrimaryStorageUuids == null) {
            avoidPrimaryStorageUuids = new ArrayList<String>();
        }

        avoidPrimaryStorageUuids.add(priUuid);
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
