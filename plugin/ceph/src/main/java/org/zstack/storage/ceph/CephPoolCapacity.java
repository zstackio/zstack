package org.zstack.storage.ceph;

/**
 * Created by lining on 2018/4/1.
 */
public class CephPoolCapacity {
    String name;
    int replicatedSize;
    Long availableCapacity;
    Long usedCapacity;

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
}
