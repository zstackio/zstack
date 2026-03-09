package org.zstack.network.service.virtualrouter.portforwarding;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VirtualRouterPortForwardingRuleRefVO.class)
public class VirtualRouterPortForwardingRuleRefVO_ {
    public static volatile SingularAttribute<VirtualRouterPortForwardingRuleRefVO, String> uuid;
    public static volatile SingularAttribute<VirtualRouterPortForwardingRuleRefVO, String> vipUuid;
    public static volatile SingularAttribute<VirtualRouterPortForwardingRuleRefVO, String> virtualRouterVmUuid;
}
