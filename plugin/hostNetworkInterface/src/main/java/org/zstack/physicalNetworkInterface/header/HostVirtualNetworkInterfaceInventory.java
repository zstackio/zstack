package org.zstack.physicalNetworkInterface.header;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = HostVirtualNetworkInterfaceVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "hostNetworkInterface", inventoryClass = HostNetworkInterfaceInventory.class,
                foreignKey = "hostNetworkInterfaceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
})
public class HostVirtualNetworkInterfaceInventory implements Serializable {
    private String uuid;

    private String description;

    private String hostUuid;

    private String hostNetworkInterfaceUuid;

    private String vmInstanceUuid;

    private HostVirtualNetworkInterfaceStatus status;

    private String vendorId;

    private String deviceId;

    private String subvendorId;

    private String subdeviceId;

    private String pciDeviceAddress;

    private String metaData;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public HostVirtualNetworkInterfaceInventory() {
    }

    public HostVirtualNetworkInterfaceInventory(HostVirtualNetworkInterfaceVO vo) {
        this.uuid = vo.getUuid();
        this.description = vo.getDescription();
        this.hostUuid = vo.getHostUuid();
        this.vmInstanceUuid = vo.getVmInstanceUuid();
        this.status = vo.getStatus();
        this.vendorId = vo.getVendorId();
        this.deviceId = vo.getDeviceId();
        this.subvendorId = vo.getSubvendorId();
        this.subdeviceId = vo.getSubdeviceId();
        this.pciDeviceAddress = vo.getPciDeviceAddress();
        this.metaData = vo.getMetaData();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static HostVirtualNetworkInterfaceInventory valueOf(HostVirtualNetworkInterfaceVO vo) {
        return new HostVirtualNetworkInterfaceInventory(vo);
    }

    public static List<HostVirtualNetworkInterfaceInventory> valueOf(Collection<HostVirtualNetworkInterfaceVO> vos) {
        List<HostVirtualNetworkInterfaceInventory> invs = new ArrayList<>();
        for (HostVirtualNetworkInterfaceVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHostNetworkInterfaceUuid() {
        return hostNetworkInterfaceUuid;
    }

    public void setHostNetworkInterfaceUuid(String hostNetworkInterfaceUuid) {
        this.hostNetworkInterfaceUuid = hostNetworkInterfaceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public HostVirtualNetworkInterfaceStatus getStatus() {
        return status;
    }

    public void setStatus(HostVirtualNetworkInterfaceStatus status) {
        this.status = status;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSubvendorId() {
        return subvendorId;
    }

    public void setSubvendorId(String subvendorId) {
        this.subvendorId = subvendorId;
    }

    public String getSubdeviceId() {
        return subdeviceId;
    }

    public void setSubdeviceId(String subdeviceId) {
        this.subdeviceId = subdeviceId;
    }

    public String getPciDeviceAddress() {
        return pciDeviceAddress;
    }

    public void setPciDeviceAddress(String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
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
