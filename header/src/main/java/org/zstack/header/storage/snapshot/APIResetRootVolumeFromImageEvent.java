package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;

/**
 * Created by miao on 11/3/16.
 */
public class APIResetRootVolumeFromImageEvent extends APIEvent {
    public APIResetRootVolumeFromImageEvent(String apiId) {
        super(apiId);
    }

    public APIResetRootVolumeFromImageEvent() {
        super(null);
    }
}
