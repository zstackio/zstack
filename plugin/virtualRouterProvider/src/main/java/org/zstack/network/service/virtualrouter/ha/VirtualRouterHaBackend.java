package org.zstack.network.service.virtualrouter.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.network.service.VirtualRouterHaCallbackInterface;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterSystemTags;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import java.util.List;
import java.util.Map;

public class VirtualRouterHaBackend {
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    protected DatabaseFacade dbf;

    /* VirtualRouterHaGroupExtensionPoint should */
    public NoRollbackFlow getAttachL3NetworkFlow() {
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmNicInventory nic = (VmNicInventory) data.get(VirtualRouterConstant.Param.VR_NIC.toString());
                List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
                if (exps.isEmpty()) {
                    trigger.next();
                    return;
                }

                exps.get(0).VirtualRouterVmHaAttachL3Network(nic.getVmInstanceUuid(), nic.getL3NetworkUuid(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        };
    }

    public void detachL3NetworkFromVirtualRouterHaGroup(String vrUuid, String l3NetworkUuid, boolean isRollback, Completion completion) {
        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps.isEmpty()) {
            completion.success();
            return;
        }

        exps.get(0).VirtualRouterVmHaDetachL3Network(vrUuid, l3NetworkUuid, isRollback, completion);
    }

    public void submitVirutalRouterHaTask(VirtualRouterHaCallbackInterface callback, Map<String, Object> data, Completion completion) {
        String vrUuid = (String)data.get(VirtualRouterHaCallbackInterface.Params.OriginRouterUuid.toString());
        VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(dbf.findByUuid(vrUuid, VirtualRouterVmVO.class));
        if (!vrInv.isHaEnabled()) {
            completion.success();
            return;
        }

        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps.isEmpty()) {
            completion.success();
            return;
        }

        String peerUuid = exps.get(0).getPeerUuid(vrUuid);
        if (peerUuid == null) {
            completion.success();
            return;
        }

        data.put(VirtualRouterHaCallbackInterface.Params.PeerRouterUuid.toString(), peerUuid);
        exps.get(0).submitTaskToHaRouter(callback, data, completion);
    }

    public boolean isSnatDisabledOnRouter(String vrUuid) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVO.isHaEnabled()) {
            return VirtualRouterSystemTags.VR_DISABLE_NETWORK_SERVICE_SNAT.hasTag(vrUuid);
        } else {
            List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
            if (exps.isEmpty()) {
                return false;
            }

            List<String> state = exps.get(0).getNetworkServicesFromHaVrUuid(NetworkServiceType.SNAT.toString(),  vrUuid);
            if (state == null || state.isEmpty()) {
                return false;
            } else {
                return Boolean.parseBoolean(state.get(0));
            }
        }
    }

    public void cleanupHaNetworkService(String vrUuid, Completion completion) {
        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps.isEmpty()) {
            completion.success();
            return ;
        }

        exps.get(0).cleanupHaNetworkService(vrUuid, completion);
    }
}
