package org.zstack.network.service.portforwarding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.network.service.portforwarding.PortForwardingConstant.Params;
import org.zstack.network.service.vip.*;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PortForwardingReturnVipFlow extends NoRollbackFlow {
    @Override
    public void run(final FlowTrigger trigger, Map data) {
        VipInventory v = (VipInventory) data.get(VipConstant.Params.VIP.toString());

        new Vip(v.getUuid()).release(new Completion(trigger) {
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
}
