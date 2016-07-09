package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by root on 7/30/16.
 */
public class APIStartVmInstanceSchedulerEvent extends APIEvent{
    public APIStartVmInstanceSchedulerEvent(String apiId) {
        super(apiId);
    }
    public APIStartVmInstanceSchedulerEvent() {
        super(null);
    }
}
