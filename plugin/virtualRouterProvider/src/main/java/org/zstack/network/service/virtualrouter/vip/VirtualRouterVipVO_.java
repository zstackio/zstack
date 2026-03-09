package org.zstack.network.service.virtualrouter.vip;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VirtualRouterVipVO.class)
public class VirtualRouterVipVO_ {
    public static volatile SingularAttribute<VirtualRouterVipVO, String> virtualRouterVmUuid;
    public static volatile SingularAttribute<VirtualRouterVipVO, String> uuid;
}
