package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.HashMap;

public class VirtualRouterVipConfigManagerImpl implements VirtualRouterVipConfigManager, Component {
    @Autowired
    protected PluginRegistry pluginRgty;

    private HashMap<String, VirtualRouterVipConfigFactory> vipConfigFactoryHashMap = new HashMap<>();

    @Override
    public VirtualRouterVipConfigFactory getVirtualRouterVipConfigFactory(String type) {
        return vipConfigFactoryHashMap.get(type);
    }

    @Override
    public boolean start() {
        for (VirtualRouterVipConfigFactory factory : pluginRgty.getExtensionList(VirtualRouterVipConfigFactory.class)) {
            VirtualRouterVipConfigFactory old = vipConfigFactoryHashMap.get(factory.getApplianceVmType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VirtualRouterVipConfigFactory[%s, %s] for applianceVm type[%s]",
                        old.getClass().getName(), factory.getClass().getName(), factory.getApplianceVmType().toString()));
            }
            vipConfigFactoryHashMap.put(factory.getApplianceVmType().toString(), factory);
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
