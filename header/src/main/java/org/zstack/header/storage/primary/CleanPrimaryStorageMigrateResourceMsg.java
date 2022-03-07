package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class CleanPrimaryStorageMigrateResourceMsg extends NeedReplyMessage implements PrimaryStorageMessage {
    private List<String> volumeUuids;
    private String primaryStorageUuid;
    private String srcPsUuid;
    private String apiUuid;

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public List<String> getVolumeUuids() {
        return volumeUuids;
    }

    public void setVolumeUuids(List<String> volumeUuids) {
        this.volumeUuids = volumeUuids;
    }

    public String getSrcPsUuid() {
        return srcPsUuid;
    }

    public void setSrcPsUuid(String srcPsUuid) {
        this.srcPsUuid = srcPsUuid;
    }

    public String getApiUuid() {
        return apiUuid;
    }

    public void setApiUuid(String apiUuid) {
        this.apiUuid = apiUuid;
    }
}
