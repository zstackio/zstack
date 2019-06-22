package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterManager;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.network.service.lb.LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncVipFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected VipManager vipManager;
    @Autowired
    protected VirtualRouterManager vrMgr;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        String serviceProvderType = vrMgr.getVirtualRouterServiceProviderType(vr.getUuid(), null);
        VirtualRouterVipBackend vipExt = (VirtualRouterVipBackend)vipManager.getVipBackend(serviceProvderType);

        List<String> vrVips = vipExt.getAllVipsOnThisRouter(vr.getUuid());

        List<String> peerL3Vips = null;
        if (vr.getGuestL3Networks() != null && !vr.getGuestL3Networks().isEmpty()) {
            peerL3Vips = Q.New(VipPeerL3NetworkRefVO.class).select(VipPeerL3NetworkRefVO_.vipUuid)
                    .in(VipPeerL3NetworkRefVO_.l3NetworkUuid, vr.getGuestL3Networks())
                    .listValues();
        }

        Set<String> vipUuids = null;
        if (vrVips != null && !vrVips.isEmpty()) {
            vipUuids = new HashSet<>(vrVips);
        }

        if (peerL3Vips != null && !peerL3Vips.isEmpty()) {
            if (vipUuids == null) {
                vipUuids = new HashSet<>(peerL3Vips);
            } else {
                vipUuids.addAll(new HashSet<String>(peerL3Vips));
            }
        }

        if (vipUuids == null) {
            chain.next();
            return;
        }

        List<VipVO> vips = vipUuids.stream()
                .map(uuid -> (VipVO)Q.New(VipVO.class).eq(VipVO_.uuid, uuid).find())
                .collect(Collectors.toList());
        List<VipInventory> invs = VipInventory.valueOf(vips);
        vipExt.createVipOnVirtualRouterVm(vr, invs, new Completion(chain) {
            @Override
            public void success() {
                vipExt.attachVipToVirtualRouter(vr.getUuid(),
                        vips.stream().map(VipVO::getUuid).collect(Collectors.toList()));

                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        chain.rollback();
    }
}
