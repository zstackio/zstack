package org.zstack.header.localVolumeCache;

import org.hibernate.Hibernate;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

@Inventory(mappingVOClass = VmLocalVolumeCachePoolVO.class)
@PythonClassInventory
public class VmLocalVolumeCachePoolInventory implements Serializable {
    private String uuid;
    private String hostUuid;
    private String name;
    private String description;
    private LinkedHashMap metadata;

    @Queryable(mappingClass = VmLocalVolumeCachePoolCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "totalCapacity"))
    private long totalCapacity;

    @Queryable(mappingClass = VmLocalVolumeCachePoolCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "availableCapacity"))
    private long availableCapacity;

    @Queryable(mappingClass = VmLocalVolumeCachePoolCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "allocated"))
    private long allocated;

    @Queryable(mappingClass = VmLocalVolumeCachePoolCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "dirty"))
    private long dirty;

    private VmLocalVolumeCachePoolState state;
    private VmLocalVolumeCachePoolStatus status;
    private Set<VmLocalVolumeCacheInventory> caches;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmLocalVolumeCachePoolInventory valueOf(VmLocalVolumeCachePoolVO vo) {
        VmLocalVolumeCachePoolInventory inv = new VmLocalVolumeCachePoolInventory();
        inv.setUuid(vo.getUuid());
        inv.setHostUuid(vo.getHostUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        if (vo.getMetadata() != null) {
            inv.setMetadata(JSONObjectUtil.toObject(vo.getMetadata(), LinkedHashMap.class));
        }
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
        if(vo.getCaches() != null  && Hibernate.isInitialized(vo.getCaches())) {
            Set<VmLocalVolumeCacheInventory> cacheInventories = new HashSet<>();
            for (VmLocalVolumeCacheVO cacheVO : vo.getCaches()) {
                cacheInventories.add(cacheVO.toInventory());
            }
            inv.setCaches(cacheInventories);
        }
        return inv;
    }

    public static List<VmLocalVolumeCachePoolInventory> valueOf(Collection<VmLocalVolumeCachePoolVO> vos) {
        List<VmLocalVolumeCachePoolInventory> invs = new ArrayList<>();
        for (VmLocalVolumeCachePoolVO vo : vos) {
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

    public LinkedHashMap getMetadata() {
        return metadata;
    }

    public void setMetadata(LinkedHashMap metadata) {
        this.metadata = metadata;
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

    public Set<VmLocalVolumeCacheInventory> getCaches() {
        return caches;
    }

    public void setCaches(Set<VmLocalVolumeCacheInventory> caches) {
        this.caches = caches;
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

