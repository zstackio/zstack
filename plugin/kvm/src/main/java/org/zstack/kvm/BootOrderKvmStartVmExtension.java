package org.zstack.kvm;

import org.zstack.compute.vm.VmSystemTags;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;

/**
 * author:kaicai.hu
 * Date:2019/12/25
 */
public class BootOrderKvmStartVmExtension implements KVMStartVmExtensionPoint {

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
        if (VmSystemTags.BOOT_ORDER_ONCE.hasTag(spec.getVmInventory().getUuid(), VmInstanceVO.class)) {
            VmSystemTags.BOOT_ORDER.deleteInherentTag(spec.getVmInventory().getUuid());
            VmSystemTags.BOOT_ORDER_ONCE.deleteInherentTag(spec.getVmInventory().getUuid());
        }
        if (VmSystemTags.CDROM_BOOT_ONCE.hasTag(spec.getVmInventory().getUuid(), VmInstanceVO.class)) {
            VmSystemTags.BOOT_ORDER.deleteInherentTag(spec.getVmInventory().getUuid());
            VmSystemTags.CDROM_BOOT_ONCE.deleteInherentTag(spec.getVmInventory().getUuid());
        }
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {

    }
}
