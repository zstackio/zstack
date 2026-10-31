package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;

public interface ExternalVolumeCreationExtensionPoint {
    boolean canHandleVolumeInstantiation(PrimaryStorageControllerSvc controller, VolumeInventory volume);

    boolean supportsDataTemplateEncryption(PrimaryStorageControllerSvc controller);

    void createEmpty(PrimaryStorageControllerSvc controller, String primaryStorageUuid,
                     CreateVolumeSpec spec, String hostUuid, ReturnValueCompletion<VolumeStats> completion);

    void cloneRoot(PrimaryStorageControllerSvc controller, String primaryStorageUuid,
                   String sourceInstallPath, CreateVolumeSpec spec, String hostUuid,
                   ReturnValueCompletion<VolumeStats> completion);

    void encryptDataTemplate(PrimaryStorageControllerSvc controller, EncryptVolumeBitsOnPrimaryStorageMsg msg,
                             ReturnValueCompletion<EncryptVolumeBitsOnPrimaryStorageReply> completion);
}
