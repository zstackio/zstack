package org.zstack.kvm;

import org.zstack.compute.vm.VmSystemTags;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTagCreator;

import static org.zstack.kvm.KVMSystemTags.EDK_RPM_TOKEN;
import static org.zstack.kvm.KVMSystemTags.VM_EDK;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * author:kaicai.hu
 * Date:2019/12/25
 */
public class BootKvmStartVmExtension implements KVMStartVmExtensionPoint, KVMSyncVmDeviceInfoExtensionPoint {

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
        if (VmSystemTags.BOOT_ORDER_ONCE.hasTag(spec.getVmInventory().getUuid(), VmInstanceVO.class)) {
            VmSystemTags.BOOT_ORDER.delete(spec.getVmInventory().getUuid());
            VmSystemTags.BOOT_ORDER_ONCE.delete(spec.getVmInventory().getUuid());
        }
        if (VmSystemTags.CDROM_BOOT_ONCE.hasTag(spec.getVmInventory().getUuid(), VmInstanceVO.class)) {
            VmSystemTags.BOOT_ORDER.delete(spec.getVmInventory().getUuid());
            VmSystemTags.CDROM_BOOT_ONCE.delete(spec.getVmInventory().getUuid());
        }
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {

    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {

    }

    @Override
    public void afterReceiveVmDeviceInfoResponse(VmInstanceInventory vm, KVMAgentCommands.VmDevicesInfoResponse rsp, VmInstanceSpec spec) {
        saveVmEdkStatesFromCommand(spec.getVmInventory().getUuid(), rsp);
    }

    @SuppressWarnings("unchecked")
    private void saveVmEdkStatesFromCommand(String vmUuid, KVMAgentCommands.VmDevicesInfoResponse rsp) {
        if (rsp.getEdkRpm() == null) {
            VM_EDK.deleteInherentTag(vmUuid);
            return;
        }

        SystemTagCreator creator = VM_EDK.newSystemTagCreator(vmUuid);
        creator.setTagByTokens(map(e(EDK_RPM_TOKEN, rsp.getEdkRpm())));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();
    }
}
