package org.zstack.storage.ceph.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.volume.VolumeInventory;

public interface CephVolumeCreationExtensionPoint {
    boolean canHandleVolumeInstantiation(VolumeInventory volume);

    void createEmpty(String primaryStorageUuid, String volumeUuid, String installPath, long size,
                     String preferredHostUuid, ReturnValueCompletion<Long> completion);

    void cloneRoot(String primaryStorageUuid, String volumeUuid, String sourcePath, String targetPath,
                   String hostUuid, Long virtualSize, ReturnValueCompletion<Long> completion);

    void encryptDataTemplate(EncryptVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<Long> completion);
}
