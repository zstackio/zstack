package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.vm.VmErrors;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceNicFactory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicType;
import org.zstack.header.vm.VmNicVO;
import org.zstack.identity.Account;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


import static org.zstack.core.Platform.err;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;


public class TfVmNicFactory extends VmNicFactory {
    private static final CLogger logger = Utils.getLogger(TfVmNicFactory.class);
    private static final VSwitchType vSwitchType = new VSwitchType(VmInstanceConstant.L2_TF_VSWITCH_TYPE);
    private static final VmNicType type = new VmNicType(VmInstanceConstant.TF_VIRTUAL_NIC_TYPE);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public VmNicType getType() {
        type.setHasAddon(true);
        vSwitchType.addVmNicType(VmNicType.VmNicSubType.NONE, type);

        return type;
    }

    public VmNicVO createVmNic(VmNicInventory nic, VmInstanceSpec spec) {
        String acntUuid = Account.getAccountUuidOfResource(spec.getVmInventory().getUuid());

        VmNicVO vnic = VmInstanceNicFactory.createVmNic(nic);
        vnic.setType(type.toString());
        vnic.setAccountUuid(acntUuid);
        vnic = persistAndRetryIfMacCollision(vnic);
        if (vnic == null) {
            throw new FlowException(err(ORG_ZSTACK_COMPUTE_VM_10091, VmErrors.ALLOCATE_MAC_ERROR, "unable to find an available mac address after re-try 5 times, too many collisions"));
        }

        vnic = dbf.reload(vnic);
        spec.getDestNics().add(VmNicInventory.valueOf(vnic));
        logger.debug(String.format("Create TFVNIC [%s] success.", vnic.getUuid()));
        return vnic;
    }
}
