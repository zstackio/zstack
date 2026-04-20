package org.zstack.header.volumeCache;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.volume.VolumeEO;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = "volumeUuid")
})
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "volumeUuid", targetField = "uuid")
        }
)
public class VolumeCacheVO extends ResourceVO implements ToInventory {

    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String volumeUuid;

    @Column
    @ForeignKey(parentEntityClass = HostCacheStoreVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String poolUuid;

    @Column(length = 2048)
    private String installPath;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeCacheMode cacheMode;

    @Column
    @Enumerated(EnumType.STRING)
    private VolumeCacheStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public VolumeCacheMode getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(VolumeCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public VolumeCacheStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeCacheStatus status) {
        this.status = status;
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
