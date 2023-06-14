package org.zstack.physicalNetworkInterface.header;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountConstant;
import org.zstack.pciDevice.PciDeviceTO;
import org.zstack.pciDevice.PciDeviceType;
import org.zstack.pciDevice.PciDeviceVO;
import org.zstack.pciDevice.PciDeviceVO_;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecState;
import org.zstack.pciDevice.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HostVirtualNetworkInterfaceTO {
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

    private String parentAddress;
    public HostVirtualNetworkInterfaceTO() {
    }

    public HostVirtualNetworkInterfaceTO(HostVirtualNetworkInterfaceVO vo) {
        this.setUuid(vo.getUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setVendorId(vo.getVendorId());
        this.setDeviceId(vo.getDeviceId());
        this.setSubvendorId(vo.getSubvendorId());
        this.setSubdeviceId(vo.getSubdeviceId());
        this.setPciDeviceAddress(vo.getPciDeviceAddress());
    }

    public static HostVirtualNetworkInterfaceTO valueOf(HostVirtualNetworkInterfaceVO vo) {
        return new HostVirtualNetworkInterfaceTO(vo);
    }

    public static List<HostVirtualNetworkInterfaceTO> valueOf(List<HostVirtualNetworkInterfaceVO> vos) {
        List<HostVirtualNetworkInterfaceTO> tos = new ArrayList<>();

        for (HostVirtualNetworkInterfaceVO vo : vos) {
            tos.add(new HostVirtualNetworkInterfaceTO(vo));
        }
        return tos;
    }

    public static void update(HostVirtualNetworkInterfaceVO vo, HostVirtualNetworkInterfaceTO to) {
       /* vo.setName(StringUtils.isEmpty(to.getName()) ? vo.getName() : to.getName() + "_" + vo.getUuid().substring(0, 8));
        vo.setDescription(StringUtils.isEmpty(to.getDescription()) ? vo.getDescription() : to.getDescription());
        vo.setType(PciDeviceType.valueOf(to.getType()));
        vo.setPciDeviceAddress(to.getPciDeviceAddress());
        vo.setIommuGroup(vo.getHostUuid() + '_' + to.getIommuGroup());

        // If the pci device is SRIOV_VIRTUALIZABLE or VFIO_MDEV_VIRTUALIZABLE, then stick with it, do not change!
        PciDeviceVirtStatus oldStatus = vo.getVirtStatus();
        PciDeviceVirtStatus newStatus = PciDeviceVirtStatus.valueOf(to.virtStatus);
        List<PciDeviceVirtStatus> virtualizableStatus = Arrays.asList(PciDeviceVirtStatus.SRIOV_VIRTUALIZABLE, PciDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE);
        if (!virtualizableStatus.contains(oldStatus)) {
            vo.setVirtStatus(newStatus);
        }

        // if pci type is Ethernet, will sync virt status from kvmagent
        if (vo.getType() == PciDeviceType.Ethernet_Controller ) {
            vo.setVirtStatus(newStatus);
        }

        // reduce the influence of pci device address format changing from 00:00.0 to 0000:00:00.0
        if (StringUtils.isEmpty(vo.getParentUuid()) && StringUtils.isNotBlank(to.getParentAddress())) {
            vo.setParentUuid(Q.New(PciDeviceVO.class).eq(PciDeviceVO_.pciDeviceAddress, to.getParentAddress()).select(PciDeviceVO_.uuid).findValue());
        }*/
    }

    public static boolean pciDeviceAddressEquals(String addr1, String addr2) {
        if (addr1.split(":").length < 3) {
            addr1 = "0000:" + addr1;
        }
        if (addr2.split(":").length < 3) {
            addr2 = "0000:" + addr2;
        }
        return addr1.equals(addr2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostVirtualNetworkInterfaceTO)) return false;

        HostVirtualNetworkInterfaceTO that = (HostVirtualNetworkInterfaceTO) o;
        if (!hostUuid.equals(that.hostUuid)) return false;
        if (!vendorId.equals(that.vendorId)) return false;
        if (!deviceId.equals(that.deviceId)) return false;
        if (StringUtils.isNotBlank(subvendorId) ? !subvendorId.equals(that.getSubvendorId()) : StringUtils.isNotBlank(that.getSubvendorId())) return false;
        if (StringUtils.isNotBlank(subdeviceId) ? !subdeviceId.equals(that.getSubdeviceId()) : StringUtils.isNotBlank(that.getSubdeviceId())) return false;
        // before 3.5.2, pciDeviceAddress is like 01:00.0, it changed into 0000:01:00.0 since 3.5.2
        return pciDeviceAddressEquals(pciDeviceAddress, that.pciDeviceAddress);
    }

    @Override
    public int hashCode() {
        int result = hostUuid.hashCode();
        result = 31 * result + vendorId.hashCode();
        result = 31 * result + deviceId.hashCode();
        result = 31 * result + (subvendorId != null ? subvendorId.hashCode() : 0);
        result = 31 * result + (subdeviceId != null ? subdeviceId.hashCode() : 0);
        result = 31 * result + ((pciDeviceAddress.split(":").length < 3) ? ("0000:" + pciDeviceAddress).hashCode() : pciDeviceAddress.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "PciDeviceTO{" +
                "uuid='" + uuid + '\'' +
                ", description='" + description + '\'' +
                ", hostUuid='" + hostUuid + '\'' +
                ", vendorId='" + vendorId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", subvendorId='" + subvendorId + '\'' +
                ", subdeviceId='" + subdeviceId + '\'' +
                ", pciDeviceAddress='" + pciDeviceAddress + '\'' +
                ", parentAddress='" + parentAddress + '\'' +
                '}';
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

    public String getParentAddress() {
        return parentAddress;
    }

    public void setParentAddress(String parentAddress) {
        this.parentAddress = parentAddress;
    }
}
