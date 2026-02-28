package org.zstack.header.localVolumeCache;

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
                @EntityGraph.Neighbour(type = VmLocalVolumeCachePoolVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class VmLocalVolumeCachePoolCapacityVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VmLocalVolumeCachePoolVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String uuid;

    @Column
    private long totalCapacity;

    @Column
    private long availableCapacity;

    @Column
    private long allocated;

    @Column
    private long dirty;

    public VmLocalVolumeCachePoolCapacityVO() {
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

    public long getAllocated() {
        return allocated;
    }

    public void setAllocated(long allocated) {
        this.allocated = allocated;
    }

    public long getDirty() {
        return dirty;
    }

    public void setDirty(long dirty) {
        this.dirty = dirty;
    }
}
