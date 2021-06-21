package org.zstack.kvm;

import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;

/**
 * Created by Wenhao.Zhang on 21/06/21
 */
public class PVPanicStartVmExtension implements KVMStartVmAddonExtensionPoint {
    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return VmInstanceType.valueOf(VmInstanceConstant.USER_VM_TYPE);
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (isPVPanicEnable()) {
            cmd.getAddons().putIfAbsent("pvpanic", Boolean.TRUE);
            cmd.getAddons().putIfAbsent("onCrash", "preserve");
        }
    }

    public boolean isPVPanicEnable() {
        return true; // TODO find in ResourceConfigVO
    }
}
