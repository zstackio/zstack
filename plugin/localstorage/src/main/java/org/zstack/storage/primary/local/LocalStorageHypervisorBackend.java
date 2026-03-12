package org.zstack.storage.primary.local;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.AskInstallPathForNewSnapshotMsg;
import org.zstack.header.storage.primary.AskInstallPathForNewSnapshotReply;
import org.zstack.header.storage.primary.BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg;
import org.zstack.header.storage.primary.BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply;
import org.zstack.header.storage.primary.CancelDownloadBitsFromKVMHostToPrimaryStorageMsg;
import org.zstack.header.storage.primary.CancelDownloadBitsFromKVMHostToPrimaryStorageReply;
import org.zstack.header.storage.primary.ChangeVolumeTypeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ChangeVolumeTypeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.CheckSnapshotMsg;
import org.zstack.header.storage.primary.CommitVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.CommitVolumeSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.CreateImageCacheFromVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.CreateImageCacheFromVolumeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.DeleteBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteBitsOnPrimaryStorageReply;
import org.zstack.header.storage.primary.DeleteImageCacheOnPrimaryStorageReply;
import org.zstack.header.storage.primary.DeleteIsoFromPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteIsoFromPrimaryStorageReply;
import org.zstack.header.storage.primary.DeleteSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteVolumeBitsOnPrimaryStorageReply;
import org.zstack.header.storage.primary.DeleteVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.DeleteVolumeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.DownloadBitsFromKVMHostToPrimaryStorageMsg;
import org.zstack.header.storage.primary.DownloadBitsFromKVMHostToPrimaryStorageReply;
import org.zstack.header.storage.primary.DownloadDataVolumeToPrimaryStorageMsg;
import org.zstack.header.storage.primary.DownloadDataVolumeToPrimaryStorageReply;
import org.zstack.header.storage.primary.DownloadIsoToPrimaryStorageMsg;
import org.zstack.header.storage.primary.DownloadIsoToPrimaryStorageReply;
import org.zstack.header.storage.primary.DownloadVolumeTemplateToPrimaryStorageMsg;
import org.zstack.header.storage.primary.DownloadVolumeTemplateToPrimaryStorageReply;
import org.zstack.header.storage.primary.GetDownloadBitsFromKVMHostProgressMsg;
import org.zstack.header.storage.primary.GetDownloadBitsFromKVMHostProgressReply;
import org.zstack.header.storage.primary.GetInstallPathForDataVolumeDownloadMsg;
import org.zstack.header.storage.primary.GetInstallPathForDataVolumeDownloadReply;
import org.zstack.header.storage.primary.GetVolumeBackingChainFromPrimaryStorageMsg;
import org.zstack.header.storage.primary.GetVolumeBackingChainFromPrimaryStorageReply;
import org.zstack.header.storage.primary.GetVolumeRootImageUuidFromPrimaryStorageMsg;
import org.zstack.header.storage.primary.GetVolumeRootImageUuidFromPrimaryStorageReply;
import org.zstack.header.storage.primary.GetVolumeSnapshotEncryptedOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.GetVolumeSnapshotEncryptedOnPrimaryStorageReply;
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PullVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PullVolumeSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.ReInitRootVolumeFromTemplateOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ReInitRootVolumeFromTemplateOnPrimaryStorageReply;
import org.zstack.header.storage.primary.RevertVolumeFromSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.RevertVolumeFromSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.SyncVolumeSizeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.SyncVolumeSizeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.TakeSnapshotMsg;
import org.zstack.header.storage.primary.TakeSnapshotReply;
import org.zstack.header.storage.primary.UnlinkBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.UnlinkBitsOnPrimaryStorageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.BatchSyncVolumeSizeOnPrimaryStorageMsg;
import org.zstack.header.volume.BatchSyncVolumeSizeOnPrimaryStorageReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStats;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;

import java.util.List;

/**
 * Created by frank on 6/30/2015.
 */
public abstract class LocalStorageHypervisorBackend extends LocalStorageBase {
    public LocalStorageHypervisorBackend() {
    }

    public LocalStorageHypervisorBackend(PrimaryStorageVO self) {
        super(self);
    }

    abstract void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    abstract void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply> completion);

    abstract void handle(DeleteVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadDataVolumeToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion);

    abstract void handle(GetInstallPathForDataVolumeDownloadMsg msg, ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply> completion);

    abstract void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply> completion);

    abstract void handle(DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion);

    abstract void handle(DownloadIsoToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion);

    abstract void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion);

    abstract void handle(InitPrimaryStorageOnHostConnectedMsg msg, ReturnValueCompletion<PhysicalCapacityUsage> completion);

    abstract void handle(TakeSnapshotMsg msg, String hostUuid, ReturnValueCompletion<TakeSnapshotReply> completion);

    abstract void handle(CheckSnapshotMsg msg, String hostUuid, Completion completion);

    abstract void handle(DeleteSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion);

    abstract void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, String hostUuid, ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion);

    abstract void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void stream(VolumeSnapshotInventory from, VolumeInventory to, boolean fullRebase, String hostUuid, Completion completion);

    abstract void handle(LocalStorageCreateEmptyVolumeMsg msg, ReturnValueCompletion<LocalStorageCreateEmptyVolumeReply> completion);

    abstract void handle(LocalStorageDirectlyDeleteBitsMsg msg, String hostUuid, ReturnValueCompletion<LocalStorageDirectlyDeleteBitsReply> completion);

    abstract void handle(CreateTemporaryVolumeFromSnapshotMsg msg, String hostUuid, ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion);

    abstract void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg, String huuid, ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply> completion);

    abstract void handle(BatchSyncVolumeSizeOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<BatchSyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply> completion);

    abstract void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion);

    abstract void handle(UploadBitsFromLocalStorageToBackupStorageMsg msg, String hostUuid, ReturnValueCompletion<UploadBitsFromLocalStorageToBackupStorageReply> completion);

    abstract void handle(GetVolumeRootImageUuidFromPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply> completion);

    abstract void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply> returnValueCompletion);

    abstract void handleHypervisorSpecificMessage(LocalStorageHypervisorSpecificMessage msg);

    abstract void downloadImageToCache(ImageInventory img, String hostUuid, ReturnValueCompletion<String> completion);

    abstract void handle(LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<DeleteImageCacheOnPrimaryStorageReply> completion);

    abstract void handle(AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion);

    abstract void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    abstract void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<CancelDownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    abstract void handle(ChangeVolumeTypeOnPrimaryStorageMsg msg, ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply> completion);

    abstract void handle(UnlinkBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<UnlinkBitsOnPrimaryStorageReply> completion);

    abstract List<Flow> createMigrateBitsVolumeFlow(MigrateBitsStruct struct);

    abstract void deleteBits(String path, String hostUuid, Completion completion);

    abstract void createEmptyVolume(VolumeInventory volume, String hostUuid, ReturnValueCompletion<VolumeStats> completion);

    abstract void createEmptyVolumeWithBackingFile(VolumeInventory volume, String hostUuid, String backingFile, ReturnValueCompletion<VolumeStats> completion);

    abstract void checkHostAttachedPSMountPath(String hostUuid, ReturnValueCompletion<LocalStorageKvmBackend.CheckInitializedFileRsp> completion);

    abstract void initializeHostAttachedPSMountPath(String hostUuid, Completion completion);

    abstract void handle(GetDownloadBitsFromKVMHostProgressMsg msg, ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion);

    abstract void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion);

    abstract void handle(CommitVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(PullVolumeSnapshotOnPrimaryStorageMsg msg, String hostUuid, ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply> completion);
}
