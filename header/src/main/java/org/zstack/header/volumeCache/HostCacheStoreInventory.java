package org.zstack.header.volumeCache;

import org.hibernate.Hibernate;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@Inventory(mappingVOClass = HostCacheStoreVO.class)
@PythonClassInventory
public class HostCacheStoreInventory implements Serializable {
    private String uuid;
    private String hostUuid;
    private String name;
    private String description;
    private String mountPoint;

    @Queryable(mappingClass = HostCacheStoreCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "totalCapacity"))
    private long totalCapacity;

    @Queryable(mappingClass = HostCacheStoreCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "availableCapacity"))
    private long availableCapacity;

    @Queryable(mappingClass = HostCacheStoreCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "allocated"))
    private long allocated;

    @Queryable(mappingClass = HostCacheStoreCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "dirty"))
    private long dirty;

    private HostCacheStoreState state;
    private HostCacheStoreStatus status;
    private List<HostCacheStoreDeviceInventory> devices;
    private Set<VolumeCacheInventory> caches;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static HostCacheStoreInventory valueOf(HostCacheStoreVO vo) {
        HostCacheStoreInventory inv = new HostCacheStoreInventory();
        inv.setUuid(vo.getUuid());
        inv.setHostUuid(vo.getHostUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setMountPoint(vo.getMountPoint());
        if (vo.getCapacity() != null) {
            inv.setTotalCapacity(vo.getCapacity().getTotalCapacity());
            inv.setAvailableCapacity(vo.getCapacity().getAvailableCapacity());
            inv.setAllocated(vo.getCapacity().getAllocated());
            inv.setDirty(vo.getCapacity().getDirty());
        }
        inv.setState(vo.getState());
        inv.setStatus(vo.getStatus());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        // persisted-only device refs (path + wwid). Callers that need live
        // vendor/model/serial/size must call the host block-devices API and
        // join on wwid (preferred) or path.
        List<HostCacheStoreDeviceInventory> deviceInvs = new ArrayList<>();
        if (vo.getDevices() != null) {
            for (HostCacheStoreDeviceRef ref : vo.getDevices()) {
                deviceInvs.add(HostCacheStoreDeviceInventory.valueOf(ref));
            }
        }
        inv.setDevices(deviceInvs);
        if (vo.getCaches() != null  && Hibernate.isInitialized(vo.getCaches())) {
            Set<VolumeCacheInventory> cacheInventories = new HashSet<>();
            for (VolumeCacheVO cacheVO : vo.getCaches()) {
                cacheInventories.add(cacheVO.toInventory());
            }
            inv.setCaches(cacheInventories);
        }
        return inv;
    }

    public static List<HostCacheStoreInventory> valueOf(Collection<HostCacheStoreVO> vos) {
        List<HostCacheStoreInventory> invs = new ArrayList<>();
        for (HostCacheStoreVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public Set<VolumeCacheInventory> getCaches() {
        return caches;
    }

    public void setCaches(Set<VolumeCacheInventory> caches) {
        this.caches = caches;
    }

    public List<HostCacheStoreDeviceInventory> getDevices() {
        return devices;
    }

    public void setDevices(List<HostCacheStoreDeviceInventory> devices) {
        this.devices = devices;
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

