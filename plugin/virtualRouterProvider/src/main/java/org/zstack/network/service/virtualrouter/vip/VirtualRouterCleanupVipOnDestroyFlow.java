package org.zstack.network.service.virtualrouter.vip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.network.service.vip.VipConstant;
import org.zstack.network.service.vip.VipDeletionMsg;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
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
    @Autowired
    protected VipConfigProxy vipConfigProxy;

    private static CLogger logger = Utils.getLogger(VirtualRouterCleanupVipOnDestroyFlow.class);

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final String vrUuid = (String) data.get(Param.VR_UUID.toString());
        boolean isHaRouter = (boolean) data.get(VirtualRouterConstant.Param.IS_HA_ROUTER.toString());

        if (isHaRouter) {
            logger.debug("skip ha virtual router");
            trigger.next();
            return;
        }

        List<String> vipUuids = vipConfigProxy.getServiceUuidsByRouterUuid(vrUuid, VipVO.class.getSimpleName());
        if (vipUuids.isEmpty()) {
            logger.debug(String.format("there is no vip attached to virtual router[uuid:%s]", vrUuid));
            trigger.next();
            return;
        }

        List<VipVO> vips = Q.New(VipVO.class).in(VipVO_.uuid, vipUuids).eq(VipVO_.system, true).list();
        if (!vips.isEmpty()) {
            new While<>(vips).each((vip, compl) -> {
                VipDeletionMsg msg = new VipDeletionMsg();
                msg.setVipUuid(vip.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, VipConstant.SERVICE_ID, msg.getVipUuid());
                if (vip.getServicesTypes() == null || vip.getServicesTypes().isEmpty()) {
                    /* TODO: this ugly code because when delete vip, VirtualRouterVip must be delete first */
                    vipConfigProxy.detachNetworkService(vrUuid, VipVO.class.getSimpleName(), Arrays.asList(vip.getUuid()));
                }

                bus.send(msg, new CloudBusCallBack(compl) {
                    @Override
                    public void run(MessageReply reply) {
                        if(!reply.isSuccess()){
                            logger.debug(String.format("VirtualRouter remove the vip[uuid %s] on the public interface failed.", vip.getUuid()));
                        }
                        compl.done();
                    }
                });
            }).run(new WhileDoneCompletion(trigger) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    vipConfigProxy.detachNetworkService(vrUuid, VipVO.class.getSimpleName(), vipUuids);
                    trigger.next();
                }
            });
        } else {
            vipConfigProxy.detachNetworkService(vrUuid, VipVO.class.getSimpleName(), vipUuids);
            trigger.next();
        }
    }
}
