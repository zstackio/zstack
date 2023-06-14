package org.zstack.physicalNetworkInterface.header;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.NicVirtStatus;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
@AutoDeleteTag
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid"),
        @SoftDeletionCascade(parent = VmInstanceEO.class, joinColumn = "vmInstanceUuid")
})
@org.zstack.header.vo.EntityGraph(
        parents = {
                @org.zstack.header.vo.EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
                @org.zstack.header.vo.EntityGraph.Neighbour(type = HostNetworkInterfaceVO.class, myField = "parentUuid", targetField = "uuid"),
        },

        friends = {
                @org.zstack.header.vo.EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
        }
)
public class HostVirtualNetworkInterfaceVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String description;

    @Index
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Index
    @Column
    @ForeignKey(parentEntityClass = HostNetworkInterfaceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostNetworkInterfaceUuid;

    // the vm instance that this pci device attached to, null if not attached
    @Column
    @ForeignKey(parentEntityClass = VmInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = VmNicVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmNicUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private HostVirtualNetworkInterfaceStatus status;


    // the unique id of the pci device vendor, see `lspci -mmnnv`
    @Column
    private String vendorId;

    // see `lspci -mmnnv`
    @Column
    private String deviceId;

    // see `lspci -mmnnv`
    @Column
    private String subvendorId;

    // see `lspci -mmnnv`
    @Column
    private String subdeviceId;

    // see `lspci -mmnnv`
    @Column
    private String pciDeviceAddress;

    @Column
    private String metaData;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return null;
    }

    @Override
    public void setAccountUuid(String accountUuid) {

    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
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
