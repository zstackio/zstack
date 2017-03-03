package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostErrors;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.DestroyVmOnHypervisorMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDestroyOnHypervisorFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmDestroyOnHypervisorFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        final String hostUuid = spec.getVmInventory().getHostUuid() == null ? spec.getVmInventory().getLastHostUuid() : spec.getVmInventory().getHostUuid();
        if (spec.getVmInventory().getClusterUuid() == null || hostUuid == null) {
            // the vm failed to start because no host available at that time
            // no need to send DestroyVmOnHypervisorMsg
            chain.next();
            return;
        }

        if (VmInstanceState.Stopped.toString().equals(spec.getVmInventory().getState())) {
            chain.next();
            return;
        }

        DestroyVmOnHypervisorMsg msg = new DestroyVmOnHypervisorMsg();
        msg.setVmInventory(spec.getVmInventory());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(chain) {

            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                    return;
                }

                if (!reply.getError().isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                    chain.fail(reply.getError());
                    return;
                }

                DeleteVmGC gc = new DeleteVmGC();
                gc.NAME = String.format("gc-vm-%s-on-host-%s", spec.getVmInventory().getUuid(), hostUuid);
                gc.hostUuid = hostUuid;
                gc.inventory = spec.getVmInventory();
                gc.submit();

                chain.next();
            }
        });
    }
}
