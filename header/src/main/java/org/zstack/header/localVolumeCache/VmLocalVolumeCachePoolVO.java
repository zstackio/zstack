package org.zstack.header.localVolumeCache;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid")
        }
)
public class VmLocalVolumeCachePoolVO extends ResourceVO implements ToInventory {

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column(length = 2048)
    private String metadata;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uuid")
    @NoView
    private VmLocalVolumeCachePoolCapacityVO capacity;

    @Column
    @Enumerated(EnumType.STRING)
    private VmLocalVolumeCachePoolState state;

    @Column
    @Enumerated(EnumType.STRING)
    private VmLocalVolumeCachePoolStatus status;

    @OneToMany(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "poolUuid", insertable = false, updatable = false)
    private Set<VmLocalVolumeCacheVO> caches;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public VmLocalVolumeCachePoolCapacityVO getCapacity() {
        return capacity;
    }

    public void setCapacity(VmLocalVolumeCachePoolCapacityVO capacity) {
        this.capacity = capacity;
    }

    public VmLocalVolumeCachePoolState getState() {
        return state;
    }

    public void setState(VmLocalVolumeCachePoolState state) {
        this.state = state;
    }

    public VmLocalVolumeCachePoolStatus getStatus() {
        return status;
    }

    public void setStatus(VmLocalVolumeCachePoolStatus status) {
        this.status = status;
    }

    public Set<VmLocalVolumeCacheVO> getCaches() {
        return caches;
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
