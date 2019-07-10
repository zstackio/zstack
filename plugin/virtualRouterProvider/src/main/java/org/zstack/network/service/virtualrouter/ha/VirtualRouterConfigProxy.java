package org.zstack.network.service.virtualrouter.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import java.util.ArrayList;
import java.util.List;

public abstract class VirtualRouterConfigProxy {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry  pluginRgty;

    protected abstract void attachNetworkSericeToVirtualRouter(String vrUuid, String type, List<String> serviceUuids);
    protected abstract void detachNetworkSericeFromVirtualRouter(String vrUuid, String type, List<String> serviceUuids);
    protected abstract List<String> getVrUuidsFromByNetworkService(String serviceUuid);
    protected abstract List<String> getServiceUuidsFromByVRouter(String vrUuid);

    final public void attachNetworkSerice(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            attachNetworkSericeToVirtualRouter(vrUuid, type, serviceUuids);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.attachNetworkServiceToHaRouter(type, serviceUuids, vrUuid);
            }
        }
    }

    final public void DetachNetworkSerice(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            detachNetworkSericeFromVirtualRouter(vrUuid, type, serviceUuids);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.detachNetworkServiceFromHaRouter(type, serviceUuids, vrUuid);
            }
        }
    }

    final public List<String> getVrUuidsFromByNetworkService(String type, String serviceUuid) {
        List<String> vrUuids = getVrUuidsFromByNetworkService(serviceUuid);
        if (vrUuids != null || vrUuids.isEmpty()) {
            return vrUuids;
        }

        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps != null && !exps.isEmpty()) {
            vrUuids = exps.get(0).getHaVrUuidsFromNetworkService(type, serviceUuid);
        }

        return vrUuids;
    }

    final public List<String> getServiceUuidsFromByRouterUuid(String vrUuid, String type) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            return getServiceUuidsFromByVRouter(vrUuid);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                return ext.getNetworkServicesFromHaVrUuid(type, vrUuid);
            }

            return new ArrayList<>();
        }
    }
}

