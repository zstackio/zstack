package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/9/2015.
 */
@RestResponse
public class APIRemoveUserFromGroupEvent extends APIEvent {
    public APIRemoveUserFromGroupEvent() {
    }

    public APIRemoveUserFromGroupEvent(String apiId) {
        super(apiId);
    }
}
