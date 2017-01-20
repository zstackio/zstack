package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
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
 
    public static APIExportImageFromBackupStorageEvent __example__() {
        APIExportImageFromBackupStorageEvent event = new APIExportImageFromBackupStorageEvent();

        event.setImageUrl("http://bs-host-name/path/to/image.qcow2");

        return event;
    }

}
