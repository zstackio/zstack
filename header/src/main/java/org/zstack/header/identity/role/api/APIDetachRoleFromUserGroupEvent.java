package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachRoleFromUserGroupEvent extends APIEvent {
    public APIDetachRoleFromUserGroupEvent() {
    }

    public APIDetachRoleFromUserGroupEvent(String apiId) {
        super(apiId);
    }
}
