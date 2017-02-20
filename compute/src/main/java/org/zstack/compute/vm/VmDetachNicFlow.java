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
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.Map;

/**
 * Created by frank on 7/18/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDetachNicFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final VmNicInventory nic = spec.getDestNics().get(0);
        String defaultL3Uuid = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class).getDefaultL3NetworkUuid();

        if (defaultL3Uuid != null && defaultL3Uuid.equals(nic.getL3NetworkUuid())) {
            // reset the default L3 to a l3 network; if there is no other l3, the default l3 will be null
            String l3Uuid = CollectionUtils.find(spec.getVmInventory().getVmNics(), new Function<String, VmNicInventory>() {
                @Override
                public String call(VmNicInventory arg) {
                    return arg.getUuid().equals(nic.getUuid()) ? null : arg.getL3NetworkUuid();
                }
            });

            VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
            vm.setDefaultL3NetworkUuid(l3Uuid);
            dbf.update(vm);
        }

        ReturnIpMsg msg = new ReturnIpMsg();
        msg.setUsedIpUuid(nic.getUsedIpUuid());
        msg.setL3NetworkUuid(nic.getL3NetworkUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, nic.getL3NetworkUuid());
        bus.send(msg);

        dbf.removeByPrimaryKey(nic.getUuid(), VmNicVO.class);
        trigger.next();
    }
}
