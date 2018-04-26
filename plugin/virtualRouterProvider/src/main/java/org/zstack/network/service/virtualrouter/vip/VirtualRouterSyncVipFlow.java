package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncVipFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    @Qualifier("VirtualRouterVipBackend")
    protected VirtualRouterVipBackend vipExt;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());

        List<String> vrVips = Q.New(VirtualRouterVipVO.class).eq(VirtualRouterVipVO_.virtualRouterVmUuid, vr.getUuid())
                .select(VirtualRouterVipVO_.uuid).listValues();
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
                List<VirtualRouterVipVO> vrvips = new ArrayList<>();
                for (VipVO vip : vips) {
                    VirtualRouterVipVO vo = dbf.findByUuid(vip.getUuid(), VirtualRouterVipVO.class);
                    if (vo == null) {
                        vo = new VirtualRouterVipVO();
                        vo.setUuid(vip.getUuid());
                        vo.setVirtualRouterVmUuid(vr.getUuid());
                        vrvips.add(vo);
                    }
                }

                if (!vrvips.isEmpty()) {
                    dbf.persistCollection(vrvips);
                }

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
