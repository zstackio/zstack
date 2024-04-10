package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.appliancevm.ApplianceVmType;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import java.util.ArrayList;
import java.util.List;

public class VipConfigProxy implements ApplianceVmSyncConfigToHaGroupExtensionPoint {
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private VirtualRouterVipConfigManager vrVipConfigMgr;

    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {

    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {

    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        completion.done();
    }

    public void attachNetworkService(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        VirtualRouterVipConfigFactory factory = vrVipConfigMgr.getVirtualRouterVipConfigFactory(vrVo.getApplianceVmType());
        factory.attachNetworkService(vrUuid, serviceUuids);
    }

    final public void detachNetworkService(String vrUuid, String type, List<String> serviceUuids) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        VirtualRouterVipConfigFactory factory = vrVipConfigMgr.getVirtualRouterVipConfigFactory(vrVo.getApplianceVmType());
        factory.detachNetworkService(vrUuid, serviceUuids);
    }

    final public List<String> getVrUuidsByNetworkService(String type, String serviceUuid) {
        for (ApplianceVmType atype : ApplianceVmType.values()) {
            VirtualRouterVipConfigFactory factory = vrVipConfigMgr.getVirtualRouterVipConfigFactory(atype.toString());
            if (factory != null) {
                List<String> vrUuids = factory.getVrUuidsByNetworkService(serviceUuid);
                if (!vrUuids.isEmpty()) {
                    return vrUuids;
                }
            }
        }

        return new ArrayList<>();
    }


    final public List<String> getServiceUuidsByRouterUuid(String vrUuid, String type) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        VirtualRouterVipConfigFactory factory = vrVipConfigMgr.getVirtualRouterVipConfigFactory(vrVo.getApplianceVmType());
        return factory.getVipUuidsByRouterUuid(vrUuid);
    }
}
