package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.VipUseForList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCleanupVipOnDestroyFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    private void deleteVips(List<VipVO> vos){
        for (VipVO vo: vos) {
            VipDeletionMsg msg = new VipDeletionMsg();
            msg.setVipUuid(vo.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, msg.getVipUuid());
            bus.send(msg);
        }
    }

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final String vrUuid = (String) data.get(Param.VR_UUID.toString());
        SimpleQuery<VirtualRouterVipVO> q = dbf.createQuery(VirtualRouterVipVO.class);
        q.add(VirtualRouterVipVO_.virtualRouterVmUuid, Op.EQ, vrUuid);
        List<VirtualRouterVipVO> refs = q.list();
        List<VipVO>  vips = new ArrayList<VipVO>();
        if (!refs.isEmpty()) {
            Iterator<VirtualRouterVipVO> it = refs.iterator();
            while (it.hasNext()){
                VirtualRouterVipVO vvipVO = it.next();
                VipVO vip = dbf.findByUuid(vvipVO.getUuid(), VipVO.class);
                if (vip != null && vip.getUseFor() != null){
                    VipUseForList useForList = new VipUseForList(vip.getUseFor());
                    if(useForList.isIncluded(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE)) {
                        vips.add(vip);
                    }
                }
            }
            dbf.removeCollection(refs, VirtualRouterVipVO.class);
            if (!vips.isEmpty()) {
                deleteVips(vips);
            }
        }
        trigger.next();
    }
}
