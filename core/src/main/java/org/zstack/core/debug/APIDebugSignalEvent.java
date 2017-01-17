package org.zstack.core.debug;

import org.zstack.header.message.APIEvent;

/**
 * Created by xing5 on 2016/7/25.
 */
public class APIDebugSignalEvent extends APIEvent {
    public APIDebugSignalEvent() {
    }

    public APIDebugSignalEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDebugSignalEvent __example__() {
        APIDebugSignalEvent event = new APIDebugSignalEvent();


        return event;
    }

}
