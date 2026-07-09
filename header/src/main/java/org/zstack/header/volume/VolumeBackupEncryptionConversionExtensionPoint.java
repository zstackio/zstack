package org.zstack.header.volume;

import org.zstack.header.core.ReturnValueCompletion;

public interface VolumeBackupEncryptionConversionExtensionPoint {
    void prepareVolumeBackupEncryptionConversion(VolumeInventory volume, boolean encrypted, String hostUuid,
                                                 ReturnValueCompletion<VolumeBackupEncryptionConversionContext> completion);

    void commitVolumeBackupEncryptionConversion(VolumeBackupEncryptionConversionContext context);

    void rollbackVolumeBackupEncryptionConversion(VolumeBackupEncryptionConversionContext context);

    void afterVolumeBackupEncryptionConversionCommitted(VolumeBackupEncryptionConversionContext context);
}
