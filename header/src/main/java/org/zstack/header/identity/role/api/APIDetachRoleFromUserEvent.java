package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachRoleFromUserEvent extends APIEvent {
    public APIDetachRoleFromUserEvent() {
    }

    public APIDetachRoleFromUserEvent(String apiId) {
        super(apiId);
    }
}
