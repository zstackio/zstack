package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

public class APIAttachUserToUserGroupEvent extends APIEvent {
    public APIAttachUserToUserGroupEvent(String apiId) {
        super(apiId);
    }
    
    public APIAttachUserToUserGroupEvent() {
        super(null);
    }
}
