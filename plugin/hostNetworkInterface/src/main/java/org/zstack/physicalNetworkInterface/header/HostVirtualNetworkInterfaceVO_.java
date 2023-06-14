package org.zstack.physicalNetworkInterface.header;

import org.zstack.header.host.NicVirtStatus;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.pciDevice.*;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostVirtualNetworkInterfaceVO.class)
public class HostVirtualNetworkInterfaceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> description;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> hostUuid;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> hostNetworkInterfaceUuid;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> vmNicUuid;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, HostVirtualNetworkInterfaceStatus> status;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> vendorId;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> deviceId;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> subvendorId;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> subdeviceId;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> pciDeviceAddress;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, String> metaData;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostVirtualNetworkInterfaceVO, Timestamp> lastOpDate;
}
