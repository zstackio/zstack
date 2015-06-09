package org.zstack.storage.primary.iscsi;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by frank on 6/7/2015.
 */
@GlobalPropertyDefinition
public class IscsiBtrfsPrimaryStorageGlobalProperty {
    @GlobalProperty(name="IscsiFileSystemBackendPrimaryStorage.DownloadBitsFromSftpBackupStorageCmd.timeout", defaultValue = "3600")
    public static int DownloadBitsFromSftpBackupStorageCmd_TIMEOUT;
}
