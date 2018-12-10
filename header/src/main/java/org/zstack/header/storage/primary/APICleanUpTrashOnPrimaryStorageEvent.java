package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse
public class APICleanUpTrashOnPrimaryStorageEvent extends APIEvent {
    public APICleanUpTrashOnPrimaryStorageEvent(String apiId) {
        super(apiId);
    }

    public APICleanUpTrashOnPrimaryStorageEvent() {
        super(null);
    }

    public static APICleanUpTrashOnPrimaryStorageEvent __example__() {
        APICleanUpTrashOnPrimaryStorageEvent event = new APICleanUpTrashOnPrimaryStorageEvent();

        return event;
    }
}