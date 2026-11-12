package org.zstack.storage.ceph.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.EncryptVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.errorcode.ErrorCode;

import java.util.Collection;

public interface CephVolumeCreationExtensionPoint {
    boolean canHandleVolumeInstantiation(VolumeInventory volume);

    void createEmpty(String primaryStorageUuid, String volumeUuid, String installPath, long size,
                     String preferredHostUuid, ReturnValueCompletion<Long> completion);

    void cloneRoot(String primaryStorageUuid, String volumeUuid, String sourcePath, String targetPath,
                   String hostUuid, Long virtualSize, ReturnValueCompletion<Long> completion);

    default boolean canHandleImageDownload(Collection<String> imageSystemTags) {
        return false;
    }

    default void downloadImageBits(String primaryStorageUuid, ImageInventory image,
                                   ImageBackupStorageRefInventory backupStorageRef, String targetPath,
                                   ReturnValueCompletion<VolumeStats> completion) {
        throw new UnsupportedOperationException();
    }

    default void cloneRoot(String primaryStorageUuid, String volumeUuid, String sourceImageUuid,
                            String sourcePath, String targetPath, String hostUuid, Long virtualSize,
                            ReturnValueCompletion<VolumeStats> completion) {
        cloneRoot(primaryStorageUuid, volumeUuid, sourcePath, targetPath, hostUuid, virtualSize,
                new ReturnValueCompletion<Long>(completion) {
                    @Override
                    public void success(Long actualSize) {
                        completion.success(new VolumeStats(targetPath, actualSize));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }

    void encryptDataTemplate(EncryptVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<Long> completion);
}
