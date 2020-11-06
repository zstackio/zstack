package org.zstack.network.service.lb;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerServerGroupVmNicRefVO.class)
public class LoadBalancerServerGroupVmNicRefVO_ {
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, String> loadBalancerServerGroupUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, String> vmNicUuid;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, LoadBalancerVmNicStatus> status;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerServerGroupVmNicRefVO, Timestamp> lastOpDate;
}
