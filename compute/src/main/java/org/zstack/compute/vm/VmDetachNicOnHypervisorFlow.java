package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.DetachNicFromVmOnHypervisorMsg;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by frank on 7/18/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDetachNicOnHypervisorFlow extends NoRollbackFlow {
    private final static CLogger logger = Utils.getLogger(VmDetachNicOnHypervisorFlow.class);
    @Autowired
    private CloudBus bus;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        DetachNicFromVmOnHypervisorMsg msg = new DetachNicFromVmOnHypervisorMsg();
        msg.setHostUuid(spec.getVmInventory().getHostUuid());
        msg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msg.setNic(spec.getDestNics().get(0));
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    trigger.next();
                } else {
                    logger.warn(String.format("delete vmnic[uuid:%s] of vm [uuid:%s] from host failed because %s",
                            msg.getNic().getUuid(), msg.getVmInstanceUuid(), reply.getError().getDetails()));
                    trigger.next();
                }
            }
        });
    }
}
