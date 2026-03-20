package org.zstack.header.localVolumeCache;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VmLocalVolumeCachePoolCapacityVO.class)
@PythonClassInventory
public class VmLocalVolumeCachePoolCapacityInventory implements Serializable {
    private String uuid;
    private long totalCapacity;
    private long availableCapacity;
    private long allocated;
    private long dirty;

    public static VmLocalVolumeCachePoolCapacityInventory valueOf(VmLocalVolumeCachePoolCapacityVO vo) {
        if (vo == null) {
            return null;
        }

        VmLocalVolumeCachePoolCapacityInventory inv = new VmLocalVolumeCachePoolCapacityInventory();
        inv.setUuid(vo.getUuid());
        inv.setTotalCapacity(vo.getTotalCapacity());
        inv.setAvailableCapacity(vo.getAvailableCapacity());
        inv.setAllocated(vo.getAllocated());
        inv.setDirty(vo.getDirty());
        return inv;
    }

    public static List<VmLocalVolumeCachePoolCapacityInventory> valueOf(Collection<VmLocalVolumeCachePoolCapacityVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return new ArrayList<>();
        }
        List<VmLocalVolumeCachePoolCapacityInventory> invs = new ArrayList<>(vos.size());

        for (VmLocalVolumeCachePoolCapacityVO vo : vos) {
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
