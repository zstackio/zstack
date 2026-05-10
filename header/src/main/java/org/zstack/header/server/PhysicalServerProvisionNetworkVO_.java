package org.zstack.header.server;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerProvisionNetworkVO.class)
public class PhysicalServerProvisionNetworkVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> zoneUuid;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> name;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> description;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, ProvisionNetworkType> type;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> dhcpInterface;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> dhcpRangeStartIp;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> dhcpRangeEndIp;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> dhcpRangeNetmask;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, String> dhcpRangeGateway;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, ProvisionNetworkState> state;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkVO, Timestamp> lastOpDate;
}
