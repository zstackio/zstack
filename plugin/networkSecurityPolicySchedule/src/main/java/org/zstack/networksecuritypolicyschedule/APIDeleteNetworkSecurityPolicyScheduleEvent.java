package org.zstack.networksecuritypolicyschedule;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteNetworkSecurityPolicyScheduleEvent extends APIEvent {
    public APIDeleteNetworkSecurityPolicyScheduleEvent() {
    }

    public APIDeleteNetworkSecurityPolicyScheduleEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteNetworkSecurityPolicyScheduleEvent __example__() {
        return new APIDeleteNetworkSecurityPolicyScheduleEvent();
    }
}
