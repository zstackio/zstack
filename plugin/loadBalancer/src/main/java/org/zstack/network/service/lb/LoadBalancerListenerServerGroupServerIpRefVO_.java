package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LoadBalancerListenerServerGroupServerIpRefVO.class)
public class LoadBalancerListenerServerGroupServerIpRefVO_ {
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupServerIpRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupServerIpRefVO, String> listenerUuid;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupServerIpRefVO, String> serverGroupUuid;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupServerIpRefVO, Long> serverIpId;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupServerIpRefVO, LoadBalancerBackendServerState> state;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupServerIpRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerListenerServerGroupServerIpRefVO, Timestamp> lastOpDate;
}
