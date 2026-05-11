package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachPhysicalServerRoleEvent extends APIEvent {
    public APIDetachPhysicalServerRoleEvent() {
        super(null);
    }

    public APIDetachPhysicalServerRoleEvent(String apiId) {
        super(apiId);
    }

    public static APIDetachPhysicalServerRoleEvent __example__() {
        return new APIDetachPhysicalServerRoleEvent();
    }
}
