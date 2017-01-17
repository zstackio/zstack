package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2016/3/14.
 */
@RestResponse
public class APIDetachPoliciesFromUserEvent extends APIEvent {
    public APIDetachPoliciesFromUserEvent() {
    }

    public APIDetachPoliciesFromUserEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDetachPoliciesFromUserEvent __example__() {
        APIDetachPoliciesFromUserEvent event = new APIDetachPoliciesFromUserEvent();


        return event;
    }

}
