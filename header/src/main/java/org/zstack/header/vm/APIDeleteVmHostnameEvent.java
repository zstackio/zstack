package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 2/26/2016.
 */
public class APIDeleteVmHostnameEvent extends APIEvent {
    public APIDeleteVmHostnameEvent() {
    }

    public APIDeleteVmHostnameEvent(String apiId) {
        super(apiId);
    }
}

