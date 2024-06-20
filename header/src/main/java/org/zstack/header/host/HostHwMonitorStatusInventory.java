package org.zstack.header.host;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/28 11:00
 */
@PythonClassInventory
@Inventory(mappingVOClass = HostHwMonitorStatusVO.class, collectionValueOfMethod = "valueOf1")
public class HostHwMonitorStatusInventory implements Serializable {
    private String uuid;
    private Boolean cpuStatus;
    private Boolean memoryStatus;
    private Boolean diskStatus;
    private Boolean fanStatus;
    private Boolean powerSupplyStatus;
    private Boolean raidStatus;
    private Boolean networkStatus;
    private Boolean gpuStatus;
    private Boolean temperatureStatus;

    protected HostHwMonitorStatusInventory(HostHwMonitorStatusVO vo) {
        this.setUuid(vo.getUuid());
        this.setCpuStatus(vo.getCpuStatus());
        this.setMemoryStatus(vo.getMemoryStatus());
        this.setDiskStatus(vo.getDiskStatus());
        this.setFanStatus(vo.getFanStatus());
        this.setPowerSupplyStatus(vo.getPowerSupplyStatus());
        this.setRaidStatus(vo.getRaidStatus());
        this.setNetworkStatus(vo.getNicStatus());
        this.setGpuStatus(vo.getGpuStatus());
        this.setTemperatureStatus(vo.getTemperatureStatus());
    }

    public static HostHwMonitorStatusInventory valueOf(HostHwMonitorStatusVO vo) {
        return new HostHwMonitorStatusInventory(vo);
    }

    public static List<HostHwMonitorStatusInventory> valueOf1(Collection<HostHwMonitorStatusVO> vos) {
        List<HostHwMonitorStatusInventory> invs = new ArrayList<HostHwMonitorStatusInventory>(vos.size());
        for (HostHwMonitorStatusVO vo : vos) {
            invs.add(HostHwMonitorStatusInventory.valueOf(vo));
        }
        return invs;
    }

    public HostHwMonitorStatusInventory() {
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getCpuStatus() {
        return cpuStatus;
    }

    public void setCpuStatus(Boolean cpuStatus) {
        this.cpuStatus = cpuStatus;
    }

    public Boolean getMemoryStatus() {
        return memoryStatus;
    }

    public void setMemoryStatus(Boolean memoryStatus) {
        this.memoryStatus = memoryStatus;
    }

    public Boolean getDiskStatus() {
        return diskStatus;
    }

    public void setDiskStatus(Boolean diskStatus) {
        this.diskStatus = diskStatus;
    }

    public Boolean getFanStatus() {
        return fanStatus;
    }

    public void setFanStatus(Boolean fanStatus) {
        this.fanStatus = fanStatus;
    }

    public Boolean getPowerSupplyStatus() {
        return powerSupplyStatus;
    }

    public void setPowerSupplyStatus(Boolean powerSupplyStatus) {
        this.powerSupplyStatus = powerSupplyStatus;
    }

    public Boolean getRaidStatus() {
        return raidStatus;
    }

    public void setRaidStatus(Boolean raidStatus) {
        this.raidStatus = raidStatus;
    }

    public Boolean getNetworkStatus() {
        return networkStatus;
    }

    public void setNetworkStatus(Boolean networkStatus) {
        this.networkStatus = networkStatus;
    }

    public Boolean getGpuStatus() {
        return gpuStatus;
    }

    public void setGpuStatus(Boolean gpuStatus) {
        this.gpuStatus = gpuStatus;
    }

    public Boolean getTemperatureStatus() {
        return temperatureStatus;
    }

    public void setTemperatureStatus(Boolean temperatureStatus) {
        this.temperatureStatus = temperatureStatus;
    }
}
