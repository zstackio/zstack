package org.zstack.header.image;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.util.ArrayList;
import java.util.List;

@TagResourceType(ImageVO.class)
@Action(category = ImageConstant.ACTION_CATEGORY)
public class APIAddImageMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(maxLength = 1024)
    private String url;
    @APIParam(required = false, validValues = {"RootVolumeTemplate", "ISO", "DataVolumeTemplate"})
    private String mediaType;
    @APIParam(maxLength = 255, required = false)
    private String guestOsType;
    private boolean system;
    @APIParam
    private String format;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    @APIParam(nonempty = true, resourceType = BackupStorageVO.class)
    private List<String> backupStorageUuids;
    private String type;

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public List<String> getBackupStorageUuids() {
        if (backupStorageUuids == null) {
            backupStorageUuids = new ArrayList<String>();
        }
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String type) {
        this.mediaType = type;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getType() {
        return type;
    }

    public void setType(String imageType) {
        this.type = imageType;
    }
}
