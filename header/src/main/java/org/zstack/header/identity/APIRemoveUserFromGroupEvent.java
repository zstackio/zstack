package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/9/2015.
 */
public class APIRemoveUserFromGroupEvent extends APIEvent {
    public APIRemoveUserFromGroupEvent() {
    }

    public APIRemoveUserFromGroupEvent(String apiId) {
        super(apiId);
    }
}
