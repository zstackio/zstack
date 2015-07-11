package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

public class APIAddUserToGroupEvent extends APIEvent {
    public APIAddUserToGroupEvent(String apiId) {
        super(apiId);
    }
    
    public APIAddUserToGroupEvent() {
        super(null);
    }
}
