package org.zstack.header.volumeCache;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid")
        }
)
public class HostCacheStoreVO extends ResourceVO implements ToInventory {

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column(length = 255)
    private String mountPoint;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = HostCacheStoreDeviceRefsConverter.class)
    private List<HostCacheStoreDeviceRef> devices;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uuid")
    @NoView
    private HostCacheStoreCapacityVO capacity;

    @Column
    @Enumerated(EnumType.STRING)
    private HostCacheStoreState state;

    @Column
    @Enumerated(EnumType.STRING)
    private HostCacheStoreStatus status;

    @OneToMany(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "poolUuid", insertable = false, updatable = false)
    private Set<VolumeCacheVO> caches;

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

    public String getMountPoint() {
        return mountPoint;
    }

    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    public List<HostCacheStoreDeviceRef> getDevices() {
        return devices == null ? new ArrayList<>() : devices;
    }

    public void setDevices(List<HostCacheStoreDeviceRef> devices) {
        this.devices = devices;
    }

    public HostCacheStoreCapacityVO getCapacity() {
        return capacity;
    }

    public void setCapacity(HostCacheStoreCapacityVO capacity) {
        this.capacity = capacity;
    }

    public HostCacheStoreState getState() {
        return state;
    }

    public void setState(HostCacheStoreState state) {
        this.state = state;
    }

    public HostCacheStoreStatus getStatus() {
        return status;
    }

    public void setStatus(HostCacheStoreStatus status) {
        this.status = status;
    }

    public Set<VolumeCacheVO> getCaches() {
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
