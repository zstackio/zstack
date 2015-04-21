package org.zstack.storage.primary.iscsi;

import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 4/19/2015.
 */
public class IscsiBtrfsPrimaryStorageSimulatorConfig {
    List<DownloadBitsFromSftpBackupStorageCmd> downloadBitsFromSftpBackupStorageCmdList = new ArrayList<DownloadBitsFromSftpBackupStorageCmd>();
    volatile boolean downloadFromSftpSuccess = true;
    List<CheckBitsExistenceCmd> checkBitsExistenceCmds = new ArrayList<CheckBitsExistenceCmd>();
    volatile boolean checkBitsSuccess = true;
    List<DeleteBitsCmd> deleteBitsCmds = new ArrayList<DeleteBitsCmd>();
    volatile boolean deleteBitsSuccess = true;
    List<CreateRootVolumeFromTemplateCmd> createRootVolumeFromTemplateCmds = new ArrayList<CreateRootVolumeFromTemplateCmd>();
    volatile boolean createRootVolumeSuccess = true;
    List<CreateEmptyVolumeCmd> createEmptyVolumeCmds = new ArrayList<CreateEmptyVolumeCmd>();
    volatile boolean createEmptyVolumeSuccess = true;
    public volatile Long totalCapacity;
    public volatile Long availableCapacity;
    volatile boolean initSuccess = true;
}
