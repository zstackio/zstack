package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.NoJsonSchema;

/**
 * Created by xing5 on 2016/7/21.
 */
public class APICleanUpImageCacheOnPrimaryStorageEvent extends APIEvent {
    @NoJsonSchema
    private ImageCacheCleanupDetails details;

    public ImageCacheCleanupDetails getDetails() {
        return details;
    }

    public void setDetails(ImageCacheCleanupDetails details) {
        this.details = details;
    }

    public APICleanUpImageCacheOnPrimaryStorageEvent() {
    }

    public APICleanUpImageCacheOnPrimaryStorageEvent(String apiId) {
        super(apiId);
    }
}
