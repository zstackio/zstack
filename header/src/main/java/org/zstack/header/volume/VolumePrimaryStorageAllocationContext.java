package org.zstack.header.volume;

import java.util.Objects;

public class VolumePrimaryStorageAllocationContext {
    private final VolumeEncryptionState targetEncryptionState;
    private final String volumeUuid;
    private final String sourceType;
    private final String sourceUuid;
    private final String hostUuid;
    private final String primaryStorageUuid;

    private VolumePrimaryStorageAllocationContext(Builder builder) {
        this.targetEncryptionState = Objects.requireNonNull(builder.targetEncryptionState, "targetEncryptionState");
        this.volumeUuid = builder.volumeUuid;
        this.sourceType = builder.sourceType;
        this.sourceUuid = builder.sourceUuid;
        this.hostUuid = builder.hostUuid;
        this.primaryStorageUuid = builder.primaryStorageUuid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private VolumeEncryptionState targetEncryptionState;
        private String volumeUuid;
        private String sourceType;
        private String sourceUuid;
        private String hostUuid;
        private String primaryStorageUuid;

        public Builder targetEncryptionState(VolumeEncryptionState targetEncryptionState) {
            this.targetEncryptionState = targetEncryptionState;
            return this;
        }

        public Builder volumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
            return this;
        }

        public Builder source(String sourceType, String sourceUuid) {
            this.sourceType = sourceType;
            this.sourceUuid = sourceUuid;
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

        public VolumePrimaryStorageAllocationContext build() {
            return new VolumePrimaryStorageAllocationContext(this);
        }
    }

    public VolumeEncryptionState getTargetEncryptionState() {
        return targetEncryptionState;
    }

    public boolean isTargetEncrypted() {
        return targetEncryptionState == VolumeEncryptionState.Encrypted;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceUuid() {
        return sourceUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }
}
