package org.zstack.header.storage.backup;

import org.zstack.header.core.trash.CleanTrashResult;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestResponse(allTo = "result")
public class APICleanUpTrashOnBackupStorageEvent extends APIEvent {
    private CleanTrashResult result;

    public CleanTrashResult getResult() {
        return result;
    }

    public void setResult(CleanTrashResult result) {
        this.result = result;
    }

    public APICleanUpTrashOnBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APICleanUpTrashOnBackupStorageEvent() {
        super(null);
    }

    public static APICleanUpTrashOnBackupStorageEvent __example__() {
        APICleanUpTrashOnBackupStorageEvent event = new APICleanUpTrashOnBackupStorageEvent();
        CleanTrashResult cleaned = new CleanTrashResult();
        cleaned.setResourceUuids(Collections.singletonList(uuid()));
        cleaned.setSize(1024000L);

        event.setResult(cleaned);

        return event;
    }
}
