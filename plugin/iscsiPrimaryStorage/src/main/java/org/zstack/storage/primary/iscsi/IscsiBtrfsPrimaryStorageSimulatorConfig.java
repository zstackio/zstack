package org.zstack.storage.primary.iscsi;

import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 4/19/2015.
 */
public class IscsiBtrfsPrimaryStorageSimulatorConfig {
    public List<DownloadBitsFromSftpBackupStorageCmd> downloadBitsFromSftpBackupStorageCmdList = new ArrayList<DownloadBitsFromSftpBackupStorageCmd>();
    public volatile boolean downloadFromSftpSuccess = true;
    public List<CheckBitsExistenceCmd> checkBitsExistenceCmds = new ArrayList<CheckBitsExistenceCmd>();
    public volatile boolean checkBitsSuccess = true;
    public List<DeleteBitsCmd> deleteBitsCmds = new ArrayList<DeleteBitsCmd>();
    public volatile boolean deleteBitsSuccess = true;
    public List<CreateRootVolumeFromTemplateCmd> createRootVolumeFromTemplateCmds = new ArrayList<CreateRootVolumeFromTemplateCmd>();
    public volatile boolean createRootVolumeSuccess = true;
    public List<CreateEmptyVolumeCmd> createEmptyVolumeCmds = new ArrayList<CreateEmptyVolumeCmd>();
    public volatile boolean createEmptyVolumeSuccess = true;
    public volatile Long totalCapacity;
    public volatile Long availableCapacity;
    public volatile boolean initSuccess = true;
    public volatile boolean uploadSuccess = true;
    public List<UploadToSftpCmd> uploadToSftpCmds = new ArrayList<UploadToSftpCmd>();
    public volatile boolean createTargetSuccess = true;
    public List<CreateIscsiTargetCmd> createIscsiTargetCmds = new ArrayList<CreateIscsiTargetCmd>();
    public List<DeleteIscsiTargetCmd> deleteIscsiTargetCmds = new ArrayList<DeleteIscsiTargetCmd>();
    public volatile boolean createSubVolumeSuccess = true;
    public List<CreateSubVolumeCmd> createSubVolumeCmds = new ArrayList<CreateSubVolumeCmd>();
}
