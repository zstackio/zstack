package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.TakeSnapshotOnHypervisorMsg;
import org.zstack.header.storage.snapshot.BeforeTakeLiveSnapshotsOnVolumes;
import org.zstack.header.storage.snapshot.ConsistentType;
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotOverlayInnerMsg;
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmMsg;
import org.zstack.header.storage.snapshot.TakeVolumesSnapshotOnKvmReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotAfterDeleteExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotCreationExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.header.volume.CreateVolumeSnapshotGroupMessage;
import org.zstack.header.volume.VolumeLuksAgentSpec;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMTakeSnapshotExtensionPoint;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.VolumeTO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class VolumeSnapshotEncryptionExtension implements KVMTakeSnapshotExtensionPoint,
        BeforeTakeLiveSnapshotsOnVolumes, VolumeSnapshotCreationExtensionPoint,
        VolumeSnapshotAfterDeleteExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotEncryptionExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;
    @Autowired
    private VolumeEncryptedResourceKeyBackend volumeEncryptedResourceKeyBackend;
    @Autowired
    private VolumeEncryptedSecretHelper volumeEncryptedSecretHelper;

    @Override
    public void beforeTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg,
                                   KVMAgentCommands.TakeSnapshotCmd cmd, Completion completion) {
        try {
            VolumeVO volume = findVolume(msg.getVolume().getUuid());
            if (!volume.isEncrypted()) {
                completion.success();
                return;
            }

            VolumeSnapshotInventory snapshot = findSnapshot(msg.getSnapshotName());
            snapshotEncryptionHelper.inheritVolumeKeyToSnapshot(volume, snapshot);
            if (!cmd.isOnline()) {
                VolumeLuksAgentSpec luksSpec =
                        snapshotEncryptionHelper.prepareVolumeSecretMaterial(host.getUuid(), volume.getUuid());
                if (luksSpec != null && StringUtils.isNotBlank(luksSpec.getEncryptLuksSecretMaterialFilePath())) {
                    cmd.setEncryptLuksSecretMaterialFilePath(luksSpec.getEncryptLuksSecretMaterialFilePath());
                }

                if (msg.isFullSnapshot()) {
                    // The two secret material files carry the same DEK. They are separate
                    // because the file is read-once and deleted after use; offline full
                    // snapshot runs two qemu-img operations, so each operation needs its
                    // own one-shot file path.
                    VolumeLuksAgentSpec snapshotLuksSpec =
                        snapshotEncryptionHelper.prepareVolumeSecretMaterial(host.getUuid(), volume.getUuid());
                    if (snapshotLuksSpec != null &&
                            StringUtils.isNotBlank(snapshotLuksSpec.getEncryptLuksSecretMaterialFilePath())) {
                        cmd.setFullSnapshotLuksSecretMaterialFilePath(
                                snapshotLuksSpec.getEncryptLuksSecretMaterialFilePath());
                    }
                }
            }

            completion.success();
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            completion.fail(operr("failed to prepare encrypted volume snapshot[uuid:%s] on host[uuid:%s]: %s",
                    msg.getSnapshotName(), host.getUuid(), e.getMessage()));
        }
    }

    @Override
    public void beforeTakeLiveSnapshotsOnVolumes(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                 TakeVolumesSnapshotOnKvmMsg tmsg,
                                                 Map flowData,
                                                 Completion completion) {
        try {
            if (tmsg == null || tmsg.getSnapshotJobs() == null) {
                completion.success();
                return;
            }

            for (TakeSnapshotsOnKvmJobStruct job : tmsg.getSnapshotJobs()) {
                if (job.isMemory()) {
                    continue;
                }

                VolumeVO volume = findVolume(job.getVolumeUuid());
                if (!volume.isEncrypted()) {
                    continue;
                }

                VolumeSnapshotInventory snapshot = findSnapshot(job.getSnapshotUuid());
                snapshotEncryptionHelper.inheritVolumeKeyToSnapshot(volume, snapshot);

                VolumeLuksAgentSpec luksSpec =
                        snapshotEncryptionHelper.prepareVolumeSecretMaterial(tmsg.getHostUuid(), volume.getUuid());
                // This extension runs in a chain with storage-specific implementations;
                // keep any secret path already prepared by a storage backend.
                boolean needFillEncryptSecret = StringUtils.isBlank(job.getEncryptLuksSecretMaterialFilePath());
                if (needFillEncryptSecret && luksSpec != null &&
                        StringUtils.isNotBlank(luksSpec.getEncryptLuksSecretMaterialFilePath())) {
                    job.setEncryptLuksSecretMaterialFilePath(luksSpec.getEncryptLuksSecretMaterialFilePath());
                }

                if (job.getVolume() instanceof VolumeTO) {
                    ((VolumeTO) job.getVolume()).setLuksSecretUuid(
                            volumeEncryptedSecretHelper.resolveOrDefineSecretForVolume(
                                    tmsg.getHostUuid(), volume.getVmInstanceUuid(), volume.getUuid()));
                }
            }

            completion.success();
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            completion.fail(operr("failed to prepare encrypted live volume snapshots: %s", e.getMessage()));
        }
    }

    @Override
    public void afterVolumeSnapshotCreated(VolumeSnapshotInventory snapshot, Completion completion) {
        try {
            if (snapshot == null) {
                completion.success();
                return;
            }

            VolumeVO volume = findVolume(snapshot.getVolumeUuid());
            if (!volume.isEncrypted()) {
                completion.success();
                return;
            }

            snapshotEncryptionHelper.completeTakeSnapshot(volume, snapshot);
            completion.success();
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        } catch (RuntimeException e) {
            completion.fail(operr("failed to complete encrypted volume snapshot[uuid:%s]: %s",
                    snapshot == null ? null : snapshot.getUuid(), e.getMessage()));
        }
    }

    @Override
    public void afterTakeSnapshot(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg,
                                  KVMAgentCommands.TakeSnapshotCmd cmd,
                                  KVMAgentCommands.TakeSnapshotResponse rsp) {
    }

    @Override
    public void afterTakeSnapshotFailed(KVMHostInventory host, TakeSnapshotOnHypervisorMsg msg,
                                        KVMAgentCommands.TakeSnapshotCmd cmd,
                                        KVMAgentCommands.TakeSnapshotResponse rsp,
                                        org.zstack.header.errorcode.ErrorCode err) {
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreatedOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                             TakeVolumesSnapshotOnKvmReply treply,
                                                             Completion completion) {
        completion.success();
    }

    @Override
    public void afterVolumeLiveSnapshotGroupCreationFailsOnBackend(CreateVolumesSnapshotOverlayInnerMsg msg,
                                                                   TakeVolumesSnapshotOnKvmReply treply) {
    }

    @Override
    public void afterVolumeSnapshotGroupCreated(VolumeSnapshotGroupInventory snapshotGroup,
                                                ConsistentType consistentType,
                                                Completion completion) {
        completion.success();
    }

    @Override
    public List<Flow> beforeCreateVolumeSnapshotFlow(CreateVolumeSnapshotGroupMessage msg) {
        return null;
    }

    @Override
    public void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, Completion completion) {
        completion.success();
    }

    @Override
    public void volumeSnapshotAfterFailedDeleteExtensionPoint(VolumeSnapshotInventory snapshot) {
    }

    @Override
    public void volumeSnapshotAfterCleanUpExtensionPoint(String volumeUuid, List<VolumeSnapshotInventory> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        for (VolumeSnapshotInventory snapshot : snapshots) {
            if (snapshot == null || StringUtils.isBlank(snapshot.getUuid())) {
                continue;
            }
            if (dbf.isExist(snapshot.getUuid(), VolumeSnapshotVO.class)) {
                continue;
            }
            try {
                volumeEncryptedResourceKeyBackend.detachKeyProviderFromSnapshot(snapshot.getUuid());
            } catch (RuntimeException e) {
                logger.warn(String.format(
                        "failed to detach EncryptedResourceKeyRefVO for volume snapshot[uuid:%s] on delete cleanup: %s",
                        snapshot.getUuid(), e.getMessage()));
            }
        }
    }

    private VolumeVO findVolume(String volumeUuid) {
        if (StringUtils.isBlank(volumeUuid)) {
            throw new OperationFailureException(operr("volume uuid is required for encrypted snapshot preparation"));
        }

        VolumeVO volume = dbf.findByUuid(volumeUuid, VolumeVO.class);
        if (volume == null) {
            throw new OperationFailureException(operr("volume[uuid:%s] not found", volumeUuid));
        }
        return volume;
    }

    private VolumeSnapshotInventory findSnapshot(String snapshotUuid) {
        if (StringUtils.isBlank(snapshotUuid)) {
            throw new OperationFailureException(operr("snapshot uuid is required for encrypted snapshot preparation"));
        }

        VolumeSnapshotVO snapshot = dbf.findByUuid(snapshotUuid, VolumeSnapshotVO.class);
        if (snapshot == null) {
            throw new OperationFailureException(operr("volume snapshot[uuid:%s] not found", snapshotUuid));
        }
        return VolumeSnapshotInventory.valueOf(snapshot);
    }
}
