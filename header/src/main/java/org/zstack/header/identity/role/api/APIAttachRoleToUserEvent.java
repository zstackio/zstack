package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAttachRoleToUserEvent extends APIEvent {
    public APIAttachRoleToUserEvent() {
    }

    public APIAttachRoleToUserEvent(String apiId) {
        super(apiId);
    }
}
