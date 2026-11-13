package org.zstack.header.volume;

import java.util.Objects;

public class VolumeCreationContext {
    private final String volumeUuid;
    private final VolumeType volumeType;
    private final String sourceType;
    private final String sourceUuid;
    private final VolumeEncryptionState targetEncryptionState;
    private final String hostUuid;
    private String primaryStorageUuid;
    private String allocatedInstallUrl;
    private String installPath;
    private String format;
    private Long actualSize;

    private VolumeCreationContext(Builder builder) {
        this.volumeUuid = builder.volumeUuid;
        this.volumeType = builder.volumeType;
        this.sourceType = builder.sourceType;
        this.sourceUuid = builder.sourceUuid;
        this.targetEncryptionState = Objects.requireNonNull(builder.targetEncryptionState, "targetEncryptionState");
        this.hostUuid = builder.hostUuid;
        this.primaryStorageUuid = builder.primaryStorageUuid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String volumeUuid;
        private VolumeType volumeType;
        private String sourceType;
        private String sourceUuid;
        private VolumeEncryptionState targetEncryptionState;
        private String hostUuid;
        private String primaryStorageUuid;

        public Builder volumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
            return this;
        }

        public Builder volumeType(VolumeType volumeType) {
            this.volumeType = volumeType;
            return this;
        }

        public Builder source(String sourceType, String sourceUuid) {
            this.sourceType = sourceType;
            this.sourceUuid = sourceUuid;
            return this;
        }

        public Builder targetEncryptionState(VolumeEncryptionState targetEncryptionState) {
            this.targetEncryptionState = targetEncryptionState;
            return this;
        }

        public Builder hostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
            return this;
        }

        public Builder primaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
            return this;
        }

        public VolumeCreationContext build() {
            return new VolumeCreationContext(this);
        }
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public VolumeType getVolumeType() {
        return volumeType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceUuid() {
        return sourceUuid;
    }

    public boolean isTargetEncrypted() {
        return targetEncryptionState == VolumeEncryptionState.Encrypted;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getAllocatedInstallUrl() {
        return allocatedInstallUrl;
    }

    public void setAllocatedInstallUrl(String allocatedInstallUrl) {
        this.allocatedInstallUrl = allocatedInstallUrl;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }
}
