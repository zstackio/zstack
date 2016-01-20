package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterSyncVipFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected VirtualRouterVipBackend vipExt;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VirtualRouterVmInventory vr = (VirtualRouterVmInventory) data.get(VirtualRouterConstant.Param.VR.toString());
        SimpleQuery<VirtualRouterVipVO> q = dbf.createQuery(VirtualRouterVipVO.class);
        q.select(VirtualRouterVipVO_.uuid);
        q.add(VirtualRouterVipVO_.virtualRouterVmUuid, Op.EQ, vr.getUuid());
        List<String> vipUuids = q.listValue();
        if (vipUuids.isEmpty()) {
            chain.next();
            return;
        }

        List<VipVO> vips = dbf.listByPrimaryKeys(vipUuids, VipVO.class);
        List<VipInventory> invs = VipInventory.valueOf(vips);
        vipExt.createVipOnVirtualRouterVm(vr, invs, new Completion(chain) {
            @Override
            public void success() {
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
