package org.zstack.header.storage.primary;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 8/26/18.
 */
public class DownloadBitsFromKVMHostToPrimaryStorageMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private String srcHostUuid;
    private String hostInstallPath;
    private String primaryStorageUuid;
    private String primaryStorageInstallPath;
    private String destHostUuid;
    private Long bandWidth;
    private String longJobUuid;
    @NoLogging
    private String srcQcow2EncryptSecret;
    @NoLogging
    private String dstRbdEncryptSecret;
    private String dstRbdEncryptFormat;

    public String getDestHostUuid() {
        return destHostUuid;
    }

    public void setDestHostUuid(String destHostUuid) {
        this.destHostUuid = destHostUuid;
    }

    public String getSrcHostUuid() {
        return srcHostUuid;
    }

    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }

    public String getHostInstallPath() {
        return hostInstallPath;
    }

    public void setHostInstallPath(String hostInstallPath) {
        this.hostInstallPath = hostInstallPath;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }

    public Long getBandWidth() {
        return bandWidth;
    }

    public void setBandWidth(Long bandWidth) {
        this.bandWidth = bandWidth;
    }

    public String getLongJobUuid() {
        return longJobUuid;
    }

    public void setLongJobUuid(String longJobUuid) {
        this.longJobUuid = longJobUuid;
    }

    public String getSrcQcow2EncryptSecret() {
        return srcQcow2EncryptSecret;
    }

    public void setSrcQcow2EncryptSecret(String srcQcow2EncryptSecret) {
        this.srcQcow2EncryptSecret = srcQcow2EncryptSecret;
    }

    public String getDstRbdEncryptSecret() {
        return dstRbdEncryptSecret;
    }

    public void setDstRbdEncryptSecret(String dstRbdEncryptSecret) {
        this.dstRbdEncryptSecret = dstRbdEncryptSecret;
    }

    public String getDstRbdEncryptFormat() {
        return dstRbdEncryptFormat;
    }

    public void setDstRbdEncryptFormat(String dstRbdEncryptFormat) {
        this.dstRbdEncryptFormat = dstRbdEncryptFormat;
    }
}
