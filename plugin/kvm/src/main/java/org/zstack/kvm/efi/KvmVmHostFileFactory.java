package org.zstack.kvm.efi;

import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.kvm.tpm.TpmStateVmHostBackupFileBase;
import org.zstack.kvm.tpm.TpmStateVmHostFileBase;

import static org.zstack.core.Platform.operr;

public class KvmVmHostFileFactory {
    public AbstractVmHostFileBase createBase(VmHostFileVO file) {
        switch (file.getType()) {
        case NvRam: return new NvRamVmHostFileBase(file);
        case TpmState: return new TpmStateVmHostFileBase(file);
        default: throw operr("invalid VM host file type: " + file.getType()).toException();
        }
    }

    public AbstractVmHostBackupFileBase createBackupBase(VmHostBackupFileVO backupFile) {
        switch (backupFile.getType()) {
        case NvRam: return new NvRamVmHostBackupFileBase(backupFile);
        case TpmState: return new TpmStateVmHostBackupFileBase(backupFile);
        default: throw operr("invalid VM host file type: " + backupFile.getType()).toException();
        }
    }
}
