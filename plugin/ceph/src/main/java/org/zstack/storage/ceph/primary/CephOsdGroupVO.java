package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:18 2022/8/1
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = PrimaryStorageVO.class, joinColumn = "primaryStorageUuid")
})
public class CephOsdGroupVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    @Column
    private String osds;

    @Column
    private long availableCapacity;

    @Column
    private long availablePhysicalCapacity;

    @Column
    private long totalPhysicalCapacity;

    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public CephOsdGroupVO() {
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getOsds() {
        return osds;
    }

    public void setOsds(String osds) {
        this.osds = osds;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
