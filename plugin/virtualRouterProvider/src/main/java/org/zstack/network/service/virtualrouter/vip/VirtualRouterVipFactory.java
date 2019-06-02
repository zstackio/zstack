package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterHaGetVipBaseBackendExtensionPoint;

/**
 * Created by xing5 on 2016/12/2.
 */
public class VirtualRouterVipFactory implements VipFactory{
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public String getNetworkServiceProviderType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_PROVIDER_TYPE;
    }

    @Override
    public VipBaseBackend getVip(VipVO self) {
        /* to be simple, use extension to get ha backend */
        VipBaseBackend backend = null;
        for (VirtualRouterHaGetVipBaseBackendExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGetVipBaseBackendExtensionPoint.class)) {
            backend = ext.getVipBaseBackend(self);
        }

        if (backend != null) {
            return backend;
        }

        return new VirtualRouterVipBaseBackend(self);
    }

    @Override
    public VipBaseBackend getVip(String vrUuid, VipVO self) {
        VipBaseBackend backend = null;
        for (VirtualRouterHaGetVipBaseBackendExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGetVipBaseBackendExtensionPoint.class)) {
            backend = ext.getVipBaseBackend(vrUuid, self);
        }

        if (backend != null) {
            return backend;
        }

        return new VirtualRouterVipBaseBackend(self);
    }
}
