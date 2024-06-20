package org.zstack.header.host;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/28 10:45
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class HostHwMonitorStatusVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;

    @Column
    private Boolean cpuStatus;

    @Column
    private Boolean memoryStatus;

    @Column
    private Boolean diskStatus;

    @Column
    private Boolean nicStatus;

    @Column
    private Boolean gpuStatus;

    @Column
    private Boolean powerSupplyStatus;

    @Column
    private Boolean fanStatus;

    @Column
    private Boolean raidStatus;

    @Column
    private Boolean temperatureStatus;

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

    public Boolean getNicStatus() {
        return nicStatus;
    }

    public void setNicStatus(Boolean nicStatus) {
        this.nicStatus = nicStatus;
    }

    public Boolean getGpuStatus() {
        return gpuStatus;
    }

    public void setGpuStatus(Boolean gpuStatus) {
        this.gpuStatus = gpuStatus;
    }

    public Boolean getPowerSupplyStatus() {
        return powerSupplyStatus;
    }

    public void setPowerSupplyStatus(Boolean powerSupplyStatus) {
        this.powerSupplyStatus = powerSupplyStatus;
    }

    public Boolean getFanStatus() {
        return fanStatus;
    }

    public void setFanStatus(Boolean fanStatus) {
        this.fanStatus = fanStatus;
    }

    public Boolean getRaidStatus() {
        return raidStatus;
    }

    public void setRaidStatus(Boolean raidStatus) {
        this.raidStatus = raidStatus;
    }

    public Boolean getTemperatureStatus() {
        return temperatureStatus;
    }

    public void setTemperatureStatus(Boolean temperatureStatus) {
        this.temperatureStatus = temperatureStatus;
    }
}
