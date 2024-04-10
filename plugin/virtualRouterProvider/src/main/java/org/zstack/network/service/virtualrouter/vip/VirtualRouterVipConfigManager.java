package org.zstack.network.service.virtualrouter.vip;

public interface VirtualRouterVipConfigManager {
    VirtualRouterVipConfigFactory getVirtualRouterVipConfigFactory(String type);
}
