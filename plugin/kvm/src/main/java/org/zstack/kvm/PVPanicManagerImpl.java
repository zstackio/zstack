package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.CrashStrategy;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.resourceconfig.ResourceConfigFacade;

/**
 * Created by Wenhao.Zhang on 21/06/21
 */
public class PVPanicManagerImpl implements PVPanicManager,
        KVMStartVmAddonExtensionPoint {
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return VmInstanceType.valueOf(VmInstanceConstant.USER_VM_TYPE);
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (isPVPanicEnable(spec.getVmInventory().getUuid())) {
            cmd.getAddons().putIfAbsent("pvpanic", Boolean.TRUE);
            cmd.getAddons().putIfAbsent("onCrash", "preserve");
        }
    }

    @Override
    public boolean isPVPanicEnable(String vmUuid) {
        return !CrashStrategy.None.toString()
                .equals(rcf.getResourceConfigValue(VmGlobalConfig.VM_CRASH_STRATEGY, vmUuid, String.class));
    }
}
