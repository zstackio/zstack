package org.zstack.header.volumeCache;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = HostCacheStoreCapacityVO.class)
@PythonClassInventory
public class HostCacheStoreCapacityInventory implements Serializable {
    private String uuid;
    private long totalCapacity;
    private long availableCapacity;
    private long allocated;
    private long dirty;

    public static HostCacheStoreCapacityInventory valueOf(HostCacheStoreCapacityVO vo) {
        if (vo == null) {
            return null;
        }

        HostCacheStoreCapacityInventory inv = new HostCacheStoreCapacityInventory();
        inv.setUuid(vo.getUuid());
        inv.setTotalCapacity(vo.getTotalCapacity());
        inv.setAvailableCapacity(vo.getAvailableCapacity());
        inv.setAllocated(vo.getAllocated());
        inv.setDirty(vo.getDirty());
        return inv;
    }

    public static List<HostCacheStoreCapacityInventory> valueOf(Collection<HostCacheStoreCapacityVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return new ArrayList<>();
        }
        List<HostCacheStoreCapacityInventory> invs = new ArrayList<>(vos.size());

        for (HostCacheStoreCapacityVO vo : vos) {
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
