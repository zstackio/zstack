package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.DestroyVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDestroyOnHypervisorFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmDestroyOnHypervisorFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    
    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        String hostUuid = spec.getVmInventory().getHostUuid() == null ? spec.getVmInventory().getLastHostUuid() : spec.getVmInventory().getHostUuid();
        if (spec.getVmInventory().getClusterUuid() == null || hostUuid == null) {
            // the vm failed to start because no host available at that time
            // no need to send DestroyVmOnHypervisorMsg
            chain.next();
            return;
        }

        DestroyVmOnHypervisorMsg msg = new DestroyVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(chain) {

            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to destroy vm[uuid:%s, name:%s] on host, because %s", spec.getVmInventory().getUuid(), spec.getVmInventory().getName(), reply.getError()));
                    chain.fail(reply.getError());
                } else {
                    chain.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowTrigger chain, Map data) {
        chain.rollback();
    }
}
