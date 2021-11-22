package org.zstack.storage.primary.nfs;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.PrimaryStorageBase.PhysicalCapacityUsage;

public interface NfsPrimaryStorageBackend {
    HypervisorType getHypervisorType();

    void ping(PrimaryStorageInventory inv, Completion completion);

    void handle(PrimaryStorageInventory inv, CreateTemporaryVolumeFromSnapshotMsg msg, ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion);

    void handle(PrimaryStorageInventory inv, CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion);

    void handle(PrimaryStorageInventory inv, UploadBitsToBackupStorageMsg msg, ReturnValueCompletion<UploadBitsToBackupStorageReply> completion);

    void handle(PrimaryStorageInventory inv, SyncVolumeSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion);

    void handle(PrimaryStorageInventory inv, GetVolumeRootImageUuidFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply> completion);

    void handle(PrimaryStorageInventory inv, NfsToNfsMigrateBitsMsg msg, ReturnValueCompletion<NfsToNfsMigrateBitsReply> completion);

    void handle(PrimaryStorageInventory inv, NfsRebaseVolumeBackingFileMsg msg, ReturnValueCompletion<NfsRebaseVolumeBackingFileReply> completion);

    void handle(PrimaryStorageInventory inv, AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion);

    void handle(PrimaryStorageInventory inv, DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    void handle(PrimaryStorageInventory inv, CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    void handle(PrimaryStorageInventory inv, GetDownloadBitsFromKVMHostProgressMsg msg, ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion);

    void handle(PrimaryStorageInventory inv, ChangeVolumeTypeOnPrimaryStorageMsg msg, ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply> changeVolumeTypeOnPrimaryStorageReplyReturnValueCompletion);

    void handle(PrimaryStorageInventory inv, GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion);

    void getPhysicalCapacity(PrimaryStorageInventory inv, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    void checkIsBitsExisting(PrimaryStorageInventory inv, String installPath, ReturnValueCompletion<Boolean> completion);

    void attachToCluster(PrimaryStorageInventory inv, String clusterUuid, ReturnValueCompletion<Boolean> completion);

    void detachFromCluster(PrimaryStorageInventory inv, String clusterUuid) throws NfsPrimaryStorageException;

    void createMemoryVolume(PrimaryStorageInventory pinv, VolumeInventory volume, ReturnValueCompletion<String> completion);

    void instantiateVolume(PrimaryStorageInventory pinv, VolumeInventory volume, ReturnValueCompletion<VolumeInventory> complete);

    void deleteImageCache(ImageCacheInventory imageCache);

    void delete(PrimaryStorageInventory pinv, String installPath, Completion completion);

    void deleteFolder(PrimaryStorageInventory pinv, String installPath, Completion completion);

    void revertVolumeFromSnapshot(VolumeSnapshotInventory sinv, VolumeInventory vol, HostInventory host, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion);

    void resetRootVolumeFromImage(VolumeInventory vol, HostInventory host, ReturnValueCompletion<String> completion);

    void createVolumeFromImageCache(PrimaryStorageInventory primaryStorage, ImageCacheInventory image, VolumeInventory volume, ReturnValueCompletion<String> completion);

    void createImageCacheFromVolumeResource(PrimaryStorageInventory primaryStorage, String volumeResourceInstallPath, ImageInventory image, ReturnValueCompletion<BitsInfo> completion);

    void createTemplateFromVolume(PrimaryStorageInventory primaryStorage, VolumeInventory volume, ImageInventory image, ReturnValueCompletion<BitsInfo> completion);

    void mergeSnapshotToVolume(PrimaryStorageInventory pinv, VolumeSnapshotInventory snapshot, VolumeInventory volume, boolean fullRebase, Completion completion);

    void remount(PrimaryStorageInventory pinv, String clusterUuid, Completion completion);

    void updateMountPoint(PrimaryStorageInventory pinv, String clusterUuid, String oldMountPoint, String newMountPoint, Completion completion);

    class BitsInfo {
        private String installPath;
        private long size;
        private long actualSize;

        public long getActualSize() {
            return actualSize;
        }

        public void setActualSize(long actualSize) {
            this.actualSize = actualSize;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        BitsInfo(String installPath, long size, long actualSize) {
            this.installPath = installPath;
            this.size = size;
            this.actualSize = actualSize;
        }
    }
}
