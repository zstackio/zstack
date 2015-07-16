package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/15/2015.
 */
public class APIDeleteAccountEvent extends APIEvent {
    public APIDeleteAccountEvent() {
    }

    public APIDeleteAccountEvent(String apiId) {
        super(apiId);
    }
}
