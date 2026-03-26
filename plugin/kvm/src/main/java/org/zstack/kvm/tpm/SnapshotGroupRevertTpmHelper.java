package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.tpm.entity.TpmSpec;
import org.zstack.header.vm.APICreateVmInstanceFromVolumeSnapshotGroupMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.devices.NvRamSpec;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SnapshotGroupRevertTpmHelper {
    private static final CLogger logger = Utils.getLogger(SnapshotGroupRevertTpmHelper.class);

    @Autowired
    ResourceConfigFacade resourceConfigFacade;

    public void setupFromApi(APICreateVmInstanceFromVolumeSnapshotGroupMsg apiMsg, CreateVmInstanceMsg cmsg) {
        String snapshotGroupUuid = apiMsg.getVolumeSnapshotGroupUuid();

        boolean resetTpm;
        if (apiMsg.getResetTpm() != null) {
            resetTpm = apiMsg.getResetTpm();
        } else {
            String vmInstanceUuid = Q.New(VolumeSnapshotGroupVO.class)
                    .select(VolumeSnapshotGroupVO_.vmInstanceUuid)
                    .eq(VolumeSnapshotGroupVO_.uuid, snapshotGroupUuid)
                    .findValue();
            resetTpm = resourceConfigFacade.getResourceConfigValue(
                    VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE, vmInstanceUuid, Boolean.class);
            logger.debug(String.format("resetTpm not specified in API, resolved from resource config " +
                    "RESET_TPM_AFTER_VM_CLONE for vm[uuid:%s]: %s", vmInstanceUuid, resetTpm));
        }

        List<VmHostBackupFileVO> backupFiles = Q.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, snapshotGroupUuid)
                .list();

        if (backupFiles.isEmpty()) {
            logger.debug(String.format(
                    "no VmHostBackupFileVO found for volume snapshot group[uuid:%s], skip restoring TPM/NvRam",
                    snapshotGroupUuid));
            return;
        }

        VmHostBackupFileVO tpmBackupFile = null;
        VmHostBackupFileVO nvRamBackupFile = null;
        for (VmHostBackupFileVO f : backupFiles) {
            if (f.getType() == VmHostFileType.TpmState) {
                tpmBackupFile = f;
            } else if (f.getType() == VmHostFileType.NvRam) {
                nvRamBackupFile = f;
            }
        }

        if (tpmBackupFile == null && nvRamBackupFile == null) {
            logger.debug(String.format("no TpmState or NvRam backup file found for volume snapshot group[uuid:%s]",
                    snapshotGroupUuid));
            return;
        }

        VmDevicesSpec devicesSpec = cmsg.getDevicesSpec();
        if (devicesSpec == null) {
            devicesSpec = new VmDevicesSpec();
            cmsg.setDevicesSpec(devicesSpec);
        }

        if (tpmBackupFile != null) {
            TpmSpec tpmSpec = devicesSpec.getTpm();
            if (tpmSpec == null) {
                tpmSpec = new TpmSpec();
                devicesSpec.setTpm(tpmSpec);
            }
            tpmSpec.setEnable(true);

            if (resetTpm) {
                // resetTpm=true: reset secretUuid, generate a new one during VM creation
                logger.debug(String.format("resetTpm is true for volume snapshot group[uuid:%s], " +
                        "will reset secretUuid, tpmBackupFileUuid:%s", snapshotGroupUuid, tpmBackupFile.getUuid()));
            } else {
                tpmSpec.setBackupFileUuid(tpmBackupFile.getUuid());
                // resetTpm=false: should reuse secretUuid + keyProviderUuid recorded in VolumeSnapshotGroup,
                // but the recording step is not yet implemented, leave them empty for now
                // TODO: retrieve secretUuid and keyProviderUuid from VolumeSnapshotGroup and set them here
                logger.warn(String.format("resetTpm is false for volume snapshot group[uuid:%s], " +
                        "should restore secretUuid and keyProviderUuid but they are not yet recorded in snapshot group, " +
                        "leaving empty. tpmBackupFileUuid:%s", snapshotGroupUuid, tpmBackupFile.getUuid()));
            }
        }

        if (nvRamBackupFile != null) {
            NvRamSpec nvRamSpec = devicesSpec.getNvRam();
            if (nvRamSpec == null) {
                nvRamSpec = new NvRamSpec();
                devicesSpec.setNvRam(nvRamSpec);
            }
            nvRamSpec.setBackupFileUuid(nvRamBackupFile.getUuid());
            logger.debug(String.format("set NvRam restore info for volume snapshot group[uuid:%s], nvRamBackupFileUuid:%s",
                    snapshotGroupUuid, nvRamBackupFile.getUuid()));
        }
    }
}
