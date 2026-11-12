package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.volume.VolumeStats;

public interface ExternalVolumeCreationExtensionPoint {
    boolean canHandleVolumeInstantiation(PrimaryStorageControllerSvc controller, VolumeInventory volume);

    boolean supportsDataTemplateEncryption(PrimaryStorageControllerSvc controller);

    void createEmpty(PrimaryStorageControllerSvc controller, String primaryStorageUuid,
                     CreateVolumeSpec spec, String hostUuid, ReturnValueCompletion<VolumeStats> completion);

    void cloneRoot(PrimaryStorageControllerSvc controller, String primaryStorageUuid,
                   String sourceInstallPath, CreateVolumeSpec spec, String hostUuid,
                   ReturnValueCompletion<VolumeStats> completion);

    default void cloneRoot(PrimaryStorageControllerSvc controller, String primaryStorageUuid,
                           String sourceImageUuid, String sourceInstallPath, CreateVolumeSpec spec,
                           String hostUuid, ReturnValueCompletion<VolumeStats> completion) {
        cloneRoot(controller, primaryStorageUuid, sourceInstallPath, spec, hostUuid, completion);
    }

    default boolean canHandleImageDownload(PrimaryStorageControllerSvc controller, ImageInventory image) {
        return false;
    }

    default void downloadImageBits(PrimaryStorageControllerSvc controller, String primaryStorageUuid, ImageInventory image,
                                   ImageBackupStorageRefInventory backupStorageRef, CreateVolumeSpec spec,
                                   String targetClz, ReturnValueCompletion<VolumeStats> completion) {
        throw new UnsupportedOperationException();
    }

    void encryptDataTemplate(PrimaryStorageControllerSvc controller, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                             ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion);
}
