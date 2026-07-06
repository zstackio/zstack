package org.zstack.header.volumeCache;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostCacheStoreVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class HostCacheStoreCapacityVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = HostCacheStoreVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String uuid;

    @Column
    private long totalCapacity;

    @Column
    private long availableCapacity;

    @Column
    private long totalPhysicalCapacity;

    @Column
    private long availablePhysicalCapacity;

    @Column
    private long systemUsedCapacity;

    public HostCacheStoreCapacityVO() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public long getSystemUsedCapacity() {
        return systemUsedCapacity;
    }

    public void setSystemUsedCapacity(long systemUsedCapacity) {
        this.systemUsedCapacity = systemUsedCapacity;
    }
}
