package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;

import java.util.Map;

/**
 * Created by frank on 7/19/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSetDefaultL3NetworkOnAttachingFlow implements Flow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getDefaultL3NetworkUuid() != null) {
            trigger.next();
            return;
        }

        L3NetworkInventory l3 = spec.getL3Networks().get(0);
        VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
        vm.setDefaultL3NetworkUuid(l3.getUuid());
        dbf.update(vm);

        data.put(VmSetDefaultL3NetworkOnAttachingFlow.class, true);
        trigger.next();
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        if (data.containsKey(VmSetDefaultL3NetworkOnAttachingFlow.class)) {
            VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
            vm.setDefaultL3NetworkUuid(null);
            dbf.update(vm);
        }

        trigger.rollback();
    }
}
