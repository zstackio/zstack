package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;

public class APIExportImageFromBackupStorageEvent extends APIEvent {
    public APIExportImageFromBackupStorageEvent() {
    }

    public APIExportImageFromBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    private String imageUrl;
}
