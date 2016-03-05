package org.zstack.header.tag;

import org.zstack.header.message.APIEvent;

/**
 */
public class APIDeleteTagEvent extends APIEvent {
    public APIDeleteTagEvent(String apiId) {
        super(apiId);
    }

    public APIDeleteTagEvent() {
        super(null);
    }
}
