package org.zstack.storage.primary.local;

import org.zstack.storage.primary.local.LocalStorageKvmBackend.*;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.RebaseSnapshotBackingFilesCmd;
import org.zstack.storage.primary.local.LocalStorageKvmMigrateVmFlow.VerifySnapshotChainCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpUploadBitsCmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageSimulatorConfig {
    public static class Capacity {
        public long total;
        public long avail;
    }

    public Map<String, Capacity> capacityMap = new HashMap<String, Capacity>();
    public List<InitCmd> initCmdList = new ArrayList<InitCmd>();
    public List<GetPhysicalCapacityCmd> getPhysicalCapacityCmds = new ArrayList<GetPhysicalCapacityCmd>();
    public List<CreateEmptyVolumeCmd> createEmptyVolumeCmds = new ArrayList<CreateEmptyVolumeCmd>();
    public List<CreateVolumeFromCacheCmd> createVolumeFromCacheCmds = new ArrayList<CreateVolumeFromCacheCmd>();
    public List<DeleteBitsCmd> deleteBitsCmds = new ArrayList<DeleteBitsCmd>();
    public List<SftpUploadBitsCmd> uploadBitsCmds = new ArrayList<SftpUploadBitsCmd>();
    public List<SftpDownloadBitsCmd> downloadBitsCmds = new ArrayList<SftpDownloadBitsCmd>();
    public List<CreateTemplateFromVolumeCmd> createTemplateFromVolumeCmds = new ArrayList<CreateTemplateFromVolumeCmd>();
    public List<RevertVolumeFromSnapshotCmd> revertVolumeFromSnapshotCmds = new ArrayList<RevertVolumeFromSnapshotCmd>();
    public List<MergeSnapshotCmd> mergeSnapshotCmds = new ArrayList<MergeSnapshotCmd>();
    public List<RebaseAndMergeSnapshotsCmd> rebaseAndMergeSnapshotsCmds = new ArrayList<RebaseAndMergeSnapshotsCmd>();
    public List<OfflineMergeSnapshotCmd> offlineMergeSnapshotCmds = new ArrayList<OfflineMergeSnapshotCmd>();
    public List<CheckBitsCmd> checkBitsCmds = new ArrayList<CheckBitsCmd>();
    public List<RebaseRootVolumeToBackingFileCmd> rebaseRootVolumeToBackingFileCmds = new ArrayList<RebaseRootVolumeToBackingFileCmd>();
    public List<RebaseSnapshotBackingFilesCmd> rebaseSnapshotBackingFilesCmds = new ArrayList<RebaseSnapshotBackingFilesCmd>();
    public List<VerifySnapshotChainCmd> verifySnapshotChainCmds = new ArrayList<VerifySnapshotChainCmd>();
    public volatile boolean checkBitsSuccess = true;
}
