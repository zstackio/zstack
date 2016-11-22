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
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReturnReleaseNicFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceDeletionPolicyManager deletionPolicyMgr;

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getVmNics().isEmpty()) {
            chain.next();
            return;
        }

            List<ReturnIpMsg> msgs = new ArrayList<ReturnIpMsg>(spec.getVmInventory().getVmNics().size());
            for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
                if (nic.getUsedIpUuid() != null) {
                    ReturnIpMsg msg = new ReturnIpMsg();
                    msg.setL3NetworkUuid(nic.getL3NetworkUuid());
                    msg.setUsedIpUuid(nic.getUsedIpUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
                    msgs.add(msg);
                }

                VmNicVO vo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
                if (VmInstanceConstant.USER_VM_TYPE.equals(spec.getVmInventory().getType())) {
                    VmInstanceDeletionPolicy deletionPolicy = deletionPolicyMgr.getDeletionPolicy(spec.getVmInventory().getUuid());
                    if (deletionPolicy == VmInstanceDeletionPolicy.Direct) {
                        dbf.remove(vo);
                    } else {
                        vo.setUsedIpUuid(null);
                        vo.setIp(null);
                        vo.setGateway(null);
                        vo.setNetmask(null);
                        dbf.update(vo);
                    }
                } else {
                    dbf.remove(vo);
                }
            }

            bus.send(msgs);

        chain.next();
    }
}
