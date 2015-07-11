package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/10/2015.
 */
public class APIResetUserPasswordEvent extends APIEvent {
    public APIResetUserPasswordEvent() {
    }

    public APIResetUserPasswordEvent(String apiId) {
        super(apiId);
    }
}
