package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class KvmSecureBootExtensions implements KVMStartVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmSecureBootExtensions.class);

    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!isUefiBootMode(cmd.getBootMode())) {
            return;
        }

        ResourceConfig resourceConfig;
        resourceConfig = resourceConfigFacade.getResourceConfig(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity());
        cmd.setSecureBoot(resourceConfig.getResourceConfigValue(spec.getVmInventory().getUuid(), Boolean.class));

        resourceConfig = resourceConfigFacade.getResourceConfig(KVMGlobalConfig.VM_EDK_VERSION_CONFIG.getIdentity());
        final String edkVersion = resourceConfig.getResourceConfigValue(spec.getVmInventory().getUuid(), String.class);
        if (edkVersion != null && !edkVersion.isEmpty()) {
            cmd.setEdkVersion(edkVersion);
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
        // do-nothing
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        // do-nothing
    }

    private boolean isUefiBootMode(String bootMode) {
        return bootMode.equals(ImageBootMode.UEFI.toString()) || bootMode.equals(ImageBootMode.UEFI_WITH_CSM.toString());
    }
}
