package org.zstack.storage.primary.nfs;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.PrimaryStorageBase.PhysicalCapacityUsage;

import java.util.List;

public interface NfsPrimaryStorageBackend {
    public static class CreateBitsFromSnapshotResult {
        private String installPath;
        private long size;

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
    }

    HypervisorType getHypervisorType();

    void getPhysicalCapacity(PrimaryStorageInventory inv, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    void checkIsBitsExisting(PrimaryStorageInventory inv, String installPath, ReturnValueCompletion<Boolean> completion);

    void attachToCluster(PrimaryStorageInventory inv, String clusterUuid) throws NfsPrimaryStorageException;
    
    void detachFromCluster(PrimaryStorageInventory inv, String clusterUuid) throws NfsPrimaryStorageException;
    
    void instantiateVolume(PrimaryStorageInventory pinv, VolumeInventory volume, ReturnValueCompletion<VolumeInventory> complete);

    void deleteImageCache(ImageCacheInventory imageCache);

    void delete(PrimaryStorageInventory pinv, String installPath, Completion completion);

    void deleteFolder(PrimaryStorageInventory pinv, String installPath, Completion completion);

    void revertVolumeFromSnapshot(VolumeSnapshotInventory sinv, VolumeInventory vol, HostInventory host, ReturnValueCompletion<String> completion);

    void createTemplateFromVolume(PrimaryStorageInventory primaryStorage, VolumeInventory rootVolume, ImageInventory image, ReturnValueCompletion<String> completion);

    void createTemplateFromVolumeSnapshot(PrimaryStorageInventory pinv, List<SnapshotDownloadInfo> infos, String imageUuid,
                                          boolean needDownload, ReturnValueCompletion<CreateBitsFromSnapshotResult> completion);

    void createDataVolumeFromVolumeSnapshot(PrimaryStorageInventory pinv, List<SnapshotDownloadInfo> infos, String volumeUuid,
                                          boolean needDownload, ReturnValueCompletion<CreateBitsFromSnapshotResult> completion);

    void moveBits(PrimaryStorageInventory pinv, String srcPath, String destPath, Completion completion);

    void mergeSnapshotToVolume(PrimaryStorageInventory pinv, VolumeSnapshotInventory snapshot, VolumeInventory volume, boolean fullRebase, Completion completion);
}
