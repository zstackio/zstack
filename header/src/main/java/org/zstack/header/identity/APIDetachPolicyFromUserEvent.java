package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/9/2015.
 */
@RestResponse
public class APIDetachPolicyFromUserEvent extends APIEvent {
    public APIDetachPolicyFromUserEvent() {
    }

    public APIDetachPolicyFromUserEvent(String apiId) {
        super(apiId);
    }
}
