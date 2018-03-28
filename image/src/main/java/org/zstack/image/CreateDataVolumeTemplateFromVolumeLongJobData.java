package org.zstack.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.longjob.LongJobMessageData;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by Camile on 3/8/18.
 * copy by APICreateDataVolumeTemplateFromVolumeMsg
 */
public class CreateDataVolumeTemplateFromVolumeLongJobData extends LongJobMessageData {
    private String name;
    private String description;
    private String volumeUuid;
    private List<String> backupStorageUuids;
    private String resourceUuid;
    private SessionInventory session;
    private List<String> systemTags;
    private List<String> userTags;

    public CreateDataVolumeTemplateFromVolumeLongJobData(NeedReplyMessage msg) {
        super(msg);
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

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public List<String> getUserTags() {
        return userTags;
    }

    public void setUserTags(List<String> userTags) {
        this.userTags = userTags;
    }
}
