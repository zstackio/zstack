package org.zstack.header.storage.backup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse
public class APICleanUpTrashOnBackupStorageEvent extends APIEvent {
    public APICleanUpTrashOnBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APICleanUpTrashOnBackupStorageEvent() {
        super(null);
    }

    public static APICleanUpTrashOnBackupStorageEvent __example__() {
        APICleanUpTrashOnBackupStorageEvent event = new APICleanUpTrashOnBackupStorageEvent();

        return event;
    }
}
