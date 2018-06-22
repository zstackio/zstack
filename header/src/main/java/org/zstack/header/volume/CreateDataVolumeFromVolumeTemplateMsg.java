package org.zstack.header.volume;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Create by weiwang at 2018/6/19
 */
public class CreateDataVolumeFromVolumeTemplateMsg extends NeedReplyMessage {
    private String imageUuid;
    private String name;
    private String description;
    private String primaryStorageUuid;
    private String hostUuid;
    private String resourceUuid;
    private SessionInventory session;
    private APICreateDataVolumeFromVolumeTemplateMsg apiMsg;

    public CreateDataVolumeFromVolumeTemplateMsg() {
    }

    public CreateDataVolumeFromVolumeTemplateMsg(APICreateDataVolumeFromVolumeTemplateMsg amsg) {
        imageUuid = amsg.getImageUuid();
        name = amsg.getName();
        description = amsg.getDescription();
        primaryStorageUuid = amsg.getPrimaryStorageUuid();
        hostUuid = amsg.getHostUuid();
        resourceUuid = amsg.getResourceUuid();
        session = amsg.getSession();
        apiMsg = amsg;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
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

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public APICreateDataVolumeFromVolumeTemplateMsg getApiMsg() {
        return apiMsg;
    }

    public void setApiMsg(APICreateDataVolumeFromVolumeTemplateMsg amsg) {
        this.apiMsg = amsg;
    }
}
