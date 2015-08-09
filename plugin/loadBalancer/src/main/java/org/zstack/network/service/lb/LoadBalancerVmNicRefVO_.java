package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@StaticMetamodel(LoadBalancerVmNicRefVO.class)
public class LoadBalancerVmNicRefVO_ {
    public static volatile SingularAttribute<LoadBalancerVipRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, String> loadBalancerUuid;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, String> vmNicUuid;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, LoadBalancerVmNicStatus> status;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, Timestamp> lastOpDate;
}
