package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;

/**
 * Created by david on 8/31/16.
 */
public class APIDeleteExportedImageFromBackupStorageEvent extends APIEvent {
    public APIDeleteExportedImageFromBackupStorageEvent() {
    }

    public APIDeleteExportedImageFromBackupStorageEvent(String apiId) {
        super(apiId);
    }
}
