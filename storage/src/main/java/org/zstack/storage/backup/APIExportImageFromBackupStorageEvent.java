package org.zstack.storage.backup;

import org.zstack.header.message.APIEvent;

public class APIExportImageFromBackupStorageEvent extends APIEvent {
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    private String imageUrl;
}
