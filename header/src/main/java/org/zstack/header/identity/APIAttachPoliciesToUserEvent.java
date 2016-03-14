package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by xing5 on 2016/3/14.
 */
public class APIAttachPoliciesToUserEvent extends APIEvent {
    public APIAttachPoliciesToUserEvent() {
    }

    public APIAttachPoliciesToUserEvent(String apiId) {
        super(apiId);
    }
}
