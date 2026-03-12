package org.zstack.storage.primary.smp;

import org.zstack.header.cluster.ClusterConnectionStatus;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.AskInstallPathForNewSnapshotMsg;
import org.zstack.header.storage.primary.AskInstallPathForNewSnapshotReply;
import org.zstack.header.storage.primary.BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg;
import org.zstack.header.storage.primary.BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply;
import org.zstack.header.storage.primary.CancelDownloadBitsFromKVMHostToPrimaryStorageMsg;
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
import org.zstack.header.storage.primary.GetVolumeSnapshotEncryptedOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.GetVolumeSnapshotEncryptedOnPrimaryStorageReply;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PullVolumeSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PullVolumeSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.ReInitRootVolumeFromTemplateOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ReInitRootVolumeFromTemplateOnPrimaryStorageReply;
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ResizeVolumeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.RevertVolumeFromSnapshotOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.RevertVolumeFromSnapshotOnPrimaryStorageReply;
import org.zstack.header.storage.primary.SyncVolumeSizeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.SyncVolumeSizeOnPrimaryStorageReply;
import org.zstack.header.storage.primary.TakeSnapshotMsg;
import org.zstack.header.storage.primary.TakeSnapshotReply;
import org.zstack.header.storage.primary.UnlinkBitsOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.UnlinkBitsOnPrimaryStorageReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageMsg;
import org.zstack.storage.primary.EstimateVolumeTemplateSizeOnPrimaryStorageReply;

/**
 * Created by frank on 6/30/2015.
 */
public abstract class HypervisorBackend extends SMPPrimaryStorageBase {
    public HypervisorBackend() {
    }

    public HypervisorBackend(PrimaryStorageVO self) {
        super(self);
    }

    abstract void handle(InstantiateVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadVolumeTemplateToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadVolumeTemplateToPrimaryStorageReply> completion);

    abstract void handle(DeleteVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply> completion);

    abstract void handle(DownloadDataVolumeToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply> completion);

    abstract void handle(GetInstallPathForDataVolumeDownloadMsg msg, ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply> completion);

    abstract void handle(DeleteVolumeBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply> completion);

    abstract void handle(DeleteBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply> completion);

    abstract void handle(DownloadIsoToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadIsoToPrimaryStorageReply> completion);

    abstract void handle(DeleteIsoFromPrimaryStorageMsg msg, ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply> completion);

    abstract void handle(CheckSnapshotMsg msg, Completion completion);

    abstract void handle(TakeSnapshotMsg msg, ReturnValueCompletion<TakeSnapshotReply> completion);

    abstract void handle(DeleteSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(RevertVolumeFromSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg, ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply> completion);

    abstract void handle(CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void stream(VolumeSnapshotInventory from, VolumeInventory to, boolean fullRebase, Completion completion);

    abstract void handle(DownloadBitsFromKVMHostToPrimaryStorageMsg msg, ReturnValueCompletion<DownloadBitsFromKVMHostToPrimaryStorageReply> completion);

    abstract void handle(CancelDownloadBitsFromKVMHostToPrimaryStorageMsg msg, Completion completion);

    abstract void handle(GetDownloadBitsFromKVMHostProgressMsg msg, ReturnValueCompletion<GetDownloadBitsFromKVMHostProgressReply> completion);

    abstract void deleteBits(String path, Completion completion);

    abstract void deleteBits(String path, boolean folder, Completion completion);

    abstract void handle(CreateImageCacheFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeOnPrimaryStorageReply> completion);

    abstract void handle(CreateImageCacheFromVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateImageCacheFromVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply> completion);

    abstract void handle(UploadBitsToBackupStorageMsg msg, ReturnValueCompletion<UploadBitsToBackupStorageReply> completion);

    abstract void handleHypervisorSpecificMessage(SMPPrimaryStorageHypervisorSpecificMessage msg);

    abstract void connectByClusterUuid(String clusterUuid, ReturnValueCompletion<ClusterConnectionStatus> completion);

    abstract void handle(SyncVolumeSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply> completion);

    abstract void handle(EstimateVolumeTemplateSizeOnPrimaryStorageMsg msg, ReturnValueCompletion<EstimateVolumeTemplateSizeOnPrimaryStorageReply> completion);

    abstract void handle(CreateTemporaryVolumeFromSnapshotMsg msg, ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply> completion);

    abstract void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg, ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply> completion);

    abstract void handle(AskInstallPathForNewSnapshotMsg msg, ReturnValueCompletion<AskInstallPathForNewSnapshotReply> completion);

    abstract void handle(ChangeVolumeTypeOnPrimaryStorageMsg msg, ReturnValueCompletion<ChangeVolumeTypeOnPrimaryStorageReply> completion);

    abstract void handle(UnlinkBitsOnPrimaryStorageMsg msg, ReturnValueCompletion<UnlinkBitsOnPrimaryStorageReply> completion);

    abstract void downloadImageToCache(VmInstanceSpec.ImageSpec img, final ReturnValueCompletion<ImageCacheInventory> completion);

    abstract void handle(GetVolumeSnapshotEncryptedOnPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeSnapshotEncryptedOnPrimaryStorageReply> completion);

    abstract void handle(GetVolumeBackingChainFromPrimaryStorageMsg msg, ReturnValueCompletion<GetVolumeBackingChainFromPrimaryStorageReply> returnValueCompletion);

    abstract void handle(ResizeVolumeOnPrimaryStorageMsg msg, ReturnValueCompletion<ResizeVolumeOnPrimaryStorageReply> returnValueCompletion);

    abstract void handle(CommitVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<CommitVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void handle(PullVolumeSnapshotOnPrimaryStorageMsg msg, ReturnValueCompletion<PullVolumeSnapshotOnPrimaryStorageReply> completion);

    abstract void createEmptyVolumeWithBackingFile(final VolumeInventory volume, String hostUuid, String backingFile, final ReturnValueCompletion<KvmBackend.AgentRsp> completion);
}
