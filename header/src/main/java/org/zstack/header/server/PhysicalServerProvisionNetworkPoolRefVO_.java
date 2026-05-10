package org.zstack.header.server;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerProvisionNetworkPoolRefVO.class)
public class PhysicalServerProvisionNetworkPoolRefVO_ {
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkPoolRefVO, Long> id;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkPoolRefVO, String> networkUuid;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkPoolRefVO, String> poolUuid;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkPoolRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkPoolRefVO, Timestamp> lastOpDate;
}
