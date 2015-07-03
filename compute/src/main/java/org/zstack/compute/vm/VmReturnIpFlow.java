package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReturnIpFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (!spec.getVmInventory().getVmNics().isEmpty()) {
            List<ReturnIpMsg> msgs = new ArrayList<ReturnIpMsg>(spec.getVmInventory().getVmNics().size());
            List<String> nicUuids = new ArrayList<String>();
            for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
                nicUuids.add(nic.getUuid());
                if (nic.getUsedIpUuid() != null) {
                    ReturnIpMsg msg = new ReturnIpMsg();
                    msg.setL3NetworkUuid(nic.getL3NetworkUuid());
                    msg.setUsedIpUuid(nic.getUsedIpUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                    msgs.add(msg);
                }
            }

            dbf.removeByPrimaryKeys(nicUuids, VmNicVO.class);
            bus.send(msgs);
        }
        chain.next();
    }
}
