package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

public class APIAttachPolicyToUserEvent extends APIEvent {
    public APIAttachPolicyToUserEvent() {
        super(null);
    }
    public APIAttachPolicyToUserEvent(String apiId) {
        super(apiId);
    }
}
