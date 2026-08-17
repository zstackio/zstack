package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.kvm.tpm.TpmStateVmHostBackupFileBase;
import org.zstack.kvm.tpm.TpmStateVmHostFileBase;
import org.zstack.kvm.vmfiles.AbstractVmHostBackupFileBase;
import org.zstack.kvm.vmfiles.AbstractVmHostFileBase;
import org.zstack.resourceconfig.ResourceConfigFacade;

import java.util.Set;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class KvmVmHostFileFactory {
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private VmTpmManager vmTpmManager;

    public AbstractVmHostFileBase createBase(VmHostFileVO file) {
        switch (file.getType()) {
        case NvRam: return new NvRamVmHostFileBase(file);
        case TpmState: return new TpmStateVmHostFileBase(file);
        default: throw new OperationFailureException(operr(ORG_ZSTACK_KVM_10163,
                "invalid VM host file type: %s", file.getType()));
        }
    }

    public AbstractVmHostBackupFileBase createBackupBase(VmHostBackupFileVO backupFile) {
        switch (backupFile.getType()) {
        case NvRam: return new NvRamVmHostBackupFileBase(backupFile);
        case TpmState: return new TpmStateVmHostBackupFileBase(backupFile);
        default: throw new OperationFailureException(operr(ORG_ZSTACK_KVM_10163,
                "invalid VM host file type: %s", backupFile.getType()));
        }
    }

    public Set<VmHostFileType> vmHostFileTypeNeedRegisterForVm(String vmUuid) {
        return vmTpmManager.vmHostFileTypeNeedRegisterForVm(vmUuid);
    }

    public boolean needRegister(VmHostFileType type, String vmUuid) {
        return vmTpmManager.needRegister(type, vmUuid);
    }
}
