package org.zstack.header.storage.addon.primary;

public class ExternalPrimaryStorageImageDownloadContext {
    private String primaryStorageUuid;
    private String primaryStorageType;
    private String backupStorageUuid;
    private String backupStorageInstallPath;
    private String imageUuid;
    private Long imageSize;
    private Long imageActualSize;
    private String targetResourceType;
    private String targetResourceUuid;
    private String primaryStorageInstallPath;
    private Long primaryStorageSize;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPrimaryStorageType() {
        return primaryStorageType;
    }

    public void setPrimaryStorageType(String primaryStorageType) {
        this.primaryStorageType = primaryStorageType;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getBackupStorageInstallPath() {
        return backupStorageInstallPath;
    }

    public void setBackupStorageInstallPath(String backupStorageInstallPath) {
        this.backupStorageInstallPath = backupStorageInstallPath;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public Long getImageSize() {
        return imageSize;
    }

    public void setImageSize(Long imageSize) {
        this.imageSize = imageSize;
    }

    public Long getImageActualSize() {
        return imageActualSize;
    }

    public void setImageActualSize(Long imageActualSize) {
        this.imageActualSize = imageActualSize;
    }

    public String getTargetResourceType() {
        return targetResourceType;
    }

    public void setTargetResourceType(String targetResourceType) {
        this.targetResourceType = targetResourceType;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getPrimaryStorageInstallPath() {
        return primaryStorageInstallPath;
    }

    public void setPrimaryStorageInstallPath(String primaryStorageInstallPath) {
        this.primaryStorageInstallPath = primaryStorageInstallPath;
    }

    public Long getPrimaryStorageSize() {
        return primaryStorageSize;
    }

    public void setPrimaryStorageSize(Long primaryStorageSize) {
        this.primaryStorageSize = primaryStorageSize;
    }
}
