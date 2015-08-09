package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/8/2015.
 */
@StaticMetamodel(LoadBalancerVipRefVO.class)
public class LoadBalancerVipRefVO_ {
    public static volatile SingularAttribute<LoadBalancerVipRefVO, Long> id;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, String> vipUuid;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, String> loadBalancerUuid;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerVipRefVO, Timestamp> lastOpDate;
}

