package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;

/**
 * Created by miao on 11/3/16.
 */
public class APIReInitVmInstanceEvent extends APIEvent {
    public APIReInitVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIReInitVmInstanceEvent() {
        super(null);
    }
}
