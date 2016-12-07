package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 2/26/2016.
 */
@RestResponse
public class APIDeleteVmHostnameEvent extends APIEvent {
    public APIDeleteVmHostnameEvent() {
    }

    public APIDeleteVmHostnameEvent(String apiId) {
        super(apiId);
    }
}

