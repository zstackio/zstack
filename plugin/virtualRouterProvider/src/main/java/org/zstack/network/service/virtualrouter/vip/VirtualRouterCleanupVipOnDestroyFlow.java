package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipDeletionMsg;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant.Param;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VirtualRouterCleanupVipOnDestroyFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    private static CLogger logger = Utils.getLogger(VirtualRouterCleanupVipOnDestroyFlow.class);

    private void deleteVips(List<VipVO> vos, Completion completion){
        for (VipVO vo: vos) {
            VipDeletionMsg msg = new VipDeletionMsg();
            msg.setVipUuid(vo.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, msg.getVipUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if(!reply.isSuccess()){
                        logger.debug(String.format("VirtualRouter remove the vip[uuid %s] on the public interface failed.", vo.getUuid()));
                    }
                    completion.success();
                }
            });
        }
    }

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final String vrUuid = (String) data.get(Param.VR_UUID.toString());
        SimpleQuery<VirtualRouterVipVO> q = dbf.createQuery(VirtualRouterVipVO.class);
        q.add(VirtualRouterVipVO_.virtualRouterVmUuid, Op.EQ, vrUuid);
        List<VirtualRouterVipVO> refs = q.list();
        List<VipVO>  vips = new ArrayList<VipVO>();

        if (refs.isEmpty()) {
            trigger.next();
            return;
        }

        Iterator<VirtualRouterVipVO> it = refs.iterator();
        while (it.hasNext()){
            VirtualRouterVipVO vvipVO = it.next();
            VipVO vip = dbf.findByUuid(vvipVO.getUuid(), VipVO.class);
            if (vip != null){
                Set<String> useFor = vip.getServicesTypes();
                if(useFor != null && useFor.contains(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE)) {
                    vips.add(vip);
                }
            }
        }
        dbf.removeCollection(refs, VirtualRouterVipVO.class);
        if (!vips.isEmpty()) {
            deleteVips(vips, new Completion(trigger) {
                @Override
                public void success() {
                    trigger.next();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    trigger.next();
                }
            });
        } else {
            trigger.next();
        }
    }
}
