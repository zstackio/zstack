package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by lining on 2019/1/16.
 */
@RestResponse
public class APICheckPasswordStrengthEvent extends APIEvent {
    public APICheckPasswordStrengthEvent() {
    }

    public APICheckPasswordStrengthEvent(String apiId) {
        super(apiId);
    }
}
