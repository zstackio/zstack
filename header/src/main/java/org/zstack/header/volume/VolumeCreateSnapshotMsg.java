package org.zstack.header.volume;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.message.NeedQuotaCheckMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by david on 10/7/16.
 */
@ApiTimeout(apiClasses = {APICreateVolumeSnapshotMsg.class})
public class VolumeCreateSnapshotMsg extends NeedReplyMessage implements VolumeMessage, NeedQuotaCheckMessage {
    private String accountUuid;
    private String resourceUuid;
    private String name;
    private String description;
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
