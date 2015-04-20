package org.zstack.storage.primary.iscsi;

import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.DownloadBitsFromSftpBackupStorageCmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 4/19/2015.
 */
public class IscsiBtrfsBackendPrimaryStorageSimulatorConfig {
    List<DownloadBitsFromSftpBackupStorageCmd> downloadBitsFromSftpBackupStorageCmdList = new ArrayList<DownloadBitsFromSftpBackupStorageCmd>();
    volatile boolean downloadFromSftpSuccess = true;
}
