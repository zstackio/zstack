package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/10/2015.
 */
public class APIUpdateUserEvent extends APIEvent {
    public APIUpdateUserEvent() {
    }

    public APIUpdateUserEvent(String apiId) {
        super(apiId);
    }
}
