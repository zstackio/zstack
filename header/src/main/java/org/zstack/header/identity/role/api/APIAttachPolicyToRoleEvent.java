package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAttachPolicyToRoleEvent extends APIEvent {
    public APIAttachPolicyToRoleEvent() {
    }

    public APIAttachPolicyToRoleEvent(String apiId) {
        super(apiId);
    }
}
