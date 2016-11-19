package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.eip.EipConstant.Params;
import org.zstack.network.service.vip.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class EipPrepareVipFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(EipPrepareVipFlow.class);

    @Autowired
    private CloudBus bus;

    private static final String SUCCESS = EipPrepareVipFlow.class.getName();

    public void run(final FlowTrigger trigger, final Map data) {
        final VipInventory v = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        final String serviceProviderType = (String) data.get(VipConstant.Params.VIP_SERVICE_PROVIDER_TYPE.toString());
        final L3NetworkInventory peerL3 = (L3NetworkInventory) data.get(VipConstant.Params.GUEST_L3NETWORK_VIP_FOR.toString());
        boolean needLockVip = data.containsKey(Params.NEED_LOCK_VIP.toString());

        Vip vip = new Vip(v.getUuid());
        vip.setServiceProvider(serviceProviderType);
        vip.setUseFor(needLockVip ? EipConstant.EIP_NETWORK_SERVICE_TYPE : null);
        vip.setPeerL3NetworkUuid(peerL3.getUuid());
        vip.acquire(new Completion(trigger) {
            @Override
            public void success() {
                data.put(SUCCESS, true);
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void rollback(final FlowRollback trigger, Map data) {
        final VipInventory v = (VipInventory) data.get(VipConstant.Params.VIP.toString());
        if (!data.containsKey(SUCCESS)) {
            trigger.rollback();
            return;
        }

        Vip vip = new Vip(v.getUuid());
        vip.release(new Completion(trigger) {
            @Override
            public void success() {
                trigger.rollback();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(errorCode.toString());
                trigger.rollback();
            }
        });
    }
}
