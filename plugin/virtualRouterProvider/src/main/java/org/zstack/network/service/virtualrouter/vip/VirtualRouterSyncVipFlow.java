package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        SimpleQuery<VipVO> q = dbf.createQuery(VipVO.class);
        q.add(VipVO_.peerL3NetworkUuid, Op.IN, vr.getGuestL3Networks());
        List<VipVO> vips = q.list();
        if (vips.isEmpty()) {
            chain.next();
            return;
        }

        List<VipInventory> invs = VipInventory.valueOf(vips);
        vipExt.createVipOnVirtualRouterVm(vr, invs, new Completion(chain) {
            @Override
            public void success() {
                //TODO: remove this, we need to remove VirtualRouterVipVO table
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
