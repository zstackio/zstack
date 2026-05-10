package org.zstack.header.server;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

/**
 * U16 (NB-19): unified flat-summary hardware-info row, one per PhysicalServer.
 * Sibling table to the device-level {@link PhysicalServerHardwareDetailVO}; this VO
 * holds the aggregated summary that {@code PhysicalServerHardwareService.discoverHardware()}
 * fans into via mergeNonNull.
 *
 * <p>Schema is owned by U14's {@code V5.5.18__schema.sql}; in unit test (hbm2ddl) the
 * JPA annotations are sufficient to auto-create the table.</p>
 */
@Entity
@Table(name = "PhysicalServerHardwareInfoVO")
public class PhysicalServerHardwareInfoVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = PhysicalServerVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String serverUuid;

    @Column
    private String manufacturer;

    @Column
    private String model;

    @Column
    private String serialNumber;

    @Column
    private String biosVersion;

    @Column
    private String cpuModel;

    @Column
    private Integer cpuSockets;

    @Column
    private Integer cpuCores;

    @Column
    private String cpuArchitecture;

    @Column
    private Long totalMemoryBytes;

    @Column
    private Integer memoryModuleCount;

    @Column
    private Long totalDiskBytes;

    @Column
    private Integer diskCount;

    @Column
    private Integer nicCount;

    @Column
    private Integer gpuCount;

    @Column
    private String healthStatus;

    /**
     * P1-3: first-writer-wins. The first {@code discoverHardware} pass that produced any
     * non-null carrier field writes its winning source here (per the in-pass ordering
     * IPMI_FRU &gt; KVM_AGENT &gt; K8S_NODEINFO). Subsequent passes refresh data columns
     * and {@link #lastDiscoverDate} but do NOT overwrite this value — it is a stable
     * "who first identified this host" tag, not a churning "currently primary contributor"
     * signal. Operators wanting per-field provenance should look at lastDiscoverDate +
     * field-level audit (out of scope for v5.5.18).
     */
    @Column
    private String discoverSource;

    @Column
    private Timestamp lastDiscoverDate;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getBiosVersion() {
        return biosVersion;
    }

    public void setBiosVersion(String biosVersion) {
        this.biosVersion = biosVersion;
    }

    public String getCpuModel() {
        return cpuModel;
    }

    public void setCpuModel(String cpuModel) {
        this.cpuModel = cpuModel;
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public String getCpuArchitecture() {
        return cpuArchitecture;
    }

    public void setCpuArchitecture(String cpuArchitecture) {
        this.cpuArchitecture = cpuArchitecture;
    }

    public Long getTotalMemoryBytes() {
        return totalMemoryBytes;
    }

    public void setTotalMemoryBytes(Long totalMemoryBytes) {
        this.totalMemoryBytes = totalMemoryBytes;
    }

    public Integer getMemoryModuleCount() {
        return memoryModuleCount;
    }

    public void setMemoryModuleCount(Integer memoryModuleCount) {
        this.memoryModuleCount = memoryModuleCount;
    }

    public Long getTotalDiskBytes() {
        return totalDiskBytes;
    }

    public void setTotalDiskBytes(Long totalDiskBytes) {
        this.totalDiskBytes = totalDiskBytes;
    }

    public Integer getDiskCount() {
        return diskCount;
    }

    public void setDiskCount(Integer diskCount) {
        this.diskCount = diskCount;
    }

    public Integer getNicCount() {
        return nicCount;
    }

    public void setNicCount(Integer nicCount) {
        this.nicCount = nicCount;
    }

    public Integer getGpuCount() {
        return gpuCount;
    }

    public void setGpuCount(Integer gpuCount) {
        this.gpuCount = gpuCount;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getDiscoverSource() {
        return discoverSource;
    }

    public void setDiscoverSource(String discoverSource) {
        this.discoverSource = discoverSource;
    }

    public Timestamp getLastDiscoverDate() {
        return lastDiscoverDate;
    }

    public void setLastDiscoverDate(Timestamp lastDiscoverDate) {
        this.lastDiscoverDate = lastDiscoverDate;
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
