package org.zstack.network.service.virtualrouter.ha;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.IpRangeAO;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.IpRangeVO_;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import java.util.ArrayList;
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
                VmNicInventory nic = (VmNicInventory) data.get(VirtualRouterConstant.Param.VR_NIC);
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

    public List<String> getSecondaryIpsOfVirtualRouterHaGroup(String vrUuid, String l3NetworkUuid) {
        VirtualRouterVmVO vrVO = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVO.isHaEnabled()) {
            return new ArrayList<>();
        }

        return Q.New(IpRangeVO.class).select(IpRangeVO_.gateway).eq(IpRangeVO_.l3NetworkUuid, l3NetworkUuid)
                .limit(1).listValues();
    }

    public void prepareVirtualRouterHaConfig(String vrUuid, Completion completion) {
        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps.isEmpty()) {
            completion.success();
            return;
        }

        exps.get(0).prepareVirtualRouterHaConfig(vrUuid, completion);
    }
}
