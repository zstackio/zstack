package org.zstack.header.server;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerProvisionNetworkClusterRefVO.class)
public class PhysicalServerProvisionNetworkClusterRefVO_ {
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkClusterRefVO, Long> id;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkClusterRefVO, String> networkUuid;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkClusterRefVO, String> clusterUuid;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkClusterRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerProvisionNetworkClusterRefVO, Timestamp> lastOpDate;
}
