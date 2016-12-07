package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/9/2015.
 */
@RestResponse
public class APIDeletePolicyEvent extends APIEvent {
    public APIDeletePolicyEvent() {
    }

    public APIDeletePolicyEvent(String apiId) {
        super(apiId);
    }
}
