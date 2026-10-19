package org.zstack.kvm;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.vm.VmNicDriverModel;
import org.zstack.header.vm.VmNicDriverType;
import org.zstack.header.vm.VmNicType;

import java.util.Map;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_VM_10346;

/** Validate after command extensions, when the actual guest device backend is known. */
public class KVMNicDriverValidator {
    public static void validate(KVMAgentCommands.NicTO nic, Map addons, List<String> supportedModels) {
        // Match the agent's hostdev precedence. Removing a NIC must not call this validator.
        if (nic.getPciDeviceAddress() != null && nic.getSrcPath() == null) {
            return;
        }
        boolean vhostUser = L2NetworkConstant.ACCEL_TYPE_VHOST_USER_SPACE.equals(nic.getType())
                || L2NetworkConstant.ACCEL_TYPE_VDPA.equals(nic.getType())
                || (addons != null && addons.get("vhostSrcPath") != null);
        VmNicDriverModel capability;
        try {
            capability = VmNicType.valueOf(nic.getType()).getDriverModel();
        } catch (IllegalArgumentException e) {
            capability = null;
        }
        String driver = nic.getDriverType() == null
                ? (Boolean.TRUE.equals(nic.getUseVirtio()) ? VmNicDriverType.VIRTIO.toString() : VmNicDriverType.E1000_KVM.toString()) : nic.getDriverType();
        if (capability == null || !supportedModels.contains(driver) || ((vhostUser || capability == VmNicDriverModel.Virtio) && !VmNicDriverType.VIRTIO.toString().equals(driver))) {
            throw new OperationFailureException(argerr(ORG_ZSTACK_COMPUTE_VM_10346,
                    "NIC[uuid:%s, type:%s] does not support driver model[%s]: %s",
                    nic.getUuid(), nic.getType(), driver,
                    capability == null ? "backend capability is not registered"
                            : !supportedModels.contains(driver) ? "unsupported NIC model" : "the effective backend requires virtio"));
        }
    }
}
