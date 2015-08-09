package org.zstack.network.service.lb;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/9/2015.
 */
@StaticMetamodel(LoadBalancerListenerVO.class)
public class LoadBalancerListenerVO_ {
    public static volatile SingularAttribute<LoadBalancerListenerVO, String> uuid;
    public static volatile SingularAttribute<LoadBalancerListenerVO, String> loadBalancerUuid;
    public static volatile SingularAttribute<LoadBalancerListenerVO, String> name;
    public static volatile SingularAttribute<LoadBalancerListenerVO, String> description;
    public static volatile SingularAttribute<LoadBalancerListenerVO, Integer> instancePort;
    public static volatile SingularAttribute<LoadBalancerListenerVO, Integer> loadBalancerPort;
    public static volatile SingularAttribute<LoadBalancerListenerVO, String> protocol;
    public static volatile SingularAttribute<LoadBalancerListenerVO, Timestamp> createDate;
    public static volatile SingularAttribute<LoadBalancerListenerVO, Timestamp> lastOpDate;
}
