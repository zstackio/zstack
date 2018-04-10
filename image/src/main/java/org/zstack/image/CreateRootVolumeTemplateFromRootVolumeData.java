package org.zstack.image;

import org.zstack.header.identity.SessionInventory;
import org.zstack.header.longjob.LongJobMessageData;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by camile on 2/5/2018.
 * copy by APICreateRootVolumeTemplateFromRootVolumeMsg
 */
public class CreateRootVolumeTemplateFromRootVolumeData extends LongJobMessageData {
    public CreateRootVolumeTemplateFromRootVolumeData(NeedReplyMessage msg) {
        super(msg);
    }
    private String name;
    private String description;
    private String guestOsType;
    private List<String> backupStorageUuids;
    private String rootVolumeUuid;
    private String platform;
    private boolean system;

    private String resourceUuid;
    private SessionInventory session;
    private List<String> systemTags;
    private List<String> userTags;

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

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
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

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
