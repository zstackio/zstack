package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerListenerServerGroupVmNicRefVO.class)
public class LoadBalancerListenerServerGroupVmNicRefVO_ {
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupVmNicRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupVmNicRefVO, String> listenerUuid;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupVmNicRefVO, String> serverGroupUuid;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupVmNicRefVO, String> vmNicUuid;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupVmNicRefVO, LoadBalancerBackendServerState> state;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupVmNicRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupVmNicRefVO, Timestamp> lastOpDate;
}
