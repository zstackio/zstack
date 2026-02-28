package org.zstack.header.localVolumeCache;

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
public class VmLocalVolumeCacheVO extends ResourceVO implements ToInventory {

    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String volumeUuid;

    @Column
    @ForeignKey(parentEntityClass = VmLocalVolumeCachePoolVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String poolUuid;

    @Column(length = 2048)
    private String installPath;

    @Column
    @Enumerated(EnumType.STRING)
    private VmLocalVolumeCacheMode cacheMode;

    @Column
    @Enumerated(EnumType.STRING)
    private VmLocalVolumeCacheState state;

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

    public VmLocalVolumeCacheMode getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(VmLocalVolumeCacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public VmLocalVolumeCacheState getState() {
        return state;
    }

    public void setState(VmLocalVolumeCacheState state) {
        this.state = state;
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
