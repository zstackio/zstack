package org.zstack.header.server;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Truth-table entity for physical server capacity (Phase 2B).
 *
 * 10 HostCapacityVO-aligned columns + 6 new governance columns + timestamps.
 * In Phase 2B, HostCapacityVO becomes a MySQL VIEW backed by this table;
 * until then this VO is written directly by the capacity write path (U4-U6).
 *
 * Index rationale (2026-04-22):
 * - PK on uuid (shared with PhysicalServerVO) — 1:1 relationship, no extra FK index needed.
 * - availableCpu / availableMemory: allocator hot-path reads (filter on available >= requested).
 * - capacityState: used by recalculate background job to find Stale rows.
 */
@Entity
@Table(name = "PhysicalServerCapacityVO",
        indexes = {
                @javax.persistence.Index(name = "idx_ps_cap_avail_cpu",    columnList = "availableCpu"),
                @javax.persistence.Index(name = "idx_ps_cap_avail_mem",    columnList = "availableMemory"),
                @javax.persistence.Index(name = "idx_ps_cap_state",        columnList = "capacityState")
        })
public class PhysicalServerCapacityVO {

    // -----------------------------------------------------------------------
    // PK — shared uuid with PhysicalServerVO (1:1 via FK)
    // -----------------------------------------------------------------------

    @Id
    @Column(length = 32, nullable = false)
    @ForeignKey(parentEntityClass = PhysicalServerVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String uuid;

    // -----------------------------------------------------------------------
    // 10 HostCapacityVO-aligned columns
    // -----------------------------------------------------------------------

    /** Total logical memory (bytes) after overprovisioning factor is applied. */
    @Column
    private long totalMemory;

    /** Total logical CPU (mhz or cores × ratio) after overprovisioning factor. */
    @Column
    private long totalCpu;

    /** Physical CPU count (pre-overprovisioning). Matches HostCapacityVO.cpuNum type (long). */
    @Column
    private long cpuNum;

    @Column
    private int cpuSockets;

    @Column
    private int cpuCoreNum;

    @Column
    private long availableMemory;

    @Column
    private long availableCpu;

    @Column
    private long totalPhysicalMemory;

    @Column
    private long availablePhysicalMemory;

    // -----------------------------------------------------------------------
    // 6 new governance columns
    // -----------------------------------------------------------------------

    @Column
    private float cpuOverprovisioningRatio = 1.0f;

    @Column
    private float memoryOverprovisioningRatio = 1.0f;

    @Column
    private long reservedMemory = 0L;

    @Column
    private long totalDisk = 0L;

    @Column
    private long availableDisk = 0L;

    @Column(length = 32)
    @Enumerated(EnumType.STRING)
    private PhysicalServerCapacityState capacityState;

    // -----------------------------------------------------------------------
    // Timestamps (ZStack convention — mirrors PhysicalServerAO pattern)
    // -----------------------------------------------------------------------

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PrePersist
    private void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createDate == null) {
            createDate = now;
        }
        if (lastOpDate == null) {
            lastOpDate = now;
        }
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    // -----------------------------------------------------------------------
    // Getters / Setters
    // -----------------------------------------------------------------------

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

    public long getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(long cpuNum) {
        this.cpuNum = cpuNum;
    }

    public int getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(int cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public int getCpuCoreNum() {
        return cpuCoreNum;
    }

    public void setCpuCoreNum(int cpuCoreNum) {
        this.cpuCoreNum = cpuCoreNum;
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

    public long getTotalPhysicalMemory() {
        return totalPhysicalMemory;
    }

    public void setTotalPhysicalMemory(long totalPhysicalMemory) {
        this.totalPhysicalMemory = totalPhysicalMemory;
    }

    public long getAvailablePhysicalMemory() {
        return availablePhysicalMemory;
    }

    public void setAvailablePhysicalMemory(long availablePhysicalMemory) {
        this.availablePhysicalMemory = availablePhysicalMemory;
    }

    public float getCpuOverprovisioningRatio() {
        return cpuOverprovisioningRatio;
    }

    public void setCpuOverprovisioningRatio(float cpuOverprovisioningRatio) {
        this.cpuOverprovisioningRatio = cpuOverprovisioningRatio;
    }

    public float getMemoryOverprovisioningRatio() {
        return memoryOverprovisioningRatio;
    }

    public void setMemoryOverprovisioningRatio(float memoryOverprovisioningRatio) {
        this.memoryOverprovisioningRatio = memoryOverprovisioningRatio;
    }

    public long getReservedMemory() {
        return reservedMemory;
    }

    public void setReservedMemory(long reservedMemory) {
        this.reservedMemory = reservedMemory;
    }

    public long getTotalDisk() {
        return totalDisk;
    }

    public void setTotalDisk(long totalDisk) {
        this.totalDisk = totalDisk;
    }

    public long getAvailableDisk() {
        return availableDisk;
    }

    public void setAvailableDisk(long availableDisk) {
        this.availableDisk = availableDisk;
    }

    public PhysicalServerCapacityState getCapacityState() {
        return capacityState;
    }

    public void setCapacityState(PhysicalServerCapacityState capacityState) {
        this.capacityState = capacityState;
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
