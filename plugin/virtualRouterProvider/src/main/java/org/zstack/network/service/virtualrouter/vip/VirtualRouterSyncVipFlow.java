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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        if (vr.getGuestL3Networks() == null || vr.getGuestL3Networks().isEmpty()) {
            chain.next();
            return;
        }
        List<VipPeerL3NetworkRefVO> refs = Q.New(VipPeerL3NetworkRefVO.class)
                .in(VipPeerL3NetworkRefVO_.l3NetworkUuid, vr.getGuestL3Networks())
                .list();
        if (refs == null || refs.isEmpty()) {
            chain.next();
            return;
        }

        List<String> vipUuids = new ArrayList<>(refs.stream()
                .map(ref -> ref.getVipUuid())
                .collect(Collectors.toSet()));
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
