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
    private Integer cpuNum;
    private Integer cpuSockets;
    private Long availableMemory;
    private Long availableCpu;
    private Long totalPhysicalMemory;
    private Long availablePhysicalMemory;

    public HostCapacityInventory valueOf(HostCapacityVO vo) {
        HostCapacityInventory inv = new HostCapacityInventory();
        inv.setUuid(vo.getUuid());
        inv.setTotalMemory(vo.getTotalMemory());
        inv.setAvailableMemory(vo.getAvailableMemory());
        inv.setTotalCpu(vo.getTotalCpu());
        inv.setAvailableCpu(vo.getAvailableCpu());
        inv.setAvailablePhysicalMemory(vo.getAvailablePhysicalMemory());
        inv.setTotalPhysicalMemory(vo.getTotalPhysicalMemory());
        inv.setCpuNum(vo.getCpuNum());
        inv.setCpuSockets(vo.getCpuSockets());
        return inv;
    }

    public List<HostCapacityInventory> valueOf(Collection<HostCapacityVO> vos) {
        List<HostCapacityInventory> invs = new ArrayList<HostCapacityInventory>();
        for (HostCapacityVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getTotalPhysicalMemory() {
        return totalPhysicalMemory;
    }

    public void setTotalPhysicalMemory(Long totalPhysicalMemory) {
        this.totalPhysicalMemory = totalPhysicalMemory;
    }

    public Long getAvailablePhysicalMemory() {
        return availablePhysicalMemory;
    }

    public void setAvailablePhysicalMemory(Long availablePhysicalMemory) {
        this.availablePhysicalMemory = availablePhysicalMemory;
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
