package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by xing5 on 2016/3/14.
 */
public class APIDetachPolicesFromUserEvent extends APIEvent {
    public APIDetachPolicesFromUserEvent() {
    }

    public APIDetachPolicesFromUserEvent(String apiId) {
        super(apiId);
    }
}
