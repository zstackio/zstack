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

    protected abstract void attachNetworkServiceToVirtualRouter(String vrUuid, String type, List<String> serviceUuids);
    protected abstract void detachNetworkServiceFromVirtualRouter(String vrUuid, String type, List<String> serviceUuids);
    protected abstract List<String> getVrUuidsByNetworkService(String serviceUuid);
    protected abstract List<String> getServiceUuidsByVRouter(String vrUuid);

    final public void attachNetworkService(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            attachNetworkServiceToVirtualRouter(vrUuid, type, serviceUuids);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.attachNetworkServiceToHaRouter(type, serviceUuids, vrUuid);
            }
        }
    }

    final public void detachNetworkService(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            detachNetworkServiceFromVirtualRouter(vrUuid, type, serviceUuids);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.detachNetworkServiceFromHaRouter(type, serviceUuids, vrUuid);
            }
        }
    }

    final public List<String> getVrUuidsByNetworkService(String type, String serviceUuid) {
        List<String> vrUuids = getVrUuidsByNetworkService(serviceUuid);
        if (vrUuids != null && !vrUuids.isEmpty() && vrUuids.size() <= 1) {
            return vrUuids;
        }

        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps != null && !exps.isEmpty()) {
            vrUuids = exps.get(0).getHaVrUuidsFromNetworkService(type, serviceUuid);
        }

        return vrUuids;
    }

    final public List<String> getServiceUuidsByRouterUuid(String vrUuid, String type) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            return getServiceUuidsByVRouter(vrUuid);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                return ext.getNetworkServicesFromHaVrUuid(type, vrUuid);
            }

            return new ArrayList<>();
        }
    }

    final public List<String> getServiceUuidsByHaGrupUuid(String haGroupUuid, String type) {
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            return ext.getNetworkServicesFromHaGroupUuid(type, haGroupUuid);
        }

        return new ArrayList<>();
    }
}

