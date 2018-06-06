package org.zstack.header.identity.role.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemovePolicyStatementsFromRoleEvent extends APIEvent {
    public APIRemovePolicyStatementsFromRoleEvent() {
    }

    public APIRemovePolicyStatementsFromRoleEvent(String apiId) {
        super(apiId);
    }

    public static APIRemovePolicyStatementsFromRoleEvent __example__() {
        APIRemovePolicyStatementsFromRoleEvent event = new APIRemovePolicyStatementsFromRoleEvent();
        return event;
    }
}
