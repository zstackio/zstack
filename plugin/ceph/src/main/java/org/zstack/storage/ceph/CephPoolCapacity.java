package org.zstack.storage.ceph;

/**
 * Created by lining on 2018/4/1.
 */
public class CephPoolCapacity {
    String name;
    int replicatedSize;
    String securityPolicy;
    Float diskUtilization;
    Long availableCapacity;
    Long usedCapacity;
    Long totalCapacity;
    String relatedOsds;

    public String getRelatedOsds() {
        return relatedOsds;
    }

    public void setRelatedOsds(String relatedOsds) {
        this.relatedOsds = relatedOsds;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public int getReplicatedSize() {
        return replicatedSize;
    }

    public void setReplicatedSize(int replicatedSize) {
        this.replicatedSize = replicatedSize;
    }

    public Long getUsedCapacity() {
        return usedCapacity;
    }

    public void setUsedCapacity(Long usedCapacity) {
        this.usedCapacity = usedCapacity;
    }

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public void setDiskUtilization(Float diskUtilization) {
        this.diskUtilization = diskUtilization;
    }

    public Float getDiskUtilization() {
        return diskUtilization;
    }

    public void setSecurityPolicy(String securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    public String getSecurityPolicy() {
        return securityPolicy;
    }
}
