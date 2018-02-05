package org.zstack.header.image;

import org.zstack.header.core.ApiTimeout;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by camile on 2/5/2018.
 * copy by APICreateRootVolumeTemplateFromRootVolumeMsg
 */
@ApiTimeout(apiClasses = {APICreateRootVolumeTemplateFromRootVolumeMsg.class})
public class CreateRootVolumeTemplateFromRootVolumeMsg extends NeedReplyMessage {
    private String name;
    private String description;
    private String guestOsType;
    private List<String> backupStorageUuids;
    private String rootVolumeUuid;
    private String platform;
    private boolean system;
    private String resourceUuid;
    private SessionInventory session;

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

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
}
