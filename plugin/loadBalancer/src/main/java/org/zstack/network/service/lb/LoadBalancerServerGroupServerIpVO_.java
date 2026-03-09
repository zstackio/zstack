package org.zstack.network.service.lb;

import org.zstack.header.vo.ResourceVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerServerGroupServerIpVO.class)
public class LoadBalancerServerGroupServerIpVO_ {
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, String> serverGroupUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, String> ipAddress;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, Long> weight;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, LoadBalancerBackendServerStatus> status;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, Timestamp> lastOpDate;
}
