package org.zstack.network.hostNetwork.lldp.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostNetworkInterfaceLldpRefVO.class)
public class HostNetworkInterfaceLldpRefVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostNetworkInterfaceLldpVO, Long> id;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> interfaceUuid;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> chassisId;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Integer> timeToLive;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> managementAddress;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> systemName;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> systemDescription;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> systemCapabilities;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, String> portDescription;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Integer> vlanId;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Long> aggregationPortId;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Integer> mtu;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkInterfaceLldpRefVO, Timestamp> lastOpDate;
}
