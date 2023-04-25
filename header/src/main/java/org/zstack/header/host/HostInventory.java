package org.zstack.header.host;

import org.zstack.header.allocator.HostCapacityInventory;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.zone.ZoneInventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @inventory inventory for host. Depending on hypervisor, the inventory may have extra fields
 * @example {
 * "inventory": {
 * "zoneUuid": "2893ce85c43d4a3a8d78f414da39966e",
 * "name": "host1-192.168.0.203",
 * "uuid": "43673938584447b2a29ab3d53f9d88d3",
 * "clusterUuid": "8524072a4274403892bcc5b1972c2576",
 * "description": "Test",
 * "managementIp": "192.168.0.203",
 * "hypervisorType": "KVM",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "Feb 28, 2014 6:49:24 PM",
 * "lastOpDate": "Feb 28, 2014 6:49:24 PM"
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = HostVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "clusterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "hostUuid")
})
public class HostInventory implements Serializable {
    /**
     * @desc uuid of zone this host belongs to
     */
    private String zoneUuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc host uuid
     */
    private String uuid;
    /**
     * @desc uuid of cluster this host belongs to
     */
    private String clusterUuid;
    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;
    /**
     * @desc IPv4 address of host's management nic
     * <p>
     * .. note:: This field could be DNS name
     */
    private String managementIp;
    /**
     * @desc type of hypervisor installed on the host
     */
    @TypeField
    private String hypervisorType;
    /**
     * @desc - Disabled: no vm can be created on this host
     * - PreMaintenance: host is in middle way of entering state Maintenance
     * - Maintenance: host is ready for maintenance work, for example, upgrading CPU/memory. No vm can be created on this host
     * <p>
     * .. note:: PreMaintenance is an ephemeral state after admin switches host state to Maintenance. During entering
     * Maintenance, zstack will try to live migrate all running vm to other hosts, vm failed to migrate will be stopped.
     * In maintenance mode, host will not receive any commands from zstack, admin can safely shut it off and do whatever upgrade
     * work
     * @choices - Enabled
     * - Disabled
     * - PreMaintenance
     * - Maintenance
     */
    private String state;
    /**
     * @desc - Connecting: zstack management server is trying to establish connection to hypervisor agent
     * - Connected: connection to hypervisor agent has been established
     * - Disconnected: connection to hypervisor agent is broken, no commands can be sent to hypervisor and no vm can be created
     * on this host
     * @choices - Connecting
     * - Connected
     * - Disconnected
     */
    private String status;

    @Queryable(mappingClass = HostCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "totalCpu"))
    private Long totalCpuCapacity;

    @Queryable(mappingClass = HostCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "availableCpu"))
    private Long availableCpuCapacity;

    @Queryable(mappingClass = HostCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "cpuSockets"))
    private Integer cpuSockets;

    @Queryable(mappingClass = HostCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "totalMemory"))
    private Long totalMemoryCapacity;

    @Queryable(mappingClass = HostCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "availableMemory"))
    private Long availableMemoryCapacity;

    @Queryable(mappingClass = HostCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "cpuNum"))
    private Integer cpuNum;

    private String architecture;

    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    protected HostInventory(HostVO vo) {
        this.setStatus(vo.getStatus().toString());
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setHypervisorType(vo.getHypervisorType());
        this.setLastOpDate(vo.getLastOpDate());
        this.setManagementIp(vo.getManagementIp());
        this.setName(vo.getName());
        this.setState(vo.getState().toString());
        this.setUuid(vo.getUuid());
        this.setZoneUuid(vo.getZoneUuid());
        this.setClusterUuid(vo.getClusterUuid());
        this.setArchitecture(vo.getArchitecture());
        if (vo.getCapacity() != null) {
            this.setTotalCpuCapacity(vo.getCapacity().getTotalCpu());
            this.setAvailableCpuCapacity(vo.getCapacity().getAvailableCpu());
            this.setTotalMemoryCapacity(vo.getCapacity().getTotalMemory());
            this.setAvailableMemoryCapacity(vo.getCapacity().getAvailableMemory());
            this.setCpuSockets(vo.getCapacity().getCpuSockets());
            this.setCpuNum(vo.getCapacity().getCpuNum());
        }
    }

    public HostInventory() {
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public static HostInventory valueOf(HostVO vo) {
        return new HostInventory(vo);
    }

    public static List<HostInventory> valueOf(Collection<HostVO> vos) {
        List<HostInventory> invs = new ArrayList<HostInventory>(vos.size());
        for (HostVO vo : vos) {
            invs.add(HostInventory.valueOf(vo));
        }
        return invs;
    }

    public Long getTotalCpuCapacity() {
        return totalCpuCapacity;
    }

    public void setTotalCpuCapacity(Long totalCpuCapacity) {
        this.totalCpuCapacity = totalCpuCapacity;
    }

    public Long getAvailableCpuCapacity() {
        return availableCpuCapacity;
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public void setAvailableCpuCapacity(Long availableCpuCapacity) {
        this.availableCpuCapacity = availableCpuCapacity;
    }

    public Long getTotalMemoryCapacity() {
        return totalMemoryCapacity;
    }

    public void setTotalMemoryCapacity(Long totalMemoryCapacity) {
        this.totalMemoryCapacity = totalMemoryCapacity;
    }

    public Long getAvailableMemoryCapacity() {
        return availableMemoryCapacity;
    }

    public void setAvailableMemoryCapacity(Long availableMemoryCapacity) {
        this.availableMemoryCapacity = availableMemoryCapacity;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }
}
