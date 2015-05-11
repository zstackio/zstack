package org.zstack.storage.primary.nfs;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 */
@GlobalPropertyDefinition
public class NfsPrimaryStorageGlobalProperty {
    @GlobalProperty(name="NfsPrimaryStorage.kvm.CreateTemplateFromVolumeCmd.timeout", defaultValue = "3600")
    public static int KVM_CreateTemplateFromVolumeCmd_TIMEOUT;
    @GlobalProperty(name="NfsPrimaryStorage.kvm.DownloadBitsFromSftpBackupStorageCmd.timeout", defaultValue = "3600")
    public static int KVM_DownloadBitsFromSftpBackupStorageCmd_TIMEOUT;
    @GlobalProperty(name="NfsPrimaryStorage.kvm.CreateRootVolumeFromTemplateCmd.timeout", defaultValue = "3600")
    public static int KVM_CreateRootVolumeFromTemplateCmd_TIMEOUT;
    @GlobalProperty(name="NfsPrimaryStorage.kvm.UploadToSftpCmd.timeout", defaultValue = "3600")
    public static int KVM_UploadToSftpCmd_TIMEOUT;
}
