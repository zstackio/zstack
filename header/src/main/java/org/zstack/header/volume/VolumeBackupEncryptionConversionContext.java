package org.zstack.header.volume;

public class VolumeBackupEncryptionConversionContext {
    private String volumeUuid;
    private boolean targetEncrypted;
    private Object extensionData;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public boolean isTargetEncrypted() {
        return targetEncrypted;
    }

    public void setTargetEncrypted(boolean targetEncrypted) {
        this.targetEncrypted = targetEncrypted;
    }

    public Object getExtensionData() {
        return extensionData;
    }

    public void setExtensionData(Object extensionData) {
        this.extensionData = extensionData;
    }
}
