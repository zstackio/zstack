package org.zstack.network.service.virtualrouter;

import org.zstack.network.service.vip.VipBaseBackend;
import org.zstack.network.service.vip.VipVO;

public interface VirtualRouterHaGetVipBaseBackendExtensionPoint {
    VipBaseBackend getVipBaseBackend(VipVO vip);
    VipBaseBackend getVipBaseBackend(String vrUuid, VipVO vip);
}
