package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by root on 7/30/16.
 */
public class APIStopVmInstanceSchedulerEvent extends APIEvent {
    public APIStopVmInstanceSchedulerEvent(String apiId) {
        super(apiId);
    }
    public APIStopVmInstanceSchedulerEvent() {
        super(null);
    }
}
