package org.zstack.storage.primary.local;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.*;

import java.util.List;

/**
 * Created by frank on 6/30/2015.
 */
public abstract class LocalStorageHypervisorBackend extends LocalStorageBase {
    public LocalStorageHypervisorBackend(PrimaryStorageVO self) {
        super(self);
    }

    abstract void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    abstract void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DeleteVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadDataVolumeToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion);

    abstract void handle(DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion);

    abstract void handle(DownloadIsoToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion);

    abstract void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion);

    abstract void handle(InitPrimaryStorageOnHostConnectedMsg msg, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    abstract void handle(TakeSnapshotMsg msg, String hostUuid, ReturnValueCompletion<TakeSnapshotReply> completion);

    abstract void handle(DeleteSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion);

    abstract void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, String hostUuid, ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion);

    abstract void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(MergeVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(LocalStorageCreateEmptyVolumeMsg msg, ReturnValueCompletion<LocalStorageCreateEmptyVolumeReply> completion);

    abstract void handle(LocalStorageDirectlyDeleteBitsMsg msg, String hostUuid, ReturnValueCompletion<LocalStorageDirectlyDeleteBitsReply> completion);

    abstract void handle(CreateTemporaryVolumeFromSnapshotMsg msg, String hostUuid, ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion);

    abstract void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(UploadBitsFromLocalStorageToBackupStorageMsg msg, String hostUuid, ReturnValueCompletion<UploadBitsFromLocalStorageToBackupStorageReply> completion);

    abstract void handle(GetVolumeRootImageUuidFromPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply> completion);

    abstract void handleHypervisorSpecificMessage(LocalStorageHypervisorSpecificMessage msg);

    abstract void downloadImageToCache(ImageInventory img, String hostUuid, ReturnValueCompletion<String> completion);

    abstract void handle(LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<DeleteImageCacheOnPrimaryStorageReply> completion);

    abstract List<Flow> createMigrateBitsFlow(MigrateBitsStruct struct);

    abstract void deleteBits(String path, String hostUuid, Completion completion);
}
