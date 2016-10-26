package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.SetRootPasswordMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by mingjian.deng on 16/10/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSetRootPasswordFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmSetRootPasswordFlow.class);
    @Autowired
    private CloudBus bus;
    @Override
    public void run(final FlowTrigger trigger, Map data) {
        logger.debug("check if need reset rootpassword before start a new created vm.");
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if(spec.getAccountPerference() == null)
            trigger.next();

        if(spec.getDestHost() == null) {
            ErrorCode err = new ErrorCode(
                    "NO_DEST_HOST_FOUND", "not dest host found",
                    "not dest host found, can't send change password cmd to the host!"
            );
            trigger.fail(err);
        }
        String qcowFilePath = VmQcowFileFind.generateQcowFilePath(trigger, spec);
        if(qcowFilePath == null) {
            logger.warn("qcowFilePath is null, but it should not block the create vm.");
            trigger.next();
        }
        SetRootPasswordMsg msg = new SetRootPasswordMsg();
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setVmUuid(spec.getAccountPerference().getVmUuid());
        msg.setRootPassword(spec.getAccountPerference().getAccountPassword());
        msg.setQcowFile(qcowFilePath);
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
