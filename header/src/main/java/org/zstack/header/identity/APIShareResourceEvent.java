package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/13/2015.
 */
public class APIShareResourceEvent extends APIEvent {
    public APIShareResourceEvent() {
    }

    public APIShareResourceEvent(String apiId) {
        super(apiId);
    }
}
