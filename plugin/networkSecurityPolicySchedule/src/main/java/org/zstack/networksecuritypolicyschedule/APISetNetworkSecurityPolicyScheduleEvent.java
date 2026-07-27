package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetNetworkSecurityPolicyScheduleEvent extends APIEvent {
    public APISetNetworkSecurityPolicyScheduleEvent() {
    }

    public APISetNetworkSecurityPolicyScheduleEvent(String apiId) {
        super(apiId);
    }
}
