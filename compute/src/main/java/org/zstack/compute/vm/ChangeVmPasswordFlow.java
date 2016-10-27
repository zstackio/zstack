package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StringUtils;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.ChangeVmPasswordMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by mingjian.deng on 16/10/18.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ChangeVmPasswordFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(ChangeVmPasswordFlow.class);
    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        logger.debug("change password while vm is running.");
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        ChangeVmPasswordMsg msg = new ChangeVmPasswordMsg();
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setAccountPerference(spec.getAccountPerference());
        if(spec.getDestHost() == null) {
            ErrorCode err = new ErrorCode(
                    "NO_DEST_HOST_FOUND", "not dest host found",
                    "not dest host found, can't send change password cmd to the host!"
            );
            trigger.fail(err);
        }
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }
                trigger.next();
            }
        });
    }
}
