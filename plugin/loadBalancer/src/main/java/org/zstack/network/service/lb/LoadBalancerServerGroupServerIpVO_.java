package org.zstack.network.service.lb;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerServerGroupServerIpVO.class)
public class LoadBalancerServerGroupServerIpVO_ {
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, String> loadBalancerServerGroupUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, String> vmNicUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, String> ipAddress;;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, LoadBalancerBackendServerStatus> status;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerServerGroupServerIpVO, Timestamp> lastOpDate;
}
