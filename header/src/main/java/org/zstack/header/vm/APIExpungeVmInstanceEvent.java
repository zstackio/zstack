package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 11/12/2015.
 */
public class APIExpungeVmInstanceEvent extends APIEvent {
    public APIExpungeVmInstanceEvent() {
    }

    public APIExpungeVmInstanceEvent(String apiId) {
        super(apiId);
    }
}
