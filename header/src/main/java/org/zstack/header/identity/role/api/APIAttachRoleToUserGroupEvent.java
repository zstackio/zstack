package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAttachRoleToUserGroupEvent extends APIEvent {
    public APIAttachRoleToUserGroupEvent() {
    }

    public APIAttachRoleToUserGroupEvent(String apiId) {
        super(apiId);
    }
}
