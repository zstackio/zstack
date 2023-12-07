package org.zstack.network.hostNetwork;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 4/24/20.
 */
@StaticMetamodel(HostNetworkInterfaceVO.class)
public class HostNetworkInterfaceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> hostUuid;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> bondingUuid;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> interfaceModel;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> vendorId;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> deviceId;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> subvendorId;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> subdeviceId;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> interfaceName;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> interfaceType;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> mac;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> ipAddresses;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> gateway;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> callBackIp;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> pciDeviceAddress;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> offloadStatus;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, Long> speed;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, Boolean> slaveActive;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, Boolean> carrierActive;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, String> description;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkInterfaceVO, Timestamp> lastOpDate;
}
