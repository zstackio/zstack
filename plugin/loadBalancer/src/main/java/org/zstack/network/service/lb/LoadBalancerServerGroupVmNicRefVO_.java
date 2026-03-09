package org.zstack.network.service.lb;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerServerGroupVmNicRefVO.class)
public class LoadBalancerServerGroupVmNicRefVO_ {
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, String> serverGroupUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, String> vmNicUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Long> weight;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Integer> ipVersion;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, LoadBalancerVmNicStatus> status;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Timestamp> lastOpDate;
}
