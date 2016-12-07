package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAttachPolicyToUserGroupEvent extends APIEvent {
    public APIAttachPolicyToUserGroupEvent(String apiId) {
        super(apiId);
    }

    public APIAttachPolicyToUserGroupEvent() {
        super(null);
    }
}
