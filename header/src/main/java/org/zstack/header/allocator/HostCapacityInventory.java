package org.zstack.header.allocator;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = HostCapacityVO.class)
public class HostCapacityInventory {
    private String uuid;
    private Long totalMemory;
    private Long totalCpu;
    private Long availableMemory;
    private Long availableCpu;

    public HostCapacityInventory valueOf(HostCapacityVO vo) {
        HostCapacityInventory inv = new HostCapacityInventory();
        inv.setUuid(vo.getUuid());
        inv.setTotalMemory(vo.getTotalMemory());
        inv.setAvailableMemory(vo.getAvailableMemory());
        inv.setTotalCpu(vo.getTotalCpu());
        inv.setAvailableCpu(vo.getAvailableCpu());
        return inv;
    }

    public List<HostCapacityInventory> valueOf(Collection<HostCapacityVO> vos) {
        List<HostCapacityInventory> invs = new ArrayList<HostCapacityInventory>();
        for (HostCapacityVO vo : vos) {
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

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getTotalCpu() {
        return totalCpu;
    }

    public void setTotalCpu(long totalCpu) {
        this.totalCpu = totalCpu;
    }

    public long getAvailableMemory() {
        return availableMemory;
    }

    public void setAvailableMemory(long availableMemory) {
        this.availableMemory = availableMemory;
    }

    public long getAvailableCpu() {
        return availableCpu;
    }

    public void setAvailableCpu(long availableCpu) {
        this.availableCpu = availableCpu;
    }
}
