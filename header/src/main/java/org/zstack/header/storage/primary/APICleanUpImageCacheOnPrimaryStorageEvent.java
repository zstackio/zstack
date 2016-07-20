package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;

/**
 * Created by xing5 on 2016/7/21.
 */
public class APICleanUpImageCacheOnPrimaryStorageEvent extends APIEvent {
    public APICleanUpImageCacheOnPrimaryStorageEvent() {
    }

    public APICleanUpImageCacheOnPrimaryStorageEvent(String apiId) {
        super(apiId);
    }
}
