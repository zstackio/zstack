package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/9/2015.
 */
public class APIDeleteUserEvent extends APIEvent {
    public APIDeleteUserEvent() {
    }

    public APIDeleteUserEvent(String apiId) {
        super(apiId);
    }
}
