package org.zstack.network.service.virtualrouter.eip;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VirtualRouterEipRefVO.class)
public class VirtualRouterEipRefVO_ {
    public static volatile SingularAttribute<VirtualRouterEipRefVO, String> eipUuid;
    public static volatile SingularAttribute<VirtualRouterEipRefVO, String> virtualRouterVmUuid;
}
