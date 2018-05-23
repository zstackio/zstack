package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachPolicyFromRoleEvent extends APIEvent {
    public APIDetachPolicyFromRoleEvent() {
    }

    public APIDetachPolicyFromRoleEvent(String apiId) {
        super(apiId);
    }

    public static APIDetachPolicyFromRoleEvent __example__() {
        APIDetachPolicyFromRoleEvent event = new APIDetachPolicyFromRoleEvent();
        return event;
    }
}
