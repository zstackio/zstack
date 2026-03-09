package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 */
@StaticMetamodel(VirtualRouterVmVO.class)
public class VirtualRouterVmVO_ extends ApplianceVmVO_ {
    public static volatile SingularAttribute<VirtualRouterVmVO, String> publicNetworkUuid;
}
